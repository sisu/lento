package lento.net;

import lento.gamestate.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;

public class NetListener implements Runnable {

	static public final int DEFAULT_TCP_PORT = 53256;

	static final int TCP_GET_AREA_INFO = 0x01;
	static final int TCP_GET_PLAYER_INFO = 0x02;
	static final int TCP_AREA_INFO = 0x03;
	static final int TCP_PLAYER_INFO = 0x04;
	static final int TCP_JOIN_GAME = 0x05;
	static final int TCP_JOIN_OK = 0x06;
	static final int TCP_JOIN_FAIL = 0x07;
	static final int TCP_PLAYER_DIE = 0x08;
	static final int TCP_PLAYER_SPAWN = 0x09;
	static final int UDP_PLAYER_STATE = 0x10;
	static final int UDP_PLAYER_SHOOT = 0x11;
	static final int UDP_PLAYER_HIT = 0x12;

	GamePhysics physics;
	ArrayList<NetPlayer> players = new ArrayList<NetPlayer>();
	ServerSocket tcpSocket;

	public NetListener(GamePhysics physics) throws IOException {
		this.physics = physics;
		tcpSocket = openServerSocket();
	}
	public NetListener(GamePhysics physics, InetAddress addr, int port) throws IOException {
		this.physics = physics;
		tcpSocket = openServerSocket();
		Socket socket = new Socket(addr,port);
		handleJoin(socket);
	}
	private ServerSocket openServerSocket() throws IOException {
		ServerSocket s;
		int port = DEFAULT_TCP_PORT;
		do {
			try {
				s = new ServerSocket(port);
			} catch(BindException e) {
				System.out.println("Failed binding to port "+port);
				s = null;
				++port;
			}
		} while(s==null);
		System.out.println("Binded to port "+port);
		return s;
	}

	public void run() {
		try {
			while(true) {
				Socket socket = tcpSocket.accept();
				NetPlayer pl = new NetPlayer(socket,this);
				players.add(pl);
				new Thread(pl).start();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	void deletePlayer(NetPlayer pl) {
		physics.deletePlayer(pl);
		players.remove(pl);
	}

	private void handleJoin(Socket initial) throws IOException {
		DataOutputStream out = new DataOutputStream(initial.getOutputStream());
		DataInputStream in = new DataInputStream(initial.getInputStream());
		getAreaInfo(in,out);
		getPlayerInfo(in,out);
	}
	private void getAreaInfo(DataInputStream in,DataOutputStream out) throws IOException {
		out.write(TCP_GET_AREA_INFO);
		out.flush();

		AreaGeometry geom = physics.getGeometry();

		int reply = in.read();
		if (reply!=TCP_AREA_INFO)
			throw new IOException("Aluetietojen pyytäminen epäonnistui.");
		int wSize = in.readInt();
		int hSize = in.readInt();
		geom.resetArea(wSize, hSize);

		System.out.printf("got sizes: %d %d\n", wSize,hSize);

		Color bColor = readColor(in);
		System.out.println("got bcolor: "+bColor.toString());
		geom.setBorderColor(bColor);

		int amount = in.readUnsignedShort();
		System.out.println("amount: "+amount);
		for(int i=0; i<amount; ++i) {
			int count = in.readUnsignedShort();
			System.out.println("count: "+count);
			ColoredPolygon poly = new ColoredPolygon();
			poly.color = readColor(in);
			for(int j=0; j<count; ++j) {
				float x=in.readFloat(), y=in.readFloat();
				poly.addPoint((int)x,(int)y);
			}
			geom.addPolygon(poly);
		}
	}
	private void getPlayerInfo(DataInputStream in, DataOutputStream out) throws IOException {
		out.write(TCP_GET_PLAYER_INFO);
		out.flush();

		int reply = in.read();
		if (reply!=TCP_PLAYER_INFO)
			throw new IOException("Pelaajatietojen kysyminen epäonnistui: "+reply);
	}

	public void cleanUp() throws IOException {
		for(Iterator<NetPlayer> i=players.iterator(); i.hasNext(); ) {
			NetPlayer pl = i.next();
			pl.done = true;
			pl.socket.close();
		}
	}
	private static Color readColor(DataInputStream in) throws IOException {
		int r=in.readUnsignedByte(), g=in.readUnsignedByte(), b=in.readUnsignedByte();
		System.out.printf("Got color: %d %d %d\n", r,g,b);
		return new Color(r,g,b);
	}
}
