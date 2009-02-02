package lento.gamestate;

import java.awt.geom.*;

/*
 *
 */
class Collision {
	public Point2D.Float location;
	public Point2D.Float normal;

	Collision(Point2D.Float loc, Point2D.Float norm) {
		location = loc;
		normal = norm;
	}
}
