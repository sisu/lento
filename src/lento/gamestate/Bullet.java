package lento.gamestate;

import java.awt.geom.*;

/*
 * Sisältää tiedot yhdestä ilmassa olevasta ammuksesta.
 */
public class Bullet {
	Point2D.Float location;
	Point2D.Float speedVec;
	short shooter;
	short id;

	/** Päivittää ammuksen sijainnin ja nopeusvektorin.
	 * @param time edellisen ja nykyisen framen välillä kulunut aika sekunteina
	 * @return vanha sijainti
	 */
	Point2D.Float update(float time) {
		float oldSpeedY = speedVec.y;
		speedVec.y += time*GamePhysics.GRAVITY;

		Point2D.Float oldLoc = location;
		location = new Point2D.Float(oldLoc.x + speedVec.x*time, oldLoc.y + .5f*(oldSpeedY+speedVec.y)*time);
		return oldLoc;
	}

	/** Luo uuden ammuksen.
	 * @param x lähtöpaikan x-koordinaatti
	 * @param y lähtöpaikan y-koordinaatti
	 * @param vx nopeusvektorin x-komponentti
	 * @param vy nopeusvektorin y-komponentti
	 * @param shooter ampujan pelaaja-ID
	 * @param id ammuksen ID
	 */
	public Bullet(float x, float y, float vx, float vy, int shooter, int id) {
		location = new Point2D.Float(x,y);
		speedVec = new Point2D.Float(vx,vy);
		this.shooter = (short)shooter;
		this.id = (short)id;
	}

	/** Palauttaa ammuksen nykyisen sijainnin.
	 * @return ammuksen nykyinen sijainti
	 */
	public Point2D.Float getLoc() {
		return location;
	}
	/** Palauttaa ammuksen nykyisen nopeusvektorin.
	 * @return ammuksen nykyinen nopeusvektori
	 */
	public Point2D.Float getSpeedVec() {
		return speedVec;
	}
	/** Palauttaa ammuksen ID-numeron.
	 * @return ammuksen ID-numero
	 */
	public short getID() {
		return id;
	}
	/** Palauttaa ammuksen ampujan pelaaja-ID:n.
	 * @return ampujan pelaaja-ID
	 */
	public short getShooter() {
		return shooter;
	}
}
