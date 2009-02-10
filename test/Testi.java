import org.junit.*;
import static org.junit.Assert.*;

import lento.gamestate.*;
import java.io.*;

public class Testi {
	@Test public void testaa() {
		AreaGeometry geom;
		try {
			geom = new AreaGeometry();
//			geom = new AreaGeometry((File)null);
			fail("AreaGeometry-konstruktori hyväksyi null-viitteen.");
		} catch(Exception e) {
			assertTrue("AreaGeometry(null) -> poikkeus, ok", true);
		}
	}
//	public static junit.framework.Test suite() {
//		return new junit.framework.JUnit4TestAdapter(Testi.class);
//	}
};
