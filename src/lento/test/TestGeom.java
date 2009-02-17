package lento.gamestate;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.awt.geom.*;

public class TestGeom {
	@Test public void fileLoadTest() {
		AreaGeometry geom;
		// Yritetään antaa tyhjä tiedosto AreaGeometrylle
		try {
			geom = new AreaGeometry(null);
			assertTrue("AreaGeometryn konstruktori hyväksyi null-viitteen.", false);
		} catch(Exception e) {
			assertTrue("ok, poikkeus", true);
		}
		// Yritetään antaa eri rikkinäisiä tiedostoja AreaGeometrylle
		for(int i=1; i<=3; ++i) {
			File f = new File("../test/broken"+i+".lev");
			try {
				geom = new AreaGeometry(f);
				assertTrue("AreaGeometryn konstruktori hyväksyi rikkinäisen tiedoston "+i, false);
			} catch(Exception e) {
				assertTrue("ok, poikkeus", true);
			}
		}
	}
	@Test public void collisionTest() {
		AreaGeometry geom = null;
		try {
			File f = new File("../test/test1.lev");
//			geom = new AreaGeometry(new File("lento/test/test1.lev"));
			geom = new AreaGeometry(f);
		} catch(Exception e) {
			String curDir = System.getProperty("user.dir");
			assertTrue("Testikenttätiedoston lataus epäonnistui: "+curDir, false);
		}

		// testaa törmäystarkistusta null-viitteellä
		try {
			geom.getCollision(null,null);
			assertTrue("getCollision hyväksyi null-viitteen.", false);
		} catch(Exception e) {
			assertTrue("ok, poikkeus", true);
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
	@Test public void createTest() {
		AreaGeometry geom = new AreaGeometry();
		geom.resetArea(1000, 1000);
	}
}
