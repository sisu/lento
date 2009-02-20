package lento.gamestate;

import java.awt.geom.*;

/**
 * Bullet sisältää tiedot yhdestä ilmassa olevasta ammuksesta.
 * Tämä luokka ei tiedä huolehdi itse mitenkään mm. törmäyksistä, vaan
 * kykenee ainoastaan päivittämään sijaintiaan, kuin ammus olisi
 * tyhjässä tilassa.
 */
public class Bullet {

	/** Ammuksen sijainti */
	Point2D.Float location;
	/** Ammuksen nopeusvektori */
	Point2D.Float speedVec;

	/** Ampujan pelaaja-ID */
	short shooter;
	/** Ammuksen ID */
	short id;

	/** Päivittää ammuksen sijainnin ja nopeusvektorin.
	 * @param time edellisen ja nykyisen framen välillä kulunut aika sekunteina
	 * @return vanha sijainti
	 */
	Point2D.Float update(float time) {
		float oldSpeedY = speedVec.y;
		// v = v0 + a*t
		speedVec.y += time*GamePhysics.GRAVITY;

		Point2D.Float oldLoc = location;
		// x-suuntainen nopeus on vakio
		// y-suuntainen nopeus kasvaa tasaisesti, eli y = y0 + t*(v0+v)/2
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
