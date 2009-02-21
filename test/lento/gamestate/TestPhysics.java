package lento.gamestate;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.*;
import java.util.*;

/**
 * Testaa GamePhysics-luokaa metodeita.
 */
public class TestPhysics {

	private GamePhysics physics;
	private Player pl1;
	private Player pl2;

	/** Luo GamePhysics-olion ja lisää sillä 2 pelaajaa.
	 */
	@Before public void init() {
		physics = new GamePhysics();

		// Aseta pelialueen kooksi 500x500
		physics.getGeometry().resetArea(500, 500);

		pl1 = new Player();
		pl1.id = 25;

		pl2 = new Player();
		pl2.id = 30;

		resetPlayers();

		physics.addPlayer(pl1);
		physics.addPlayer(pl2);
	}
	/** Asettaa olioille pl1 ja pl2 lähtöpaikat. */
	private void resetPlayers() {
		pl1.spawn(new Point2D.Float(50, 50));
		pl2.spawn(new Point2D.Float(100,100));
	}
	
	/** Yritä hakea pelaajaa, jota vastaavaa pelaaja-ID:tä ei ole lisätty. */
	@Test public void getUnknownPlayer() {
		assertNull(physics.getPlayer(10));
	}

	/** Hakee pelaajan ID-numeron perusteella. */
	@Test public void getPlayer() {
		assertEquals(pl1, physics.getPlayer(pl1.getID()));
		assertEquals(pl2, physics.getPlayer(pl2.getID()));
	}

	/** Generoi ammuksia GamePhysics.createLocalBullets-metodilla. */
	@Test public void createBullets() {
		final long t = GamePhysics.SHOOT_INTERVAL;

		long prevTime = 100;
		long curTime = prevTime + 2*t;
		long nextShootTime = prevTime + t/2;
		int nextID = 15;
		int maxCount = 5;

		int count = physics.createLocalBullets(pl1, prevTime, curTime,
				nextShootTime, nextID, maxCount);

		assertEquals("Luotiin väärä määrä ammuksia ("+count+")", count, 2);

		// tarkista, että luoduilla ammuksilla on oikeat ID-numerot
		ArrayList<Bullet> bullets = physics.getBullets();

		assertEquals(bullets.get(0).getID(), nextID);
		assertEquals(bullets.get(1).getID(), nextID+1);

		// Palauta physics-olio vastaamaan tilannetta ennen testin alkua
		bullets.clear();
	}

	/** Tarkistaa, että pelaajaa liikutetaan joss pelaaja on hengissä. */
	@Test public void moveTest() {
		pl1.alive = false;
		pl2.alive = true;

		Point2D.Float origLoc1 = (Point2D.Float)pl1.location.clone();
		Point2D.Float origLoc2 = (Point2D.Float)pl2.location.clone();

		physics.update(0.1f, pl1);

		assertTrue("Kuollut pelaaja ei liiku", origLoc1.equals(pl1.location));
		assertFalse("Elossa oleva pelaaja liikkuu", origLoc2.equals(pl2.location));

		// Palauta Player-olioiden tile ennalleen
		resetPlayers();
	}

	/** Tarkistaa, että pelaaja kimpoaa seinästä. */
	@Test public void collisionTest() {
		pl1.location.x = 10;
		pl1.speedVec.x = -15; 

		// pl1 lähestyy vasenta reunaa niin nopeasti, että joko
		// x-koordinaatti tulee negatiiviseksi tai tapahtuu törmäys
		physics.update(1.0f, pl1);

		assertTrue("Törmäys ei tapahtunut", pl1.location.x > 0);

		resetPlayers();
	}

	/** Tarkistaa, että pelaaja kuolee törmäyksen yhteydessä, kun osumapisteet
	 * ovat riittävän alhaiset.
	 */
	@Test public void collisionDieTest() {
		pl1.location.x = 10;
		pl1.speedVec.x = -50; 
		pl1.alive = true;
		pl1.health = 1;

		// pl1 lähestyy vasenta reunaa niin nopeasti, että törmää seinään
		physics.update(1.0f, pl1);

		assertFalse("Pelaajan olisi pitänyt kuolla törmäykseen", pl1.alive);

		resetPlayers();
	}
}
