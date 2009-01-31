package lento.gamestate;

import java.awt.geom.*;

/*
 *
 */
class Bullet {
	private Point2D.Float location;
	private Point2D.Float speedVec;

	/* Päivittää ammuksen sijainnin ja nopeusvektorin.
	 * @return vanha sijainti
	 */
	Point2D.Float update(float time) {
		float oldSpeedY = speedVec.y;
		speedVec.y += time*GamePhysics.GRAVITY;

		Point2D.Float oldLoc = location;
		location = new Point2D.Float(oldLoc.x + speedVec.x*time, oldLoc.y + .5f*(oldSpeedY+speedVec.y)*time);
		return oldLoc;
	}
}
