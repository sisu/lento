package lento.net;

import lento.gamestate.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;

class NetPlayer extends Player implements Runnable {

	Socket socket;
	NetListener listener;
	volatile boolean done=false;
	int udpPort=0;

	NetPlayer(Socket socket, NetListener listener) {
		this.socket = socket;
		this.listener = listener;
	}

	public void run() {
		DataInputStream in=null;
		try {
			in = new DataInputStream(socket.getInputStream());
			while(socket.isConnected() && !done) {
				int packetType = in.read();
				if (packetType == -1) break;
				System.out.println("got byte: "+packetType);
				handleTcpPacket(packetType, in);
			}
		} catch(Exception e) {
		} finally {
			listener.deletePlayer(this);
			try {
				if (in!=null)
					in.close();
				socket.close();
			} catch(IOException evvk) {}
		}
	}
	private void handleTcpPacket(int type, DataInputStream in) throws IOException {
		switch(type) {
			case NetListener.TCP_GET_AREA_INFO:
				sendAreaInfo();
				break;
			case NetListener.TCP_GET_PLAYER_INFO:
				sendPlayerInfo();
				break;
			case NetListener.TCP_JOIN_GAME:
				handleJoinRequest();
				break;
			default:
				System.out.println("Warning: unknown TCP packet type "+type);
				break;
		}
	}
	private void sendAreaInfo() throws IOException {
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(NetListener.TCP_AREA_INFO);

		AreaGeometry area = listener.physics.getGeometry();
		out.writeInt(area.getWidth());
		out.writeInt(area.getHeight());

		NetListener.writeColor(out, area.getBorderColor());

		ArrayList<ColoredPolygon> polys = area.getPolygons();
		out.writeShort(polys.size());
		for(ColoredPolygon p : polys) {
			out.writeShort(p.npoints);
			NetListener.writeColor(out, p.color);

			for(int j=0; j<p.npoints; ++j) {
				out.writeFloat(p.xpoints[j]);
				out.writeFloat(p.ypoints[j]);
			}
		}
		out.flush();
	}
	private void sendPlayerInfo() throws IOException {
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(NetListener.TCP_PLAYER_INFO);

		ArrayList<NetPlayer> players = listener.players;
		out.write(listener.physics.getPlayers().size());

		// Lähetetään ensin paikallisen pelaajan tiedot
		out.write(listener.tcpSocket.getInetAddress().getAddress(), 0, 4);
		out.writeShort(listener.tcpSocket.getLocalPort());
		out.writeShort(listener.udpSocket.getPort());
		out.write(listener.localPlayer.getID());

		sendSinglePlayerData(out, listener.localPlayer);

		for(NetPlayer p : players)
			if (p.id >= 0)
				sendSinglePlayer(out,p);

		out.flush();
	}
	private void handleJoinRequest() throws IOException {
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		if (listener.physics.getPlayers().size() >= 127)
			out.write(NetListener.TCP_JOIN_FAIL);
		else {
			out.write(NetListener.TCP_JOIN_OK);
			listener.physics.addPlayer(this);
		}
	}
	private void sendSinglePlayer(DataOutputStream out, NetPlayer p) throws IOException {
		out.write(p.socket.getInetAddress().getAddress(), 0, 4);
		out.writeShort(p.socket.getPort());
		out.writeShort(p.udpPort);
		out.write(p.id);
		sendSinglePlayerData(out,p);
	}
	private void sendSinglePlayerData(DataOutputStream out, Player p) throws IOException {
		byte[] name = p.getName().getBytes("UTF-8");
		out.write(name.length);
		out.write(name, 0, name.length);

		NetListener.writeColor(out,p.getColor());

		int[] stats = p.getStats();
		assert stats.length==4;
		for(int s : stats)
			out.writeInt(s);
	}

	NetPlayer(DataInputStream in, NetListener listener, Socket connected) throws IOException {
		this.listener = listener;

		byte[] buf = new byte[4];
		in.readFully(buf);
		InetAddress addr = InetAddress.getByAddress(buf);
		int tcpPort = in.readUnsignedShort();

		System.out.println("got address and port: "+addr.toString()+" "+tcpPort);

		if (connected.getInetAddress().equals(addr) && connected.getPort()==tcpPort)
			socket = connected;
		else
			socket = new Socket(addr, tcpPort);

		udpPort = in.readUnsignedShort();
		id = in.readUnsignedByte();

		int nameLen = in.readUnsignedByte();
		buf = new byte[nameLen];
		in.readFully(buf);
		name = new String(buf, "UTF-8");

		color = NetListener.readColor(in);

		kills = in.readInt();
		deaths = in.readInt();
		damageDone = in.readInt();
		damageTaken = in.readInt();
	}
};
