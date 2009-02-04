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
			default:
				System.out.println("Warning: unknown packet type "+type);
				break;
		}
	}
	private void sendAreaInfo() throws IOException {
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		out.write(NetListener.TCP_AREA_INFO);

		AreaGeometry area = listener.physics.getGeometry();
		out.writeInt(area.getWidth());
		out.writeInt(area.getHeight());

		writeColor(out, area.getBorderColor());

		ArrayList<ColoredPolygon> polys = area.getPolygons();
		out.writeShort(polys.size());
		for(Iterator<ColoredPolygon> i=polys.iterator(); i.hasNext(); ) {
			ColoredPolygon p = i.next();
			out.writeShort(p.npoints);
			writeColor(out, p.color);

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
		// FIXME: lähetä vain oikeasti liittyneet pelaajat
		out.write(players.size()+1);
		sendSinglePlayer(out,this);
		for(Iterator<NetPlayer> i=players.iterator(); i.hasNext(); ) {
			NetPlayer p = i.next();
			sendSinglePlayer(out,p);
		}

		out.flush();
	}
	private void sendSinglePlayer(DataOutputStream out, NetPlayer p) throws IOException {
		out.write(p.socket.getInetAddress().getAddress(), 0, 4);
		out.writeShort(p.socket.getPort());
		out.writeShort(0); // FIXME: send UDP port
		out.write(p.id);
		
		byte[] name = p.name.getBytes("UTF-8");
		out.write(name.length);
		out.write(name, 0, name.length);

		writeColor(out,p.color);

		out.writeInt(p.kills);
		out.writeInt(p.deaths);
		out.writeInt(p.damageDone);
		out.writeInt(p.damageTaken);
	}

	private static void writeColor(DataOutputStream out, Color c) throws IOException {
		System.out.printf("sending colors: %d %d %d\n", c.getRed(),c.getGreen(),c.getBlue());
		out.write(c.getRed());
		out.write(c.getGreen());
		out.write(c.getBlue());
	}
};
