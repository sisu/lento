package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;

/*
 * Luokka sisältää yksittäisen pelaajan tiedot pelin aikana.
 */
class Player {

	private final float ACCEL_SPEED = 1.0f;
	private final float TURN_SPEED = 1.0f;

	String name="";
	int kills=0, deaths=0, damageDone=0, damageTaken=0;
	Color color;
	int id;

	Point2D.Float location;
	Point2D.Float speedVec;
	Point2D.Float prevLocation;
	float angle;

	private boolean accelerating;
	private int turning;

	void update(float time) {
		// FIXME: tee FPS:stä riippumaton
		angle += time*turning;

		float prevSpeedY = speedVec.y;
		speedVec.y += time*GamePhysics.GRAVITY;

		prevLocation = location;
	}
}
