package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;

/*
 * Luokka sisältää yksittäisen pelaajan tiedot pelin aikana.
 */
public class Player {

	private final float ACCEL_SPEED = 1.0f;
	private final float TURN_SPEED = 1.0f;

	String name="";
	int kills=0, deaths=0, damageDone=0, damageTaken=0;
	Color color;
	int id;

	Point2D.Float location=new Point2D.Float(0,0);
	Point2D.Float speedVec=new Point2D.Float(0,0);
	Point2D.Float prevLocation=null;
	float angle;

	protected boolean accelerating = false;
	private int turning = 0;

	boolean alive = false;

	void update(float time) {
		// FIXME: tee FPS:stä riippumaton
		angle += time*turning;

		float prevSpeedY = speedVec.y;
		speedVec.y += time*GamePhysics.GRAVITY;

		prevLocation = location;
	}

	public Point2D.Float getLoc() {
		return location;
	}
	public Color getColor() {
		return color;
	}
}
