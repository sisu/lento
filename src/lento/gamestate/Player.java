package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;

/**
 * Luokka sisältää yksittäisen pelaajan tiedot pelin aikana.
 * <p>
 * Player-luokka sisältää tietoa ainoastaan pelaajan omasta tilasta,
 * eikä tiedä mitään esim. sijainnin päivityksen aiheuttamista törmäyksistä.
 */
public class Player implements Comparable<Player> {

	/** Aluksen peruskiihtyvyys (pix/s^2) */
	private static final float ACCEL_SPEED = 800.0f;
	/** Aluksen maksiminopeus (pix/s) */
	private static final float MAX_SPEED = 700;
	/** Aluksen kääntymisnopeus, radiaania sekunnissa */
	private static final float TURN_SPEED = 5.0f;
	/** Pelaajan energiat syntymishetkellä */
	public static final int INITIAL_HEALTH = 1000;

	/** Pelaajan nimi */
	protected String name="";
	/** Pelaajasta ylläpidetyt tilastotiedot */
	protected int kills=0, deaths=0, damageDone=0, damageTaken=0;
	/** Sisältää tiedon, paljonko pelaajalle oli aiheutunut vahinkoa hänen viimeksi
	 * herätessään henkiin. Tämä tieto lähetetään peliin liittyvälle pelaajalle,
	 * jotta liittyvän pelaajan tiedot vastaisivat muiden tietoja pidemmällä
	 * aikavälilllä.
	 */
	protected int delayedDamageTaken=0;

	/** Aluksen väri */
	protected Color color;
	/** Pelaajan ID-numero */
	protected int id=-1;

	/** Pelaajan sijainti */
	protected Point2D.Float location=new Point2D.Float(0,0);
	/** Pelaajan edellinen sijainti; käytetään törmäystarkistuksessa. */
	protected Point2D.Float prevLocation=new Point2D.Float(0,0);
	/** Pelaajan nopeusvektori */
	protected Point2D.Float speedVec=new Point2D.Float(0,0);
	/** Kulma, johon alus on kääntynyt radiaaneina. */
	protected float angle;

	/** Tosi, joss pelaaja kiihdyttää. */
	protected boolean accelerating = false;
	/** -1, 1 tai 0 riippuen kääntyykö pelaaja myötäpäivään,
	 * vastapäivään vai ei ollenkaan. */
	protected int turning = 0;

	/** Tosi, joss pelaaja on hengissä. */
	protected boolean alive = false;
	/** Pelaajan jäljellä olevat osumapisteet. */
	int health;

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

			// Huolehdi, että kulma on väliltä 0-2pi
			float pi2 = (float)(2*Math.PI);
			angle -= pi2*Math.floor(angle/pi2);
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
//		System.out.println("spawning to "+loc.x+","+loc.y);
		location = loc;
		prevLocation = (Point2D.Float)loc.clone();
		speedVec = new Point2D.Float(0,0);
		angle = (float)(Math.PI/2); // suunta ylöspäin
		alive = true;
		health = INITIAL_HEALTH;
		delayedDamageTaken = damageTaken;
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
	/** Palauttaa pelaajan tilastotiedot.
	 * Toimii muuten täysin samoin kuin getStats(), mutta palauttaa
	 * itselle aiheutuneen vahingon kohdalla tiedon vahinkomäärästä
	 * pelaajan syntyessä.
	 */
	public int[] getOldStats() {
		return new int[]{kills,deaths,damageDone,delayedDamageTaken};
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

	/** Vertaa tätä oliota toiseen pelaaja-olioon tappojen ja kuolemien suhteen perusteella.
	 * @param pl pelaaja, johon vertaillaan
	 * @return negatiivinen tai positiivinen luku tai nolla riippuen, onko tämän
	 *         olion suhde pienempi, suurempi vai yhtäsuuri kuin vertailtavan
	 */
	public int compareTo(Player pl) {
		return (int)((long)pl.kills*deaths - (long)pl.deaths*kills);
	}
}
