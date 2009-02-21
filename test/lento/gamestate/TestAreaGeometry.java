package lento.gamestate;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.*;

/**
 * Testaa AreaGeometry-luokan metodeita.
 */
public class TestAreaGeometry {

	/** Yrittää antaa null-viitteen tiedostoparametrina AreaGeometryn konstruktorille.
	 * Tästä pitäisi seurata poikkeus.
	 */
	@Test(expected=Exception.class) public void nullFileLoad() throws IOException {
		new AreaGeometry(null);
	}
	// FIXME: tee paremmin?
	@Test public void fileLoadTest() {
		AreaGeometry geom;
		// Yritetään antaa eri rikkinäisiä tiedostoja AreaGeometrylle
		for(int i=1; i<=3; ++i) {
			File f = new File("./testfiles/broken"+i+".lev");
			try {
				geom = new AreaGeometry(f);
				assertTrue("AreaGeometryn konstruktori hyväksyi rikkinäisen tiedoston "+i, false);
			} catch(Exception e) {
				assertTrue("ok, poikkeus", true);
			}
		}
	}
	/** Yrittää antaa null-viitteen getCollision-metodin parametriksi.
	 */
	@Test(expected=Exception.class) public void nullCollision() {
		AreaGeometry geom = new AreaGeometry();
		geom.getCollision(null,null);
	}

	/** Luo pelialueen, ja testaa törmäysten tunnistusta.
	 */
	@Test public void collisionTest() {
		AreaGeometry geom = null;
		try {
			File f = new File("./testfiles/test1.lev");
			geom = new AreaGeometry(f);
		} catch(IOException e) {
			String curDir = System.getProperty("user.dir");
			assertTrue("Testikenttätiedoston lataus epäonnistui: "+curDir, false);
		}

		Point2D.Float a=new Point2D.Float(), b=new Point2D.Float();

		// testaa tapausta, jossa törmäystä ei tapahdu
		a.setLocation(47,5);
		b.setLocation(57,15);
		Collision coll = geom.getCollision(a,b);
		assertNull("törmäystä ei pitäisi tapahtua", coll);

		// testaa törmäystä pelialueen reunaan
		a.setLocation(100,5);
		b.setLocation(100,-5);
		coll = geom.getCollision(a,b);
		assertNotNull(coll);
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
}
