package chillblox;

import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.JmDNSDTalkServiceConfiguration;
import freddo.dtalk.util.LOG;

public class ServiceConfiguration extends JmDNSDTalkServiceConfiguration {
	@SuppressWarnings("unused")
	private final String TAG = LOG.tag(ServiceConfiguration.class);

	private final JmDNS mJmDNS;
	private final int mPort;

	public ServiceConfiguration(JmDNS jmDNS, int port) {
		super();
		mJmDNS = jmDNS;
		mPort = port;
	}

	@Override
	public int getPort() {
		return mPort;
	}

	@Override
	public String getType() {
		return "Server/1";
	}

	@Override
	public boolean runServiceDiscovery() {
		return false;
	}

	@Override
	public boolean registerService() {
		return true;
	}

	@Override
	public JmDNS getJmDNS() {
		return mJmDNS;
	}

}
