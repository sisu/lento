package lento.gamestate;

import java.awt.geom.*;

/**
 * Sisältää tiedon kentän esteen tai reunan ja siihen osuneen
 * välisestä törmäyksestä.
 */
class Collision {
	/** Paikka, jossa törmäys tapahtui. */
	public Point2D.Float location;

	/** Pinnan, johon törmättiin, normaalivektori.
	 * Tämä on aina yksikkövektori.
	 */
	public Point2D.Float normal;

	/** Luo tiedon uudesta törmäyksestä.
	 * @param loc törmäyspaikka
	 * @param norm törmäyspinnan normaalivektori törmäyskohdassa
	 */
	Collision(Point2D.Float loc, Point2D.Float norm) {
		location = loc;
		normal = norm;
	}
}
