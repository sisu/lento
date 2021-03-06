package lento.gamestate;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.*;

/**
 * Testaa Player-luokan metodeja.
 */
public class PlayerTest {

	/** Testeissä käytetyt pelaaja-oliot. */
	private Player p1,p2;

	/** Alusta pelaaja-oliot. */
	@Before public void init() {
		p1 = new Player();
		p2 = new Player();
	}
	
	/** Testaa pelaajien vertailuoperaatiota, kun toisella pelaajalla on
	 * enemmän tappoja ja vähemmän kuolemia.
	 */
	@Test public void simpleCompare() {
		p1.kills = 2;
		p1.deaths = 1;
		p2.kills = 1;
		p2.deaths = 2;

		assertTrue("Enemmän tappanut ja vähemmän kuollut ensin", p1.compareTo(p2) < 0);
	}

	/** Testaa pelaajien vertailuoperaatiota, kun toisella pelaajalla on
	 * enemmän tappoja ja kuolemia, mutta huonompi suhde.
	 */
	@Test public void ratioCompare() {
		p1.kills = 100;
		p1.deaths = 99;
		p2.kills = 1000;
		p2.deaths = 998;

		assertTrue("Tappo-kuolema-suhde ratkaisee järjestyksen", p1.compareTo(p2) < 0);
	}

	/** Testaa pelaajien vertailuoperaatiota, kun toisella pelaajalla on
	 * enemmän tappoja ja kuolemia, mutta suhteet ovat samat.
	 */
	@Test public void equalCompareTest() {
		p1.kills = 10;
		p1.deaths = 20;
		p2.kills = 40;
		p2.deaths = 80;

		assertTrue("Suhteen ollessa sama pelaajien vertailun oltava 0", p1.compareTo(p2) == 0);
	}

	// Vertailufunktion erikoistapaukset

	/** Testaa vertailua, kun vertailtava 1 ei ole tappanut ketään. */
	@Test public void noOwnKillsCompare() {
		p1.kills=0;
		p2.kills=1;
		assertTrue(p1.compareTo(p2) > 0);
	}
	/** Testaa vertailua, kun vertailtava 2 ei ole tappanut ketään. */
	@Test public void noOtherKillsCompare() {
		p1.kills=1;
		p2.kills=0;
		assertTrue(p1.compareTo(p2) < 0);
	}
	/** Testaa vertailua, kun vertailtava 1 ei ole kuollut kertaakaan. */
	@Test public void noOwnDeathsCompare() {
		p1.deaths=0;
		p2.deaths=1;
		assertTrue(p1.compareTo(p2)<0);
	}
	/** Testaa vertailua, kun vertailtava 2 ei ole kuollut kertaakaan. */
	@Test public void noOtherDeathsCompare() {
		p1.deaths=1;
		p2.deaths=0;
		assertTrue(p1.compareTo(p2)>0);
	}
	/** Testaa vertailua, kun kumpikaan vertailtava ei ole tappanut ketään. */
	@Test public void bothZeroKillsCompare() {
		p1.deaths=1;
		p2.deaths=2;
		assertTrue(p1.compareTo(p2)<0);
		assertTrue(p2.compareTo(p1)>0);
	}
	/** Testaa vertailua, kun kumpikaan vertailtava ei ole kuollut kertaakaan. */
	@Test public void bothZeroDeathsCompare() {
		p1.kills=1;
		p2.kills=2;
		assertTrue(p1.compareTo(p2)>0);
		assertTrue(p2.compareTo(p1)<0);
	}
}
