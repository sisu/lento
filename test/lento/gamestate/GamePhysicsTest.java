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
public class GamePhysicsTest {

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
		pl1.spawn(new Point2D.Float(50, 50));

		pl2 = new Player();
		pl2.id = 30;
		pl2.spawn(new Point2D.Float(100,100));

		physics.addPlayer(pl1);
		physics.addPlayer(pl2);
	}
	
	/** Yritä hakea pelaajaa, jota vastaavaa pelaaja-ID:tä ei ole lisätty. */
	@Test public void getUnknownPlayer() {
		assertNull(physics.getPlayer(10));
	}

	/** Hakee pelaajan ID-numeron perusteella. */
	@Test public void getPlayer() {
		assertEquals(pl1, physics.getPlayer(pl1.getID()));
		assertEquals(pl2, physics.getPlayer(pl2.getID()));
		assertNull(physics.getPlayer(50));
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

		assertEquals(bullets.size(), 2);
		assertEquals(bullets.get(0).getID(), nextID);
		assertEquals(bullets.get(1).getID(), nextID+1);
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
	}

	/** Tarkistaa, että pelaaja kimpoaa seinästä. */
	@Test public void wallReflection() {
		pl1.location.x = 10;
		pl1.speedVec.x = -15; 

		// pl1 lähestyy vasenta reunaa niin nopeasti, että joko
		// x-koordinaatti tulee negatiiviseksi tai tapahtuu törmäys
		physics.update(1.0f, pl1);

		assertTrue("Törmäys ei tapahtunut", pl1.location.x > 0);
	}

	/** Tarkistaa, että pelaaja kuolee törmäyksen yhteydessä, kun osumapisteet
	 * ovat riittävän alhaiset.
	 */
	@Test public void collisionDeath() {
		pl1.location.x = 10;
		pl1.speedVec.x = -50; 
		pl1.alive = true;
		pl1.health = 1;

		// pl1 lähestyy vasenta reunaa niin nopeasti, että törmää seinään
		physics.update(1.0f, pl1);

		assertFalse("Pelaajan olisi pitänyt kuolla törmäykseen", pl1.alive);
	}

	/** Tarkistaa, että ammus katoaa törmätessään seinään. */
	@Test public void bulletCollisionToWall() {
		// vasemmalle liikkuva ammus paikassa (10,100)
		Bullet b = new Bullet(10, 100, -20, 0, pl2.id, 10);
		physics.addBullet(b);

		assertEquals("Ammuksia annettu physics-oliolle tasan 1", physics.getBullets().size(), 1);

		// ammus ehtii törmätä vasempaan seinään
		physics.update(1.0f, pl1);

		assertTrue("Ammuksen olisi pitänyt kadota", physics.getBullets().isEmpty());
	}

	/** Tarkistaa, että ammus osuu pelaajaan, mutta pelaaja ei kuole, kun
	 * osumapisteitä on tarpeeksi jäljellä.
	 */
	@Test public void bulletHitPlayer() {
		// Ylöspäin nouseva ammus pl2:n alapuolella
		Point2D.Float loc = pl2.location;
		Bullet b = new Bullet(loc.x, loc.y-GamePhysics.BULLET_HIT_RANGE*2, 0, 100, pl2.id, 10);
		physics.addBullet(b);

		pl2.alive = true;
		pl2.health = GamePhysics.BULLET_DAMAGE*2;

		// ammus ehtii osua pelaajaan pl2 ja tappaa hänet
		physics.update(1.0f, pl2);

		assertTrue("Pelaajan ei pidä kuolla", pl2.alive);
		assertTrue("Ammuksen olisi pitänyt kadota", physics.getBullets().isEmpty());

		assertEquals("Itseensä osumista ei lasketa tehtyyn vahinkoon", pl2.damageDone, 0);
	}

	/** Tarkistaa, että pelaaja kuolee osuessaan ammukseen, kun pelaajan
	 * osumapisteet ovat riittävän vähissä.
	 */
	@Test public void bulletKillPlayer() {
		// Ylöspäin nouseva ammus pl2:n alapuolella
		Point2D.Float loc = pl2.location;
		Bullet b = new Bullet(loc.x, loc.y-GamePhysics.BULLET_HIT_RANGE*2, 0, 100, pl1.id, 10);
		physics.addBullet(b);

		pl2.alive = true;
		pl2.health = 1;

		// ammus ehtii osua pelaajaan pl2 ja tappaa hänet
		physics.update(1.0f, pl2);

		assertFalse("Pelaajan olisi pitänyt kuolla", pl2.alive);
		assertTrue("Ammuksen olisi pitänyt kadota", physics.getBullets().isEmpty());

		assertEquals("Tappomäärän olisi pitänyt kasvaa", pl1.kills, 1);
		assertEquals("Kuolinmäärän olisi pitänyt kasvaa", pl2.deaths, 1);

		assertEquals("Tehty vahinko huomioitava", pl1.damageDone, GamePhysics.BULLET_DAMAGE);
	}

	/** Yrittää poistaa pelistä null-pelaajan. */
	@Test(expected=NullPointerException.class) public void nullPlayerDelete() {
		physics.deletePlayer(null);
	}

	/** Poistaa pelistä pelaajan pl1. */
	@Test public void playerDelete() {
		physics.deletePlayer(pl1);

		assertEquals("Pelaajamäärän olisi pitänyt muuttua", physics.getPlayers().size(), 1);
		assertNull("Pelaajaa ei pitäisi löytyä", physics.getPlayer(pl1.id));
	}
}
