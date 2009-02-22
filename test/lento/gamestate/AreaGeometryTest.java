package lento.gamestate;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.*;

/**
 * Testaa AreaGeometry-luokan metodeita.
 */
public class AreaGeometryTest {

	/** Testeissä käytetty AreaGeometry-olio. */
	AreaGeometry geometry;

	/** Luo geometry-muuttujaan uuden pelialueen, jossa on 1 polygoni. */
	@Before public void init() throws IOException {
		geometry = loadFromFile(
				"1000 1000 ff00\n" +
				"ff 10 10 50 10 50 50 10 50");
	}

	/** Yrittää antaa null-viitteen tiedostoparametrina AreaGeometryn konstruktorille.
	 */
	@Test(expected=Exception.class) public void nullFileLoad() throws IOException {
		new AreaGeometry(null);
	}
	/** Yrittää luoda negatiivisen kokoisen pelialueen tiedostosta lataamalla. */
	@Test(expected=IOException.class) public void negativeAreaSizeFile() throws IOException {
		loadFromFile("-1 100 ff00ff");
	}
	/** Yrittää ladata tiedostosta pelialueen, jossa reunan väri ei ole hyväksyttävä arvo. */
	@Test(expected=IOException.class) public void illegalColorValueFile() throws IOException {
		loadFromFile("100 100 1234567");
	}
	/** Yrittää ladata tiedoston, jossa on annettu eri määrä polygonin x- ja y-komponentteja. */
	@Test(expected=IOException.class) public void wrongCoordinateCountFile() throws IOException {
		loadFromFile("100 100 aabbcc\n" + "ccddee 10 10 50 10 50");
	}

	/** Yrittää antaa null-viitteen getCollision-metodin parametriksi.
	 */
	@Test(expected=Exception.class) public void nullCollision() {
		AreaGeometry geom = new AreaGeometry();
		geom.getCollision(null,null);
	}

	/** Testaa törmäystarkistusta tapauksessa, jossa törmäystä ei tapahdu. */
	@Test public void noCollisionTest() {
		Point2D.Float a = new Point2D.Float(47, 5);
		Point2D.Float b = new Point2D.Float(57, 15);

		Collision coll = geometry.getCollision(a,b);
		assertNull("törmäystä ei pitäisi tapahtua", coll);
	}
	/** Testaa törmäystarkistusta tapauksessa, jossa törmätään pelialueen reunaan. */
	@Test public void collisionEdgeTest() {
		Point2D.Float a = new Point2D.Float(100,5);
		Point2D.Float b = new Point2D.Float(100,-5);

		Collision coll = geometry.getCollision(a,b);
		assertNotNull("Törmäyksen pitäisi tapahtua", coll);
	}
	/** Testaa törmäystarkistusta tapauksessa, jossa törmätään polygoniin. */
	@Test public void collisionPolygonTest() {
		Point2D.Float a = new Point2D.Float(25,5);
		Point2D.Float b = new Point2D.Float(25,15);

		Collision coll = geometry.getCollision(a,b);
		assertNotNull("Törmäyksen pitäisi tapahtua", coll);
	}

	/** Luo pelialueen ja lisää sinne polygonin. */
	@Test public void validCreateTest() {
		AreaGeometry geom = new AreaGeometry();
		geom.resetArea(1000, 1000);

		ColoredPolygon p = new ColoredPolygon();
		p.addPoint(10, 10);
		p.addPoint(200, -10);
		p.addPoint(200, 100);

		geom.addPolygon(p);

		// Varmista, että ainoastaan annettu polygoni on mukana polygonilistassa
		assertEquals(geom.getPolygons().size(), 1);
		assertTrue(geom.getPolygons().get(0).equals(p));
	}

	/** Yrittää antaa null-viitteen reunan väriksi. */
	@Test(expected=IllegalArgumentException.class) public void nullColorTest() {
		AreaGeometry geom = new AreaGeometry();
		geom.setBorderColor(null);
	}
	/** Yrittää antaa null-viitteen polygoniksi. */
	@Test(expected=IllegalArgumentException.class) public void nullPolyTest() {
		AreaGeometry geom = new AreaGeometry();
		geom.addPolygon(null);
	}
	/** Yrittää tehdä pelialueen koosta negatiivisen. */
	@Test(expected=IllegalArgumentException.class) public void negativeSizeCreateTest() {
		AreaGeometry geom = new AreaGeometry();
		// Yritetään tehdä -100x100-kokoinen pelialue
		geom.resetArea(-100,100);
	}
	/** Yrittää lisätä pelialueeseen monikulmion jossa on alle kolme kulmaa. */
	@Test(expected=IllegalArgumentException.class) public void tooLittlePolygonEdges() {
		AreaGeometry geom = new AreaGeometry();
		geom.resetArea(1000,1000);

		ColoredPolygon p = new ColoredPolygon();
		p.addPoint(10,10);
		p.addPoint(20,20);

		// yritetään lisätä polygoni, jossa on vain 2 pistettä
		geom.addPolygon(p);
	}

	/** Luo väliaikaistiedoston, lataa sen AreaGeometry-olioon, vapauttaa tiedoston
	 * ja lopulta palauttaa luodun AreaGeometry-olion.
	 *
	 * @param contents merkkijono, joka kirjoitetaan tiedostoon.
	 * @return luotu AreaGeometry-olio
	 *
	 * @throws IOException tiedoston lataus tai luonti epäonnistuu
	 */
	private static AreaGeometry loadFromFile(String contents) throws IOException {
		File f = null;
		AreaGeometry geom = null;
		try {
			f = File.createTempFile("test", ".lev");
			FileOutputStream out = new FileOutputStream(f);
			out.write(contents.getBytes());
			out.flush();
			out.close();

			geom = new AreaGeometry(f);
		} finally {
			f.delete();
		}
		return geom;
	}
}
