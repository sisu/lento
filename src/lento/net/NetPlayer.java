package lento.net;

import lento.gamestate.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

class NetPlayer extends Player implements Runnable {

	Socket socket;
	NetListener listener;
	volatile boolean done=false;
	int udpPort=0;
	int tcpPort=0;
	boolean waitingJoinOK = false;

	NetPlayer(Socket socket, NetListener listener) {
		this.socket = socket;
		this.listener = listener;
	}

	public void run() {
		System.out.printf("NetPlayer thread started: %s : %d\n", socket.getInetAddress().toString(), socket.getPort());
		DataInputStream in=null;
		try {
			in = new DataInputStream(socket.getInputStream());
			while(socket.isConnected() && !done) {
				System.out.println("Waiting for next packet...");
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
				handleJoinRequest(in);
				break;
			case NetListener.TCP_JOIN_OK:
				if (waitingJoinOK) {
					waitingJoinOK = false;
					listener.gotJoinOK(this);
				}
				break;
			case NetListener.TCP_PLAYER_SPAWN:
				handleSpawn(in);
				break;
			case NetListener.TCP_PLAYER_DIE:
				handleDie(in);
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
		System.out.println("asd");
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(NetListener.TCP_PLAYER_INFO);

		ArrayList<NetPlayer> players = listener.players;
		out.write(listener.physics.getPlayers().size());

		// Lähetetään ensin paikallisen pelaajan tiedot
		System.out.println("jee: "+listener.tcpSocket.getInetAddress().toString());
		out.write(listener.tcpSocket.getInetAddress().getAddress(), 0, 4);
		out.writeShort(listener.tcpSocket.getLocalPort());
		out.writeShort(listener.udpSocket.getLocalPort());
		out.write(listener.localPlayer.getID());

		sendSinglePlayerData(out, listener.localPlayer);

		for(NetPlayer p : players) {
			System.out.println("Attemping to send pl info: "+p.id);
			if (p.id >= 0)
				sendSinglePlayer(out,p);
		}

		out.flush();
	}
	private void handleJoinRequest(DataInputStream in) throws IOException {
		System.out.println("got join request");

		tcpPort = in.readUnsignedShort();
		udpPort = in.readUnsignedShort();
		System.out.printf("Got TCP and UDP ports: %d %d\n", tcpPort, udpPort);
		id = in.read();

		int nameLen = in.read();
		byte[] buf = new byte[nameLen];
		in.readFully(buf);
		name = new String(buf, "UTF-8");
		color = NetListener.readColor(in);

		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		// FIXME: tarkista onko ID vapaa
		if (listener.physics.getPlayers().size() >= 127) {
			out.write(NetListener.TCP_JOIN_FAIL);
			id = -1;
		} else {
			out.write(NetListener.TCP_JOIN_OK);
			listener.playerJoined(this);
			System.out.println("OK sent "+id);
		}
	}
	private void sendSinglePlayer(DataOutputStream out, NetPlayer p) throws IOException {
		out.write(p.socket.getInetAddress().getAddress(), 0, 4);
		System.out.printf("sending ports: %d %d\n", p.tcpPort, p.udpPort);
		out.writeShort(p.tcpPort);
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

		out.write(p.isAlive() ? 1 : 0);
	}
	private void handleDie(DataInputStream in) throws IOException {
		int killer = in.read();
		Player pl = listener.physics.getPlayer(killer);
		if (pl!=null)
			pl.addKills();

		int damage = in.readUnsignedShort();
		damageTaken += damage;
		deaths++;
		alive = false;
	}

	NetPlayer(DataInputStream in, NetListener listener) throws IOException {
		this.listener = listener;
		this.socket = null;
		readClientData(in);
	}
	NetPlayer(DataInputStream in, NetListener listener, Socket socket) throws IOException {
		this.listener = listener;
		this.socket = socket;
		readClientData(in);
	}

	private void readClientData(DataInputStream in) throws IOException {
		byte[] buf = new byte[4];
		in.readFully(buf);
		InetAddress addr = InetAddress.getByAddress(buf);
		tcpPort = in.readUnsignedShort();

		System.out.println("got address and port: "+addr.toString()+" "+tcpPort);

		if (socket==null) {
			socket = new Socket(addr, tcpPort);
			System.out.println("opened new socket");
		}

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

		alive = in.read()==1 ? true : false;
	}
	void requestJoin(int localID) throws IOException {
		waitingJoinOK = true;
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(NetListener.TCP_JOIN_GAME);

		System.out.println("Sending TCP port: "+listener.tcpSocket.getLocalPort());
		out.writeShort(listener.tcpSocket.getLocalPort());
		System.out.println("Sending UDP port: "+listener.udpSocket.getLocalPort());
		out.writeShort(listener.udpSocket.getLocalPort());
		out.write(localID);

		byte[] name = listener.localPlayer.getName().getBytes("UTF-8");
		out.write(name.length);
		out.write(name, 0, name.length);

		NetListener.writeColor(out, listener.localPlayer.getColor());
	}
	private void genID() {
		boolean ok;
		do {
			ok = true;
			id = (int)(Math.random()*128);
			for(Player pl : listener.physics.getPlayers())
				if (pl.getID()==id) {
					ok=false;
					break;
				}
		} while(!ok);
	}
	void handleUDPPacket(DatagramPacket p) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(p.getData());
		int type = in.read();
//		System.out.println("Paketti: "+type);
		try {
		switch(type) {
			case NetListener.UDP_PLAYER_STATE:
				updatePlayerState(in);
				break;
			case NetListener.UDP_PLAYER_SHOOT:
				readBullets(in);
				break;
			case NetListener.UDP_PLAYER_HIT:
				readHits(in);
				break;
			default:
				System.out.println("Warning: unknown UDP packet type: "+type);
		}
		} catch(IOException e) {
			System.out.printf("Bad data from client %s : %d\n", socket.getInetAddress().toString(), udpPort);
			// FIXME: katkaise yhteys?
		}
	}
	private void updatePlayerState(InputStream istream) throws IOException {
		DataInputStream in = new DataInputStream(istream);
		location.x = prevLocation.x = in.readFloat();
		location.y = prevLocation.y = in.readFloat();
		speedVec.x = in.readFloat();
		speedVec.y = in.readFloat();
		angle = in.readFloat();
		
		int mask = in.read();
		if ((mask&0x1)!=0) accelerating = true;
		if ((mask&0x2)!=0) turning = 1;
		else if ((mask&0x4)!=0) turning = -1;
		else turning = 0;
	}
	private void handleSpawn(DataInputStream in) throws IOException {
		float x = in.readFloat();
		float y = in.readFloat();
		spawn(new Point2D.Float(x,y));
	}
	private void readBullets(ByteArrayInputStream istream) throws IOException {
//		System.out.println("jee pateja");
		DataInputStream in = new DataInputStream(istream);
		int bulletID = in.readUnsignedShort();

		int count = istream.available()/16;
		for(int i=0; i<count; ++i) {
			float x,y,vx,vy;
			x=in.readFloat();
			y=in.readFloat();
			vx=in.readFloat();
			vy=in.readFloat();
			Bullet b = new Bullet(x,y,vx,vy,id,bulletID++);
			listener.addRemoteBullet(b);
		}
	}
	private void readHits(ByteArrayInputStream istream) throws IOException {
		DataInputStream in = new DataInputStream(istream);

		int count = istream.available()/3;
		for(int i=0; i<count; ++i) {
			int shooter = in.readUnsignedByte();
			int bulletID = in.readUnsignedShort();
			listener.addRemoteHit(shooter,bulletID);
		}
	}
};
