package lento.net;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.*;
import java.net.*;
import java.util.*;

import lento.gamestate.*;
import lento.gameui.PlayerCreator;

/**
 * Testaa NetListener-luokan metodeja.
 */
public class NetListenerTest {

	/** Testeissä käytetyt NetListener-oliot. */
	NetListener nl1, nl2;

	/** NetListener-olioiden paikalliset pelaaja-oliot. */
	Player pl1, pl2;

	/** Luo NetListener-oliot muuttujiin nl1 ja nl2 ja luo niiden välille yhteyden
	 */
	@Before public void init() throws IOException {
		GamePhysics phys1 = new GamePhysics();
		pl1 = PlayerCreator.makeLocalPlayer("pl1", Color.red, -1);
		nl1 = new NetListener(phys1, pl1);

		GamePhysics phys2 = new GamePhysics();
		// aseta pelialue kuntoon
		phys2.getGeometry().resetArea(500,500);
		phys2.getGeometry().setBorderColor(Color.white);

		pl2 = PlayerCreator.makeLocalPlayer("pl2", Color.blue, 15);
		nl2 = new NetListener(phys2, pl2);

		// ala kuunnella yhteyksiä
		new Thread(nl2).start();

		// ota yhteys toiseen NetListener-olioon
		InetAddress localhost = InetAddress.getByName("localhost");
		int id = nl1.connect(localhost, nl2.tcpSocket.getLocalPort());
		PlayerCreator.setLocalPlayerID(pl1, id);

		new Thread(nl1).start();
	}
	/** Vapauttaa NetListener-olioille varatut resurssit. */
	@After public void cleanUp() throws IOException {
		nl1.cleanUp();
		nl2.cleanUp();
	}

	/** Luo ammuksen ja lähettää sen nl1:n kautta nl2:lle. */
	@Test public void sendShoot() throws IOException, InterruptedException {
		Bullet b = new Bullet(100, 100, 50, 50, pl1.getID(), 10);
		// Lähetä ammus verkkoon
		nl1.shoot(b);
		nl1.updateChanges();

		// Odotetaan, että ammus menee perille
		Thread.sleep(50);

		// Tarkista, että ammus saapui perille
		ArrayList<Bullet> bullets = nl2.physics.getBullets();
		assertEquals(bullets.size(), 1);

		// Tarkista, että ammus on mitä pitäisikin
		Bullet b2 = bullets.get(0);
		assertEquals(b2.getShooter(), pl1.getID());
		assertEquals(b2.getID(), 10);
	}

	/** Lähettää pl1:n syntymisviestin ja koordinaatit nl2-oliolle. */
	@Test public void sendCoordinates() throws IOException, InterruptedException {
		pl1.spawn(new Point2D.Float(100, 100));
		nl1.sendSpawn();

		// Päivitetään pl1:n paikka
		nl1.physics.update(100, pl1);

		// Lähetetään päivitetyt tiedot
		nl1.updateChanges();
		Thread.sleep(50);

		// varmistetaan, että päivitys meni perille
		Player pl = nl2.physics.getPlayer(pl1.getID());
		assertNotNull("Pelaajan tiedot löydyttävä.", pl);
		assertEquals("Sijainnin päivityttävä.", pl.getLoc(), pl1.getLoc());
		assertEquals("Nopeusvektorin päivityttävä.", pl.getSpeedVec(), pl1.getSpeedVec());
	}

	/** Luo ammuksen nl2:lla, lähettää siitä tiedon nl1:lle ja lähettää
	 * nl1:ltä nl2:lle tiedon, että ammus osui pelaajaan pl1. */
	@Test public void sendBulletHit() throws IOException, InterruptedException {
		pl1.spawn(new Point2D.Float(100,100));
		nl1.sendSpawn();

		pl2.spawn(new Point2D.Float(100,200));
		nl2.sendSpawn();

		// luodaan ammus nl2:lla
		Bullet b = new Bullet(100, 180, 0, -50, pl2.getID(), 10);
		nl2.physics.addBullet(b);
		nl2.shoot(b);

		// Lähetään ammus nl2 -> nl1
		nl2.updateChanges();
		Thread.sleep(50);

		// Tarkistetaan, että ammus saapui perille nl1:llä
		assertEquals("Ammuksen olisi pitänyt tulla perille.", nl1.physics.getBullets().size(), 1);
		Bullet receivedBullet = nl1.physics.getBullets().get(0);
		assertEquals(receivedBullet.getShooter(), pl2.getID());
		assertEquals(receivedBullet.getID(), 10);

		// lähetä tieto osumisesta nl1 -> nl2
		nl1.hit(receivedBullet);
		nl1.updateChanges();
		Thread.sleep(50);

		// Tarkistetaan, että tieto osumasta saapui perille nl2:lla
		assertEquals("Ammuksen olisi pitänyt poistua.", nl2.physics.getBullets().size(), 0);
	}

	/** Poistaa nl1:een liittyneen etäpelaajan pelistä. */
	@Test public void deletePlayer() {
		NetPlayer pl = nl1.players.get(0);
		nl1.deletePlayer(pl);
		assertEquals("Ainoa etäpelaaja poistettu.", nl1.players.size(), 0);
	}
	/** Yrittää poistaa null-pelaajan pelistä. */
	@Test(expected=NullPointerException.class) public void deleteNullPlayer() {
		nl1.deletePlayer(null);
	}
}
