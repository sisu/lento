package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;

/**
 * Luokka sisältää yksittäisen pelaajan tiedot pelin aikana.
 */
public class Player {

	private static final float ACCEL_SPEED = 800.0f;
	private static final float MAX_SPEED = 700;
	private static final float TURN_SPEED = 5.0f;
	public static final int INITIAL_HEALTH = 1000;

	protected String name="";
	protected int kills=0, deaths=0, damageDone=0, damageTaken=0;
	protected Color color;
	protected int id=-1;

	protected Point2D.Float location=new Point2D.Float(0,0);
	protected Point2D.Float speedVec=new Point2D.Float(0,0);
	protected Point2D.Float prevLocation=new Point2D.Float(0,0);
	protected float angle;

	protected boolean accelerating = false;
	protected int turning = 0;

	protected boolean alive = false;
	int health;

	/** Taulukko siitä, missä indeksissä mikäkin tämän pelaajan ampuma 
	 * ammus sijaitsee GamePhysics-olion taulukossa.
	 */
	int[] bulletIndex = new int[65536];

	/** Päivittää pelaajan sijainnin ja nopeusvektorin, ja sijoittaa edellisen sijainnin
	 * prevLocation-muuttujaan. Tämä metodi ei tarkista mitään törmäyksiä, vaan olettaa
	 * pelaajan lentävän tyhjässä tilassa.
	 * @param time nykyisen ja edellisen framen välillä kulunut aika sekunteina
	 */
	void update(float time) {
		// FIXME: tee FPS:stä riippumaton
		angle += time*turning;

		float prevVY = speedVec.y, prevVX = speedVec.x;
		speedVec.y += time*GamePhysics.GRAVITY;

		prevLocation.x=location.x;
		prevLocation.y=location.y;

		if (turning!=0) {
			angle += turning*time*TURN_SPEED;
			angle = (float)fmod(angle, 2*Math.PI);
		}

		if (accelerating) {
			float sa = (float)Math.sin(angle);
			float ca = (float)Math.cos(angle);

			final float slowDown = ACCEL_SPEED/MAX_SPEED;
			speedVec.x += ca*time*ACCEL_SPEED - time*speedVec.x*slowDown;
			speedVec.y += -sa*time*ACCEL_SPEED - time*speedVec.y*slowDown;
		}
		location.x += .5f*(prevVX+speedVec.x)*time;
		location.y += .5f*(prevVY+speedVec.y)*time;
	}
	private static double fmod(double a, double b) {
		return a-b*Math.floor(a/b);
	}

	/** Palauttaa pelaajan nykyisen sijainnin.
	 * @return nykyinen sijainti
	 */
	public Point2D.Float getLoc() {
		return location;
	}
	/** Palauttaa pelaajan nykyisen nopeusvektorin.
	 * @return nykyinen nopeusvektori
	 */
	public Point2D.Float getSpeedVec() {
		return speedVec;
	}
	/** Palauttaa pelaajan aluksen värin.
	 * @return aluksen väri
	 */
	public Color getColor() {
		return color;
	}
	/** Synnyttää kuolleen pelaajan henkiin, ja asettaa pelaajalle uudet sijaintitiedot.
	 * Metodi asettaa aluksen nopeusvektoriksi nollavektorin ja kulman sellaiseksi,
	 * että alus osoittaa suoraan ylöspäin.
	 * Pelaajan osumapisteet asetetaan maksimiarvoonsa.
	 * @param loc paikka, johon pelaaja syntyy.
	 */
	public void spawn(Point2D.Float loc) {
		System.out.println("spawning to "+loc.x+","+loc.y);
		location = loc;
		prevLocation = new Point2D.Float(loc.x,loc.y);
		speedVec = new Point2D.Float(0,0);
		angle = (float)(Math.PI/2);
		alive = true;
		health = INITIAL_HEALTH;
	}
	/** Palauttaa tiedon, onko pelaaja hengissä.
	 * @return true, jos pelaaja on hengissä
	 * @return false, jos pelaaja on kuollut
	 */
	public boolean isAlive() {
		return alive;
	}
	/** Palauttaa kulman, johon pelaajan alus on kääntynyt.
	 * Kulma on aina väliltä 0-2pi.
	 * @return kulma, johon alus on kääntynyt
	 */
	public float getAngle() {
		return angle;
	}
	/** Palauttaa pelaajan nimen.
	 * @return pelaajan nimi
	 */
	public String getName() {
		return name;
	}
	/** Palauttaa pelaajan tilastotiedot.
	 * Tilastotiedot palautetaan taulukkona järjestyksessä:
	 * tapot, kuolemat, pelaajan tekemä vahinko, pelaajalle aiheutunut vahinko.
	 * @return 4:n mittainen taulukko tilastotiedoista
	 */
	public int[] getStats() {
		return new int[]{kills,deaths,damageDone,damageTaken};
	}
	/** Palauttaa pelaajan pelaaja-ID:n.
	 * @return pelaajan ID
	 */
	public int getID() {
		return id;
	}
	/** Palauttaa tiedon, kiihdyttääkö pelaaja alustaan.
	 * @return true, jos pelaaja kiihdyttää
	 * @return false, jos pelaaja ei kiihdytä.
	 */
	public boolean getAccelerating() {
		return accelerating;
	}
	/** Palauttaa tiedon siitä, mihin suuntaan pelaaja on kääntämässä alustaan.
	 * @return 0, jos alus ei ole kääntymässä
	 * @return 1, jos alus on kääntymässä vastapäivään
	 * @return -1, jos alus on kääntymässä myötäpäivään
	 */
	public int getTurning() {
		return turning;
	}
	/** Kasvattaa pelaajan tekemien tappojen määrää yhdellä.
	 */
	public void addKills() {
		kills++;
	}
	/** Palauttaa pelaajan jäljellä olevat osumapisteet.
	 * @return osumapisteet, jotka pelaajalla on jäljellä.
	 */
	public int getHealth() {
		return health;
	}
	/** Kasvattaa pelaajan tuottamaa vahinkomäärää yhden ammuksen tuottaman
	 * vahinkomäärän verran.
	 */
	public void addHitDone() {
		damageDone += GamePhysics.BULLET_DAMAGE;
	}
}
