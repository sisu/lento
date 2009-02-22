package lento.gameui;

import lento.gamestate.*;
import lento.net.*;
import java.io.*;
import java.awt.*;
import java.net.*;

import javax.sound.midi.*;

/**
 * GameLoop huolehtii pelin aikana verkon, fysiikan ja paikallisen piirron ja
 * syötteenluvun välisestä kommunikaatiosta.
 * <p>
 * GameLoop-olio luodaan pelin alustuksen yhteydessä ensimmäisenä, ja se
 * huolehtii verkon ja fysiikan alustuksesta sekä peli-ikkunan avaamisesta.
 * <p>
 * GameLoop kutsuu pelin aikana eri olioiden tilanpäivitysfunktioita, ja
 * pyrkii pitämään ruudunpäivitysnopeuden vakiona.
 */
public class GameLoop {

	/** Pelin fysiikasta huolehtiva olio */
	private GamePhysics physics;
	/** Paikallisesta pelaajasta huolehtiva olio */
	private LocalPlayer localPlayer;
	/** Verkossa kommunikoinnista huolehtiva olio */
	private NetListener net;
	/** Piirtoikkuna */
	GameFrame frame;

	/** Tosi, joss paikallinen pelaaja haluaa poistua pelistä. */
	boolean done=false;

	/** Maksimaalinen ruudunpäivitysnopeus */
	private static final int MAX_FPS = 80;
	/** Yhteen frameen minimissään käytettävä aika nanosekunteina. */
	private static final long FRAME_TIME = (long)1e9 / MAX_FPS;

	/** Luo uuden pelin yhdistämättä keneenkään muuhun.
	 * @param file tiedosto, josta pelialueen tiedot luetaan
	 * @param name paikallisen pelaajan nimi
	 * @param color paikallisen pelaajan väri
	 *
	 * @throws IOException jos tiedoston luku tai verkon alustus epäonnistuu
	 */
	public GameLoop(File file, String name, Color color) throws IOException {
		localPlayer = new LocalPlayer(this, name, color);
		physics = new GamePhysics(file);
		net = new NetListener(physics, localPlayer);
		physics.addPlayer(localPlayer);
		physics.setObserver(net);
	}
	/** Pyrkii liittymään käynnissä olevaan peliin.
	 * @param addr verkko-osoite, johon yhdistetään
	 * @param port portti, johon yhdistetään
	 * @param name paikallisen pelaajan nimi
	 * @param color paikallisen pelaajan väri
	 *
	 * @throws IOException jos yhdistäminen epäonnistuu
	 */
	public GameLoop(InetAddress addr, int port, String name, Color color) throws IOException {
		localPlayer = new LocalPlayer(this, name, color);
		physics = new GamePhysics();

		net = new NetListener(physics, localPlayer);
		int id = net.connect(addr,port);
		localPlayer.setID(id);

		physics.addPlayer(localPlayer);
		physics.setObserver(net);
	}

	/** Suorittaa pelin pääsilmukan.
	 * Metodista palaudutaan vasta, kun paikallinen pelaaja poistuu pelistä.
	 */
	public void start() throws IOException {
		frame = new GameFrame(physics, localPlayer);
		frame.addKeyListener(localPlayer);

		new Thread(net).start();

//		System.out.println("starting game loop");

		long prevTime = System.nanoTime();
		long nextTime = prevTime;
		try{
			while(!done) {
				long time = System.nanoTime();
				float diff = (time-prevTime)/1e9f;

				if (time/(1000*1000*1000) > prevTime/(1000*1000*1000)) {
//					System.out.println(frame.frameCount+"frames");
					frame.frameCount=0;
				}

				boolean spawned = false;
				if (!localPlayer.isAlive() && time>=localPlayer.spawnTime) {
					localPlayer.spawn(physics.getGeometry().getSpawnPoint());
					spawned = true;
//					System.out.println("local spawn!");
					net.sendSpawn();
				}

				boolean alive = localPlayer.isAlive();

				physics.update(diff, localPlayer);

				if (alive && !localPlayer.isAlive()) {
					// kuoltiin framen aikana
					localPlayer.spawnTime = time + LocalPlayer.SPAWN_TIME;
				}

				// ampumisesta huolehtiminen
				if (localPlayer.isAlive() && localPlayer.shooting) {
					localPlayer.nextShootTime = Math.max(localPlayer.nextShootTime, prevTime);

					int maxShoots = (int)(localPlayer.shootEnergy / LocalPlayer.SHOOT_ENERGY_USE);
					int count = physics.createLocalBullets(localPlayer, prevTime, time,
							localPlayer.nextShootTime, localPlayer.nextBulletID, maxShoots);
					localPlayer.nextBulletID += count;
					localPlayer.nextShootTime += count*GamePhysics.SHOOT_INTERVAL;
					localPlayer.shootEnergy -= count*LocalPlayer.SHOOT_ENERGY_USE;
				}
				localPlayer.recoverShootEnergy(diff);

				net.updateChanges();

				frame.repaint();

				prevTime = time;
				if (time < nextTime) {
					Thread.sleep((nextTime-time)/1000000);
				}
				nextTime += FRAME_TIME;
			}
		}catch(InterruptedException e){
			// FIXME: tee jotain?
		} finally {
			try {
				net.cleanUp();
			} catch(IOException e) {
				// poikkeusta ei tarvitse käsitellä; ei suurta väliä saatiinko kaikki suljettua
			}
			frame.setVisible(false);
		}
	}
}
