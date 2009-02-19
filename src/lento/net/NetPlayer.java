package lento.net;

import lento.gamestate.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * NetPlayer pitää kirjaa yhden etäpelaajan tiedoista.
 * Uusi NetPlayer-olio luodaan jokaiselle palvelimeen yhdistävälle
 * asiakasohjelmalle, vaikka asiakasohjelma ei vielä olisikaan
 * liittynyt peliin.
 */
class NetPlayer extends Player implements Runnable {

	/** Etäpelaajaan auki oleva TCP-yhteys */
	Socket socket;
	/** Pelin etäpelaajista huolehtivat NetListener-olio */
	NetListener listener;
	/** Etäkoneen käyttämä UDP-portti */
	int udpPort=0;
	/** Etäkoneen käyttämä TCP-portti */
	int tcpPort=0;
	/** Tieto siitä, odotetaanko tältä pelaajalta hyväksyntää paikallisen
	 * pelaajan peliin liittymiselle. */
	boolean waitingJoinOK = false;

	/** Luo olion uudelle etäpelaajalle.
	 * @param socket avoin TCP-yhteys etäpelaajaan
	 * @param listener NetListener-olio, joka pitää kirjaa tämän
	 *        pelin etäpelaajista
	 */
	NetPlayer(Socket socket, NetListener listener) {
		this.socket = socket;
		this.listener = listener;
	}

	/** Alkaa kuunnella TCP-yhteyttä etäpelaajaan, ja käsitellä pelaajalta
	 * tulevia paketteja. Tästä metodista palaudutaan vasta, kun yhteys
	 * pelaajaan katkeaa, tai päätetään poistua itse pelistä.
	 */
	public void run() {
		System.out.printf("NetPlayer thread started: %s : %d\n", socket.getInetAddress().toString(), socket.getPort());
		DataInputStream in=null;
		try {
			in = new DataInputStream(socket.getInputStream());
			while(socket.isConnected()) {
				System.out.println("Waiting for next packet...");
				int packetType = in.read();
				if (packetType == -1) break;
				System.out.println("got byte: "+packetType);
				handleTCPPacket(packetType, in);
			}
		} catch(Exception e) {
			// TCP-yhteyden poikkeuksia ei tarvitse käsitellä erityisesti.
			// Yhteyden katkeamistilanne tulkitaan aina etäpelaajan poistumisena.
		} finally {
			listener.deletePlayer(this);
			try {
				if (in!=null)
					in.close();
				socket.close();
			} catch(IOException e) {
				// Ei väliä vaikka sulkeminen aiheuttaisi virheen.
			}
		}
	}
	
	/** Huolehtii yhdestä etäkoneelta tulleesta TCP-viestistä.
	 * @param type viestin tyyppi
	 * @param in syötevirta, josta voi lukea etäpelaajan lähettämää dataa
	 */
	private void handleTCPPacket(int type, DataInputStream in) throws IOException {
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

	/** Lähettää AREA_INFO-paketin etäpelaajalle.
	 */
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

	/** Lähettää PLAYER_INFO-paketin etäpelaajalle.
	 */
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
	/** Lukee etäpelaajan lähettämän REQUEST_JOIN-viestin parametrit.
	 * Funktio päättää näiden perusteella hyväksytäänkö liittyminen,
	 * ja lähettää vastauksen.
	 * @param in syötevirta, josta paketin loppuosa voidaan lukea.
	 */
	private void handleJoinRequest(DataInputStream in) throws IOException {
		System.out.println("got join request");

		tcpPort = in.readUnsignedShort();
		udpPort = in.readUnsignedShort();
		System.out.printf("Got TCP and UDP ports: %d %d\n", tcpPort, udpPort);
		int id = in.read();

		int nameLen = in.read();
		byte[] buf = new byte[nameLen];
		in.readFully(buf);
		name = new String(buf, "UTF-8");
		color = NetListener.readColor(in);

		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		boolean idOK = true;
		for(Player pl : listener.physics.getPlayers())
			if (pl.getID() == id)
				idOK = false;
		if (!idOK || listener.physics.getPlayers().size() >= 127) {
			out.write(NetListener.TCP_JOIN_FAIL);
			this.id = -1;
		} else {
			this.id = id;
			out.write(NetListener.TCP_JOIN_OK);
			listener.playerJoined(this);
			System.out.println("OK sent "+id);
		}
	}
	/** Lähettää yhden etäpelaajan kaikki tiedot osana PLAYER_INFO-pakettia.
	 * @param out tulostusvirta, jonka kautta viesti välitetään etäkoneelle
	 * @param p pelaaja, jonka tiedot lähetetään
	 */
	private void sendSinglePlayer(DataOutputStream out, NetPlayer p) throws IOException {
		out.write(p.socket.getInetAddress().getAddress(), 0, 4);
		System.out.printf("sending ports: %d %d\n", p.tcpPort, p.udpPort);
		out.writeShort(p.tcpPort);
		out.writeShort(p.udpPort);
		out.write(p.id);
		sendSinglePlayerData(out,p);
	}
	/** Lähettää yhden pelaajan paikkatiedot osana PLAYER_INFO-pakettia.
	 * Tämä metodi on erillään sendSinglePlayer-metodista, koska tätä
	 * voidaan käyttää myös paikallisen pelaajan tietojen lähettämiseen.
	 * @param out tulostusvirta, jonka kautta viesti välitetään etäkoneelle
	 * @param p pelaaja, jonka tiedot lähetetään
	 */
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

	/** Käsittelee etäkoneelta tulleen viestin etäpelaajan kuolemasta.
	 * @param in syötevirta, josta viestin loppuosa voidaan lukea.
	 */
	private void handleDie(DataInputStream in) throws IOException {
		int killer = in.read();
		Player pl = listener.physics.getPlayer(killer);
		if (pl!=null)
			pl.addKills();

		int damage = in.readUnsignedShort();
		System.out.printf("got damage: %d\n", damage);
		damageTaken += damage;
		deaths++;
		alive = false;
	}

	/** Luo NetPlayer-olion lukemalla tiedot PLAYER_INFO-paketista.
	 * @param in syötevirta, josta tämän pelaajan tiedot voidaan lukea
	 * @param listener tämän pelin etäpelaajista huolehtiva NetListener-olio
	 */
	NetPlayer(DataInputStream in, NetListener listener) throws IOException {
		this.listener = listener;
		this.socket = null;
		readInitialData(in);
	}
	/** Luo NetPlayer-olion lukemalla verkko-osoitetta lukuunottamatta tiedot
	 * PLAYER_INFO-paketista.
	 * @param in syötevirta, josta tämän etäpelaajan tiedot voidaan lukea
	 * @param listener tämän pelin etäpelaajista huolehtiva NetListener-olio
	 * @param socket avoin TCP-yhteys tähän etäpelaajaan
	 */
	NetPlayer(DataInputStream in, NetListener listener, Socket socket) throws IOException {
		this.listener = listener;
		this.socket = socket;
		readInitialData(in);
	}

	/** Lukee pelaajan tiedot PLAYER_INFO-paketista.
	 * Jos tätä kutsuttaessa socket==null, pyrkii yhdistämään paketista
	 * löytyvään verkko-osoitteeseen, ja muuten jättää osoitteen huomiotta.
	 * @param in syötevirta, josta tämän etäpelaajan tiedot voidaan lukea
	 */
	private void readInitialData(DataInputStream in) throws IOException {
		byte[] buf = new byte[4];
		in.readFully(buf);
		InetAddress addr = InetAddress.getByAddress(buf);
		tcpPort = in.readUnsignedShort();

		System.out.println("got address and port: "+addr.toString()+" "+tcpPort);

		if (socket==null) {
			socket = new Socket();
			socket.connect(new InetSocketAddress(addr, tcpPort), NetListener.CONNECT_TIMEOUT);
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

	/** Lähettää tälle etäpelaajalle pyynnöt paikallisen pelaajan
	 * liittymisestä peliin.
	 * @param localID paikallisen pelaajan pelaaja-ID
	 */
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

		out.flush();
	}
	
	/** Käsittelee tältä etäpelaajalta tulleen UDP-paketin.
	 * @param packet käsiteltävä UDP-paketti
	 */
	void handleUDPPacket(DatagramPacket packet) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
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
			System.out.printf("Warning: Bad data from client %s (%s : %d)\n", name, socket.getInetAddress().toString(), udpPort);
			// FIXME: katkaise yhteys?
		}
	}

	/** Lukee PLAYER_STATE-paketista etäpelaajan uuden tilan.
	 * @param istream syötevirta, josta paketin sisältö voidaan lukea
	 */
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
	/** Käsittelee etäpelaajalta tulleen PLAYER_SPAWN-viestin.
	 * Kutsuu Player.spawn(float,float)-funktiota saamillaan tiedoilla.
	 * @param in syötevirta, josta viestin sisältö voidaan lukea
	 */
	private void handleSpawn(DataInputStream in) throws IOException {
		float x = in.readFloat();
		float y = in.readFloat();
		spawn(new Point2D.Float(x,y));
	}
	/** Käsittelee etäpelaajalta tulleen PLAYER_SHOOT-viestin.
	 * Metodi lisää paketissa tulleet ammukset GamePhysics-oliolle.
	 * @param istream syötevirta, josta paketin sisältö voidaan lukea
	 */
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
			listener.physics.addBullet(b);
		}
	}
	/** Käsittelee etäpelaajalta tulleen PLAYER_HIT-viestin.
	 * Kutsuu listener.addRemoteHit-metodia huolehtiakseen
	 * pelistä poistuneista ammuksista.
	 * @param istream syötevirta, josta paketin sisältö voidaan lukea
	 */
	private void readHits(ByteArrayInputStream istream) throws IOException {
		DataInputStream in = new DataInputStream(istream);

		int count = istream.available()/3;
//		System.out.println("hits count: "+count);
		for(int i=0; i<count; ++i) {
			int shooter = in.readUnsignedByte();
			int bulletID = in.readUnsignedShort();
			listener.addRemoteHit(shooter,bulletID,shooter==id);
		}
	}
};
