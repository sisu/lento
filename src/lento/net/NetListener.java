package lento.net;

import lento.gamestate.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;

/** NetListener huolehtii kaikista peliin liittyneistä etäpelaajista ja
 * kommunikoinnista niiden kanssa.
 * NetListener-olio odottaa uusia TCP-yhteyksiä, ja luo uuden NetPlayer-olion
 * jokaiselle yhdistävälle pelaajalle.
 * NetListener-huolehtii myös UDP-pakettien lukemisesta, ja antaa paketin
 * aina sen NetPlayer-olion käsiteltäväksi, jonka verkko-osoitteesta paketti
 * tuli.
 * @see lento.net.NetPlayer
 */
public class NetListener implements Runnable, PhysicsObserver {

	/** Paikallinen TCP-portti, jota yritetään ensisijaisesti käyttää.
	 * Jos tämä on varattu, yritetään tätä seuraavia porttinumeroita. */
	static public final int DEFAULT_TCP_PORT = 53256;

	/** Aikaraja, joka yhdistämistä yrittäessä odotetaan ennen luovuttamista. */
	static final int CONNECT_TIMEOUT = 2000;

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

	/** Nykyisen pelin fysiikasta huolehtiva olio */
	GamePhysics physics;
	/** Taulukko kaikista yhdistäneistä etäpelaajista.
	 * Tähän taulukkoon on kirjattu myös ne yhdistäneet pelaajat, jotka eivät vielä
	 * ole lähettäneet peliinliittymispyyntöä.
	 * Peliin kuulumattomat pelaajat tunnistetaan siitä, että niiden ID-numero on -1.
	 */
	ArrayList<NetPlayer> players = new ArrayList<NetPlayer>();
	/** Palvelin-socket, joka ottaa vastaan uusia TCP-yhteyksiä. */
	ServerSocket tcpSocket;
	/** UDP-socket, jonka kautta kaikki tämän asiakasohjelman UDP-liikenne kulkeen. */
	DatagramSocket udpSocket = new DatagramSocket();
	/** Paikallista pelaajaa vastaava Player-olio. */
	Player localPlayer;

	/** Hajautustaulu, jonka avulla haetaan UDP-paketin saapuessa paketin
	 * lähettää vastaava NetPlayer-olio.
	 */
	HashMap<InetSocketAddress,NetPlayer> playerTable = new HashMap<InetSocketAddress,NetPlayer>();

	/** Paikallisen pelaajan tällä framella ampumat ammukset.
	 * Näistä pidetään kirjaa muille pelaajille lähettämistä varten. */
	ArrayList<Bullet> localShoots = new ArrayList<Bullet>();
	/** Paikalliseen pelaajaan tällä framella osuneet ammukset.
	 * Näistä pidetään kirjaa muille pelaajille lähettämistä varten. */
	ArrayList<Bullet> localHits = new ArrayList<Bullet>();

	/** Kirjoituspuskuri, johon kirjoitetaan TCP- tai UDP-viesti ennen sen
	 * lähettämistä etäpelaajille. */
	private PacketOutputStream outBuffer = new PacketOutputStream(1<<16);

	// FIXME: jos joku ei hyväksykään?
	/** Tieto, monenko pelaajan hyväksyntää paikallisen pelaajan peliin
	 * liittymiselle vielä odotetaan, tai -1, jos liittyminen epäonnistui. */
	private int waitCount=0;

	// FIXME: hajoamiset?
	/** Tosi, joss paikallinen pelaaja on lopettanut pelin, ja kaikki yhteydet
	 * voidaan katkaista. */
	boolean done=false;

	/** Luo uuden NetPlayer-olion.
	 * @param physics Tämän pelin tilasta huolehtiva GamePhysics-olio
	 * @param localPlayer paikallisen pelaajan Player-olio
	 */
	public NetListener(GamePhysics physics, Player localPlayer) throws IOException {
		this.physics = physics;
		this.localPlayer = localPlayer;
		tcpSocket = openServerSocket();
	}
	/** Yhdistää verkossa käynnissä olevaan peliin.
	 * Tämä metodi hakee verkosta kaikki pelin tiedot ja päivittää
	 * ne @physics-oliolle. Metodista palaudutaan vasta, kun
	 * peliin liittyminen on kokonaan suoritettu, ja hyväksyntä
	 * saatu kaikilta pelaajilta.
	 * @param addr verkko-odoite, johon yhdistetään
	 * @param port portti, johon yhdistetään
	 * @return paikalliselle pelaajalle asetettu pelaaja-ID
	 */
	public int connect(InetAddress addr, int port) throws IOException {
		System.out.println("jee saatiin socket.");

		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(addr,port), CONNECT_TIMEOUT);

		if (!socket.isConnected())
			throw new IOException("Yhteyden muodostus etäkoneeseen epäonnistui");

		System.out.println("jee saatiin socket.");
		return handleOwnJoin(socket);
	}

	/** Luo ja avaa uuden TCP-socketin. Metodi yrittää ensin
	 * porttia DEFAULT_TCP_PORT, ja jos se ei ole käytettävissä,
	 * yritetään aina seuraavaa porttia kunnes vapaa portti löytyy.
	 * @return ServerSocket, johon muut voivat yhdistää.
	 */
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

	/** Alkaa kuunnella TCP- ja UDP-yhteyksiä.
	 */
	public void run() {
		new Thread(new Runnable() {
			public void run() {
				listenUDP();
			}
		}).start();
		try {
			while(!done) {
				Socket socket = tcpSocket.accept();
				NetPlayer pl = new NetPlayer(socket,this);
				players.add(pl);
				new Thread(pl).start();
			}
		} catch(SocketException e) {
			done = true;
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	/** Alkaa odottaa UDP-paketteja ja välittää niitä NetPlayer-olioille.
	 */
	private void listenUDP() {
		try {
			byte[] buf = new byte[1<<16];
			DatagramPacket p = new DatagramPacket(buf, buf.length);
			while(!done) {
				udpSocket.receive(p);
				InetSocketAddress info = new InetSocketAddress(p.getAddress(), p.getPort());
				NetPlayer from = playerTable.get(info);
				if (from==null)
					System.out.println("Warning: packet from unknown host: "+p.getAddress().toString()+" : "+p.getPort());
				else
					from.handleUDPPacket(p);
			}
		} catch(SocketException e) {
			done = true;
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	/** Poistaa etäpelaajan pelistä.
	 * @param player poistettava pelaaja
	 */
	void deletePlayer(NetPlayer player) {
		physics.deletePlayer(player);
		players.remove(player);
		playerTable.remove(getSocketAddress(player));
	}

	/** Pyytää etäpelaajalta pelin tiedot ja liittyy sen jälkeen itse peliin.
	 * Metodista palaudutaan vasta, kun peliin liittyminen on täysin suoritettu.
	 * @param initial TCP-yhteys etäpelaajaan, jolta pelin tiedot haetaan.
	 */
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

	/** Kysyy etäpelaajalta pelialueen tietoja, ja lukee vastauksen.
	 * @param in syötevirta, josta etäpelaajan vastaus voidaan lukea
	 * @param out tulostusvirta, johon kirjoitettu dataa menee etäpelaajalle
	 */
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
	/** Kysyy etäpelaajalta pelaajien tietoja, lukee vastauksen ja ottaa yhteyden kaikkiin pelaajiin.
	 * @param in syötevirta, josta etäpelaajan vastaus voidaan lukea
	 * @param out tulostusvirta, johon kirjoitettu dataa menee etäpelaajalle
	 * @param connected avoin TCP-yhteys etäpelaajaan, jolta tietoja kysytään
	 */
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
			playerTable.put(getSocketAddress(pl), pl);
		}
	}

	/** Lähettää kaikille etäpelaajille peliinliittymispyynnön, ja odottaa vastauksia.
	 * Metodista palaudutaan vasta, kun kaikilta pelaajilta on saatu hyväksyntä.
	 * @param id paikallisen pelaajan pelaaja-id
	 */
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
				throw new IOException("Liittymisen hyväksymispyyntöjen odotus epäonnistui.");
			}
		}
		if (waitCount < 0)
			throw new IOException("Peliinliittymispyyntö hylättiin.");
	}

	/** Vapauttaa kaikki tämän olion ja sen hallitsemien NetPlayer-olioiden
	 * käytössä olevat resurssit.
	 */
	public void cleanUp() throws IOException {
		done = true;
		tcpSocket.close();
		udpSocket.close();
		for(Iterator<NetPlayer> i=players.iterator(); i.hasNext(); ) {
			NetPlayer pl = i.next();
			pl.socket.close();
		}
	}

	/** Kertoo, että yksi verkkopelaajista on hyväksynyt paikallisen pelaajan
	 * liittymisen peliin. Peliin liittyessä kaikkien NetPlayer-olioiden
	 * odotetaan kutsuvan tätä metodia saatuaan hyväksynnän.
	 * @param pl pelaaja, jonka etäkoneelta hyväksymisviesti saatiin
	 */
	synchronized void gotJoinOK(NetPlayer pl) {
		physics.addPlayer(pl);
		playerTable.put(getSocketAddress(pl), pl);
		--waitCount;
		notify();
	}
	/** Kertoo, että yksi verkkopelaajista on hylännyt paikallisen pelaajan
	 * liittymispyynnön. Liittymisprosessi keskeytetään epäonnistuneena,
	 * jos yksikin etäpelaaja kieltää peliin liittymisen.
	 */
	synchronized void gotJoinFail() {
		waitCount = -1;
		notify();
	}

	/** Lukee syötevirrasta RGB-värin värikomponentit ja tekee niistä Color-olion.
	 * Metodi lukee syötevirrasta tasan 3 tavua.
	 * @param in syötevirta, josta väri luetaan
	 * @return luettu väri
	 *
	 * @throws IOException virrasta luku epäonnistuu
	 */
	static Color readColor(DataInputStream in) throws IOException {
		int r=in.readUnsignedByte(), g=in.readUnsignedByte(), b=in.readUnsignedByte();
		System.out.printf("Got color: %d %d %d\n", r,g,b);
		return new Color(r,g,b);
	}
	/** Kirjoittaa tulostusvirtaan värin RGB-muodossa kolmena tavuna.
	 * @param out tulostusvirta, johon värin komponenttien arvot kirjoitetaan
	 * @param c väri, joka kirjoitetaan tulostusvirtaan
	 *
	 * @throws IOException virtaan kirjoitus epäonnistuu
	 */
	static void writeColor(DataOutputStream out, Color c) throws IOException {
		System.out.printf("sending colors: %d %d %d\n", c.getRed(),c.getGreen(),c.getBlue());
		out.write(c.getRed());
		out.write(c.getGreen());
		out.write(c.getBlue());
	}

	/** Lähettää kaikille etäpelaajille tiedon paikallisen pelaajan uudelleensyntymästä.
	 *
	 * @throws IOException viestin lähetys epäonnistuu
	 */
	public void sendSpawn() throws IOException {
		outBuffer.reset();
		DataOutputStream out = new DataOutputStream(outBuffer);
		out.write(TCP_PLAYER_SPAWN);

		Point2D.Float loc = localPlayer.getLoc();
		out.writeFloat(loc.x);
		out.writeFloat(loc.y);

		sendTCPPacket(outBuffer.getData(), outBuffer.size());
	}
	
	/** Lähettää UDP-paketin kaikille etäpelaajille.
	 * @param buf taulukko, jonka alusta lähetettävä data luetaan
	 * @param length lähetettävän viestin pituus
	 *
	 * @throws IOException viestin lähetys epäonnistuu
	 */
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
	/** Lähettää TCP-paketin kaikille etäpelaajille.
	 * @param buf taulukko, jonka alusta lähetettävä data luetaan
	 * @param length lähetettävän viestin pituus
	 *
	 * @throws IOException viestin lähetys epäonnistuu
	 */
	void sendTCPPacket(byte[] buf, int length) throws IOException {
		for(NetPlayer pl : players) {
			if (pl.getID()<0) continue;
//			System.out.printf("sending %d(%d) to %s : %d\n", buf[0], length, pl.socket.getInetAddress().toString(), pl.socket.getPort());
			OutputStream out = pl.socket.getOutputStream();
			out.write(buf, 0, length);
			out.flush();
		}
	}

	/** Lähettää paikalliset muutokset etäpelaajille.
	 * Jos pelaaja on hengissä, lähetetään uudet sijaintitiedot.
	 * Jos pelaaja ampui tai pelaajaan osui ammuksia, lähetetään tiedot näistä.
	 *
	 * @throws IOException viestin lähetys epäonnistuu
	 */
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
//			System.out.printf("Sending %d bullets\n", localShoots.size());
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
	}

	/** Liittää pelaajan peliin. Tätä metodia ei kutsuta kaikille yhteyden
	 * muodostaville, vaan vasta, kun etäpelaaja on lähettänyt liittymispyynnön.
	 * @param pl peliin liittyvä etäpelaaja
	 */
	void playerJoined(NetPlayer pl) {
		physics.addPlayer(pl);
		playerTable.put(getSocketAddress(pl), pl);
	}


	// PhysicsObserver-rajapinnan toteutus

	/** Merkitsee lähetettäväksi tiedon, että paikallinen pelaaja on ampunut.
	 * @param b pelaajan ampuma ammus
	 */
	public void shoot(Bullet b) {
		localShoots.add(b);
	}
	/** Merkitsee lähetettäväksi tiedon, että paikalliseen pelaajaan on osunut
	 * ammus.
	 * @param b pelaajaan osunut ammus
	 */
	public void hit(Bullet b) {
		localHits.add(b);
	}
	/** Lähettää etäpelaajille tiedon paikallisen pelaajan kuolemasta.
	 * @param killer pelaajan tappaneen ammuksen ampujan pelaaja-ID
	 * @param damage pelaajalle aiheutunut vahinko hänen ollessaan hengissä
	 */
	public void die(int killer, int damage) {
		try {
			outBuffer.reset();
			DataOutputStream out = new DataOutputStream(outBuffer);
			out.write(TCP_PLAYER_DIE);
			out.write(killer);
			out.writeShort(damage);
			System.out.printf("sent damage: %d\n", damage);
			sendTCPPacket(outBuffer.getData(), outBuffer.size());
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	// PhysicsObserver-rajapinta

	/** Poistaa ammuksen pelistä ja kasvattaa ampujan osumatilastoja.
	 * @param shooter ampujan pelaaja-ID
	 * @param id ammuksen ID
	 * @param selfHit tosi, joss ammus osui ampujaan itseensä
	 */
	void addRemoteHit(int shooter, int id, boolean selfHit) {
		if (!selfHit) {
			Player pl = physics.getPlayer(shooter);
			if (pl!=null)
				pl.addHitDone();
		}
		physics.deleteBullet(shooter, id);
	}
	
	/** Generoi vapaan pelaaja-ID:n.
	 * @return väliltä 0-127 oleva numero, joka ei ole minkään pelaajan käytössä
	 */
	private int genID() {
		int id=0;
		boolean ok;
		do {
			ok = true;
			id = (int)(Math.random()*128);
			for(Player pl : players)
				if (pl.getID()==id) {
					ok=false;
					break;
				}
		} while(!ok);
		return id;
	}

	/** Palauttaa pelaajan UDP-yhteyttä vastaavan InetSocketAddress-olion.
	 * @param pl pelaaja, jonka yhteystiedot haetaan
	 * @return pelaajan verkko-osoitetta ja porttia vastaava olio
	 */
	private static InetSocketAddress getSocketAddress(NetPlayer pl) {
		return new InetSocketAddress(pl.socket.getInetAddress(), pl.udpPort);
	}
}
