package lento.gamestate;

import java.awt.geom.*;

/*
 *
 */
public class Bullet {
	Point2D.Float location;
	Point2D.Float speedVec;
	short shooter;
	short id;

	/** Päivittää ammuksen sijainnin ja nopeusvektorin.
	 * @param time edellisen ja nykyisen framen välillä kulunut aika
	 * @return vanha sijainti
	 */
	Point2D.Float update(float time) {
		float oldSpeedY = speedVec.y;
		speedVec.y += time*GamePhysics.GRAVITY;

		Point2D.Float oldLoc = location;
		location = new Point2D.Float(oldLoc.x + speedVec.x*time, oldLoc.y + .5f*(oldSpeedY+speedVec.y)*time);
		return oldLoc;
	}

	public Bullet(float x, float y, float vx, float vy, int shooter, int id) {
		location = new Point2D.Float(x,y);
		speedVec = new Point2D.Float(vx,vy);
		this.shooter = (short)shooter;
		this.id = (short)id;
	}
	public Point2D.Float getLoc() {
		return location;
	}
	public Point2D.Float getSpeedVec() {
		return speedVec;
	}
	public short getID() {
		return id;
	}
	public short getShooter() {
		return shooter;
	}
}
