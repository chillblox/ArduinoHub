package chillblox;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import chillblox.services.FdSerialHub;

import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdPresence;
import freddo.dtalk.services.FdServiceMgr;
import freddo.dtalk.util.LOG;

public class Main {

	static {
		LOG.setLogLevel(LOG.VERBOSE);
	}

	private static JmDNS sJmDNS = null;
	private static FdServiceMgr sServiceMgr = null;

	private static DTalkServiceContext sDTalkServiceContext = new DTalkServiceContext() {
		@Override
		public void runOnUiThread(Runnable r) {
			r.run();
		}

		@Override
		public void assertBackgroundThread() {
			// TODO Auto-generated method stub
		}

		@Override
		public ExecutorService getThreadPool() {
			return DTalkService.getInstance().getConfiguration().getThreadPool();
		}
	};

	public static void main(String[] args) throws IOException {
		// Create JmDNS and initialize DTalkService.
		sJmDNS = JmDNS.create();
		DTalkService.init(new ServiceConfiguration(sJmDNS, 0));

		// Create service manager & register services...
		sServiceMgr = new FdServiceMgr(sDTalkServiceContext);
		sServiceMgr.registerService(new FdPresence(sDTalkServiceContext));
		sServiceMgr.registerService(new FdSerialHub(sDTalkServiceContext));

		// add shutdown hook to stop the service...
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Dispose services
				if (sServiceMgr != null) {
					sServiceMgr.dispose();
					sServiceMgr = null;
				}

				// Shutdown dtalk service
				DTalkService.getInstance().shutdown();

				try {
					sJmDNS.close();
				} catch (IOException e) {
					// ignore
				}
			}
		});

		DTalkService.getInstance().startup();

		// -------------------------

		/* Turn off metal's use of bold fonts */
		UIManager.put("swing.boldMetal", Boolean.FALSE);
		// Schedule a job for the event-dispatching thread:
		// adding TrayIcon.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	protected static void createAndShowGUI() {
		// Check the SystemTray is supported
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		}

		final PopupMenu popup = new PopupMenu();
		final TrayIcon trayIcon = new TrayIcon(createImage("/bulb.gif", "tray icon"));
		final SystemTray tray = SystemTray.getSystemTray();

		// Create a popup menu components
		MenuItem openItem = new MenuItem("Open");
		MenuItem exitItem = new MenuItem("Exit");

		// Add components to popup menu
		popup.add(openItem);
		popup.addSeparator();
		popup.add(exitItem);

		trayIcon.setPopupMenu(popup);
		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			System.out.println("TrayIcon could not be added.");
			return;
		}

		openItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String url = "http://localhost:" + DTalkService.getInstance().getLocalServiceInfo().getPort();
				try {
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().browse(new URI(url));
					} else {
						throw new UnsupportedOperationException("Can't open URL in default browser.");
					}
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(null, e1.getMessage(), "Chillblox Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tray.remove(trayIcon);
				System.exit(0);
			}
		});
	}

	// Obtain the image URL
	protected static Image createImage(String path, String description) {
		URL imageURL = Main.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} else {
			return (new ImageIcon(imageURL, description)).getImage();
		}
	}
}
