package lento.net;

import java.net.*;

class ConnectionInfo {
	InetAddress address;
	int port;

	ConnectionInfo(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	ConnectionInfo(NetPlayer pl) {
		address = pl.socket.getInetAddress();
		port = pl.udpPort;
	}

	public int hashCode() {
		return address.hashCode()^port;
	}
	public boolean equals(Object o) {
		ConnectionInfo info = (ConnectionInfo)o;
		return port==info.port && address.equals(info.address);
	}
};

