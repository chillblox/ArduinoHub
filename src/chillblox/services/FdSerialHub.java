package chillblox.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.arkasoft.freddo.messagebus.MessageBus;
import com.arkasoft.freddo.messagebus.MessageBusListener;

import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.events.IncomingMessageEvent;
import freddo.dtalk.events.OutgoingMessageEvent;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class FdSerialHub extends FdService {
	private static final String NAME = "chillblox.SerialHub";

	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;

	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;

	public final Map<String, SerialPortConnection> mConns = new ConcurrentHashMap<String, SerialPortConnection>();

	public FdSerialHub(DTalkServiceContext context) {
		super(context, NAME);
	}

	@Override
	protected synchronized void onDisposed() {
		for (SerialPortConnection conn : mConns.values()) {
			conn.close();
		}
		mConns.clear();
	}

	public void doOpen(JSONObject request) throws DTalkException {
		try {
			String portName = request.getString(DTalk.KEY_BODY_PARAMS);
			sendResponse(request, open(portName));
		} catch (JSONException e) {
			LOG.e(NAME, e.getMessage(), e);
			sendErrorResponse(request, DTalkException.INVALID_PARAMS,
					e.getMessage());
		}
	}

	public boolean open(String portName) throws DTalkException {
		SerialPortConnection conn = mConns.get(portName);
		if (conn != null) {
			return true;
		}

		CommPortIdentifier portId = null;
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier
				.getPortIdentifiers();
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum
					.nextElement();
			if (currPortId.getName().equals(portName)) {
				portId = currPortId;
				break;
			}
		}

		if (portId == null) {
			throw new DTalkException(DTalkException.INTERNAL_ERROR,
					"Could not find COM port.");
		}

		try {

			// open serial port, and use class name for the appName.
			SerialPort serialPort = (SerialPort) portId.open(getClass()
					.getName(), TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			mConns.put(portName, new SerialPortConnection(serialPort));

		} catch (Exception e) {
			LOG.e(NAME, e.getMessage(), e);
			throw new DTalkException(DTalkException.INTERNAL_ERROR,
					e.getMessage());
		}

		return true;
	}

	public void doClose(JSONObject request) throws DTalkException {
		try {
			String portName = request.getString(DTalk.KEY_BODY_PARAMS);
			sendResponse(request, close(portName));
		} catch (JSONException e) {
			LOG.e(NAME, e.getMessage(), e);
			sendErrorResponse(request, DTalkException.INVALID_PARAMS,
					e.getMessage());
		}
	}

	public boolean close(String portName) throws DTalkException {
		SerialPortConnection conn = mConns.remove(portName);
		if (conn != null) {
			conn.close();
			return true;
		}

		throw new DTalkException(DTalkException.INTERNAL_ERROR,
				"Could not find COM port.");
	}

	public void getPorts(JSONObject request) throws DTalkException {
		sendResponse(request, getPorts());
	}

	private Object getPorts() {
		JSONArray result = new JSONArray();

		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier
				.getPortIdentifiers();
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = portEnum.nextElement();
			try {
				JSONObject o = new JSONObject();
				o.put("name", currPortId.getName());
				result.put(o);
			} catch (Exception e) {
				// ignore
			}
		}

		return result;
	}

	private class SerialPortConnection implements SerialPortEventListener, MessageBusListener<JSONObject> {

		/**
		 * A BufferedReader which will be fed by a InputStreamReader converting the
		 * bytes into characters making the displayed results codepage independent
		 */
		private final BufferedReader input;

		/** The output stream to the port */
		private final BufferedWriter output;

		private final SerialPort mSerialPort;

		private String mServiceName = null;

		SerialPortConnection(SerialPort serialPort) throws IOException {
			mSerialPort = serialPort;

			// open the streams
			input = new BufferedReader(new InputStreamReader(
					serialPort.getInputStream()));
			output = new BufferedWriter(new OutputStreamWriter(
					serialPort.getOutputStream()));

			try {
				// add event listeners
				serialPort.addEventListener(this);
			} catch (TooManyListenersException e) {
				// ignore
			}
			serialPort.notifyOnDataAvailable(true);
		}

		/**
		 * This should be called when you stop using the port. This will prevent
		 * port locking on platforms like Linux.
		 */
		public synchronized void close() {
			if (mServiceName != null) {
				MessageBus.unsubscribe(mServiceName, this);
				mServiceName = null;
			}

			if (mSerialPort != null) {
				mSerialPort.removeEventListener();
				mSerialPort.close();
			}
		}

		/**
		 * Handle an event on the serial port.
		 */
		@Override
		public synchronized void serialEvent(SerialPortEvent oEvent) {
			if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				try {
					String inputLine = input.readLine();
					LOG.d(NAME, "inputLine: %s", inputLine);

					JSONObject message = new JSONObject(inputLine);
					String action = message.optString(DTalk.KEY_BODY_ACTION);
					if ("register".equals(action)) {
						// reply...
						JSONObject result = new JSONObject();
						result.put(DTalk.KEY_BDOY_RESULT, true);
						output.write(result.toString());
						output.newLine();
						output.flush();

						// register service... (avoid double registration).
						if (mServiceName == null) {
							mServiceName = message.getString(DTalk.KEY_BODY_PARAMS);
							LOG.d(NAME, "Register: %s", mServiceName);
							MessageBus.subscribe(mServiceName, this);
						}
					} else {
						message.put(DTalk.KEY_BODY_VERSION, "1.0");
		        String to = message.optString("to", null);
		        if (to == null) {
		          MessageBus.sendMessage(new IncomingMessageEvent(message));
		        } else {
		          MessageBus.sendMessage(new OutgoingMessageEvent(message));
		        }
					}
				} catch (Exception e) {
					LOG.e(NAME, e.getMessage());
				}
			}
		}

		@Override
		public void messageSent(String topic, JSONObject message) {
			LOG.v(NAME, ">>> messageSent: %s", message);
			JSONObject _message = new JSONObject();
			try {
				_message.put(DTalk.KEY_BODY_ID, message.optString(DTalk.KEY_BODY_ID));
				_message.put(DTalk.KEY_BODY_ACTION, message.optString(DTalk.KEY_BODY_ACTION));
				_message.put(DTalk.KEY_BODY_PARAMS, message.optString(DTalk.KEY_BODY_PARAMS));
				_message.put(DTalk.KEY_FROM, message.optString(DTalk.KEY_FROM));
				output.write(_message.toString());
				output.newLine();
				output.flush();
			} catch (JSONException e) {
				LOG.e(NAME, e.getMessage(), e);
				sendErrorResponse(message, DTalkException.INVALID_JSON, e.getMessage());
			} catch (IOException e) {
				LOG.e(NAME, e.getMessage(), e);
				sendErrorResponse(message, DTalkException.INTERNAL_ERROR, e.getMessage());
			}
		}

	}

}
