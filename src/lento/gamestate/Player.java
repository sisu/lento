package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;

/*
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

	public Point2D.Float getLoc() {
		return location;
	}
	public Point2D.Float getSpeedVec() {
		return speedVec;
	}
	public Color getColor() {
		return color;
	}
	public void spawn(Point2D.Float loc) {
		System.out.println("spawning to "+loc.x+","+loc.y);
		location = loc;
		prevLocation = new Point2D.Float(loc.x,loc.y);
		speedVec = new Point2D.Float(0,0);
		angle = (float)(Math.PI/2);
		alive = true;
		health = INITIAL_HEALTH;
	}
	public boolean isAlive() {
		return alive;
	}
	public float getAngle() {
		return angle;
	}
	public String getName() {
		return name;
	}
	public int[] getStats() {
		return new int[]{kills,deaths,damageDone,damageTaken};
	}
	public int getID() {
		return id;
	}
	public boolean getAccelerating() {
		return accelerating;
	}
	public int getTurning() {
		return turning;
	}
	public void setID(int id) {
		this.id=id;
	}
	public void addKills() {
		kills++;
	}
	public int getHealth() {
		return health;
	}
	public void addHitDone() {
		damageDone += GamePhysics.BULLET_DAMAGE;
	}
}
