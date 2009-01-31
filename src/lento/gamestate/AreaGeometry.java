package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;

/*
 *
 */
class AreaGeometry {

	private Polygon[] polygons;
	private Edge[] edges;

	Collision getCollision() {
		return null;
	}

	private class Edge {
		Point2D start,end,normal;
	}
}
