package lento.gamestate;

import java.util.*;
import java.awt.geom.*;
import java.io.IOException;

/*
 * GamePhysics pitää kirjaa pelialueesta ja kaikista pelin pelaajista ja ammuksista, ja huolehtii näiden päivityksistä.
 */
class GamePhysics {

	static final float GRAVITY = 1.0f;
//	static final float 

	private AreaGeometry geometry;
	private ArrayList<Player> players = new ArrayList<Player>();
	private ArrayList<Bullet> bullets = new ArrayList<Bullet>();

	public GamePhysics(String filename) throws IOException {
		geometry = new AreaGeometry(filename);
	}
	public GamePhysics() {
		geometry = new AreaGeometry();
	}

	public void update(float time, Player localPlayer) {
		for(Iterator<Player> i=players.iterator(); i.hasNext(); )
			i.next().update(time);
		for(Iterator<Bullet> i=bullets.iterator(); i.hasNext(); ) {
			Bullet b = i.next();
			Point2D.Float prevLoc = b.update(time);
			for(Iterator<Player> j=players.iterator(); j.hasNext(); )
				checkHit(j.next().location, prevLoc, b.location);
		}
	}

	public void addPlayer(Player pl) {
		players.add(pl);
		// TODO: allocate stuff for bullets
	}
	public void addBullet(Bullet b) {
		bullets.add(b);
	}
	public float createLocalBullets(Player localPlayer, float time, float lastShot, float energy) {
		return 0;
	}

	private void checkHit(Point2D.Float p, Point2D.Float start, Point2D.Float end) {
	}
};
