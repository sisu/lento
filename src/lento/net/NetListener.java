package lento.net;

import lento.gamestate.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

public class NetListener implements Runnable, PhysicsObserver {

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

	static final int IN_BUFFER_SIZE = 65536;
	static final int OUT_BUFFER_SIZE = 65536;

	GamePhysics physics;
	ArrayList<NetPlayer> players = new ArrayList<NetPlayer>();
	ServerSocket tcpSocket;
	DatagramSocket udpSocket = new DatagramSocket();
	Player localPlayer;
	HashMap<ConnectionInfo,NetPlayer> playerTable = new HashMap<ConnectionInfo,NetPlayer>();

	ArrayList<Bullet> localShoots = new ArrayList<Bullet>();
	ArrayList<Bullet> remoteShoots = new ArrayList<Bullet>();
	ArrayList<Bullet> localHits = new ArrayList<Bullet>();
	ArrayList<Bullet> remoteHits = new ArrayList<Bullet>();

	private DatagramPacket outPacket = new DatagramPacket(new byte[OUT_BUFFER_SIZE], OUT_BUFFER_SIZE);
	private PacketOutputStream outBuffer = new PacketOutputStream(OUT_BUFFER_SIZE);

	private int waitCount=0;

	public NetListener(GamePhysics physics, Player localPlayer) throws IOException {
		this.physics = physics;
		this.localPlayer = localPlayer;
		tcpSocket = openServerSocket();
	}
	public NetListener(GamePhysics physics, Player localPlayer, InetAddress addr, int port) throws IOException {
		this.physics = physics;
		this.localPlayer = localPlayer;
		tcpSocket = openServerSocket();
		Socket socket = new Socket(addr,port);
		localPlayer.setID(handleOwnJoin(socket));
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
		new Thread(new Runnable() {
			public void run() {
				listenUDP();
			}
		}).start();
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
	private void listenUDP() {
		try {
			byte[] buf = new byte[IN_BUFFER_SIZE];
			DatagramPacket p = new DatagramPacket(buf, buf.length);
			while(true) {
				udpSocket.receive(p);
				ConnectionInfo info = new ConnectionInfo(p.getAddress(), p.getPort());
				NetPlayer from = playerTable.get(info);
				if (from==null)
					System.out.println("Warning: packet from unknown host: "+p.getAddress().toString()+" : "+p.getPort());
				else
					from.handleUDPPacket(p);
			}
		} catch(IOException e) {
		}
	}

	void deletePlayer(NetPlayer pl) {
		physics.deletePlayer(pl);
		players.remove(pl);
//		playerTable.remove();
	}

	private int handleOwnJoin(Socket initial) throws IOException {
		DataOutputStream out = new DataOutputStream(initial.getOutputStream());
		DataInputStream in = new DataInputStream(initial.getInputStream());
		getAreaInfo(in,out);
		getPlayerInfo(in,out,initial);
		for(NetPlayer p : players)
			new Thread(p).start();
		int id = genID();
		requestJoins(id);
		return id;
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
			System.out.println("vertex count: "+count);
			ColoredPolygon poly = new ColoredPolygon();
			poly.color = readColor(in);
			for(int j=0; j<count; ++j) {
				float x=in.readFloat(), y=in.readFloat();
				poly.addPoint((int)x,(int)y);
			}
			geom.addPolygon(poly);
		}
	}
	private void getPlayerInfo(DataInputStream in, DataOutputStream out, Socket connected) throws IOException {
		out.write(TCP_GET_PLAYER_INFO);
		out.flush();

		int reply = in.read();
		if (reply!=TCP_PLAYER_INFO)
			throw new IOException("Pelaajatietojen kysyminen epäonnistui: "+reply);

		int count = in.readUnsignedByte();

		System.out.println("player count: "+count);

		// Ensimmäisenä saadaan vastaajan omat tiedot
		NetPlayer current = new NetPlayer(in, this, connected);
		players.add(current);

		for(int i=0; i<count-1; ++i) {
			NetPlayer pl = new NetPlayer(in, this);
			players.add(pl);
			playerTable.put(new ConnectionInfo(pl), pl);
		}
	}
	private synchronized void requestJoins(int id) throws IOException {
		waitCount = players.size();
		System.out.println("players to wait for: "+waitCount);
		for(NetPlayer p : players)
			p.requestJoin(id);
		while(waitCount>0) {
			System.out.println("waiting: "+waitCount);
			try {
				wait();
			} catch(InterruptedException e) {
			}
		}
	}

	public void cleanUp() throws IOException {
		for(Iterator<NetPlayer> i=players.iterator(); i.hasNext(); ) {
			NetPlayer pl = i.next();
			pl.done = true;
			pl.socket.close();
		}
	}

	synchronized void gotJoinOK(NetPlayer pl) {
		physics.addPlayer(pl);
		playerTable.put(new ConnectionInfo(pl), pl);
		--waitCount;
		notify();
	}

	static Color readColor(DataInputStream in) throws IOException {
		int r=in.readUnsignedByte(), g=in.readUnsignedByte(), b=in.readUnsignedByte();
		System.out.printf("Got color: %d %d %d\n", r,g,b);
		return new Color(r,g,b);
	}
	static void writeColor(DataOutputStream out, Color c) throws IOException {
		System.out.printf("sending colors: %d %d %d\n", c.getRed(),c.getGreen(),c.getBlue());
		out.write(c.getRed());
		out.write(c.getGreen());
		out.write(c.getBlue());
	}

	public void sendSpawn() throws IOException {
		outBuffer.reset();
		DataOutputStream out = new DataOutputStream(outBuffer);
		out.write(TCP_PLAYER_SPAWN);

		Point2D.Float loc = localPlayer.getLoc();
		out.writeFloat(loc.x);
		out.writeFloat(loc.y);

		sendTCPPacket(outBuffer.getData(), outBuffer.size());
	}
	void sendUDPPacket(DatagramPacket packet) throws IOException {
		for(NetPlayer pl : players) {
			packet.setAddress(pl.socket.getInetAddress());
			packet.setPort(pl.udpPort);
			udpSocket.send(packet);
		}
	}
	void sendUDPPacket(byte[] buf, int length) throws IOException {
		DatagramPacket packet = new DatagramPacket(buf, length);
		for(NetPlayer pl : players) {
			if (pl.getID()<0) continue;
//			System.out.printf("Sending UDP packet %d: %d bytes to %s : %d\n", buf[0], length, pl.socket.getInetAddress().toString(), pl.udpPort);
			packet.setAddress(pl.socket.getInetAddress());
			packet.setPort(pl.udpPort);
			udpSocket.send(packet);
		}
	}
	void sendTCPPacket(byte[] buf, int length) throws IOException {
		for(NetPlayer pl : players) {
			if (pl.getID()<0) continue;
			System.out.printf("sending %d(%d) to %s : %d\n", buf[0], length, pl.socket.getInetAddress().toString(), pl.socket.getPort());
			OutputStream out = pl.socket.getOutputStream();
			out.write(buf, 0, length);
			out.flush();
		}
	}

	public void updateChanges() throws IOException {
		if (localPlayer.isAlive()) {
			// lähetä uusi sijainti
			outBuffer.reset();
			DataOutputStream out = new DataOutputStream(outBuffer);
			out.write(UDP_PLAYER_STATE);

			Point2D.Float loc = localPlayer.getLoc();
			out.writeFloat(loc.x);
			out.writeFloat(loc.y);

			Point2D.Float speed = localPlayer.getSpeedVec();
			out.writeFloat(speed.x);
			out.writeFloat(speed.y);

			out.writeFloat(localPlayer.getAngle());

			boolean accel = localPlayer.getAccelerating();
			int turn = localPlayer.getTurning();
			byte mask=0;
			if (accel) mask |= 0x1;
			if (turn>0) mask |= 0x2;
			if (turn<0) mask |= 0x4;
			out.write(mask);

			sendUDPPacket(outBuffer.getData(), outBuffer.size());
		}
		if (!localShoots.isEmpty()) {
			System.out.printf("Sending %d bullets\n", localShoots.size());
			outBuffer.reset();
			DataOutputStream out = new DataOutputStream(outBuffer);
			out.write(UDP_PLAYER_SHOOT);
			out.writeShort(localShoots.get(0).getID());

			for(Bullet b : localShoots) {
				Point2D.Float loc = b.getLoc();
				Point2D.Float speed = b.getSpeedVec();

				out.writeFloat(loc.x);
				out.writeFloat(loc.y);
				out.writeFloat(speed.x);
				out.writeFloat(speed.y);
			}
			sendUDPPacket(outBuffer.getData(), outBuffer.size());

			localShoots.clear();
		}
		if (!localHits.isEmpty()) {
			outBuffer.reset();
			DataOutputStream out = new DataOutputStream(outBuffer);
			out.write(UDP_PLAYER_HIT);

			for(Bullet b : localHits) {
				out.write(b.getShooter());
				out.writeShort(b.getID());
			}

			sendUDPPacket(outBuffer.getData(), outBuffer.size());

			localHits.clear();
		}
		synchronized(remoteShoots) {
			for(Bullet b : remoteShoots) {
				physics.addBullet(b);
			}
			remoteShoots.clear();
		}
		synchronized(remoteHits) {
			for(Bullet b : remoteHits) {
				physics.deleteBullet(b);
			}
			remoteHits.clear();
		}
	}
	void playerJoined(NetPlayer pl) {
		physics.addPlayer(pl);
		playerTable.put(new ConnectionInfo(pl), pl);
	}

	// PhysicsObserver-kamat
	public void shoot(Bullet b) {
		localShoots.add(b);
	}
	public void hit(Bullet b) {
		localHits.add(b);
	}
	public void die(int killer, int damage) {
		try {
			outBuffer.reset();
			DataOutputStream out = new DataOutputStream(outBuffer);
			out.write(TCP_PLAYER_DIE);
			out.write(killer);
			out.writeShort(damage);
			sendTCPPacket(outBuffer.getData(), outBuffer.size());
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	// PhysicsObserver end

	void addRemoteBullet(Bullet b) {
		synchronized(remoteShoots) {
			remoteShoots.add(b);
		}
	}
	void addRemoteHit(int shooter, int id) {
		Player pl = physics.getPlayer(shooter);
		pl.addHitDone();
		synchronized(remoteHits) {
			Bullet b = physics.getBullet(shooter,id);
			if (b!=null)
				remoteHits.add(b);
		}
	}
	private int genID() {
		int id=0;
		boolean ok;
		do {
			ok = true;
			id = (int)(Math.random()*128);
			for(Player pl : physics.getPlayers())
				if (pl.getID()==id) {
					ok=false;
					break;
				}
		} while(!ok);
		return id;
	}

}
