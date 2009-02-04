package lento.gamestate;

import java.util.*;
import java.awt.geom.*;
import java.io.*;

/*
 * GamePhysics pitää kirjaa pelialueesta ja kaikista pelin pelaajista ja ammuksista, ja huolehtii näiden päivityksistä.
 */
public class GamePhysics {

	static final float GRAVITY = 50.0f;
//	static final float 

	private AreaGeometry geometry;
	private ArrayList<Player> players = new ArrayList<Player>();
	private ArrayList<Bullet> bullets = new ArrayList<Bullet>();

	public GamePhysics(File file) throws IOException {
		geometry = new AreaGeometry(file);
	}
	public GamePhysics() {
		geometry = new AreaGeometry();
	}

	public void update(float time, Player localPlayer) {
		for(Iterator<Player> i=players.iterator(); i.hasNext(); ) {
			Player pl = i.next();
			if (pl.isAlive()) {
				pl.update(time);
				Collision coll = geometry.getCollision(pl.prevLocation, pl.location);
				while(coll != null) {
					Point2D.Float speed = pl.speedVec;
					Point2D.Float normal = coll.normal;
					Point2D.Float loc = coll.location;
					float dot = normal.x*speed.x + normal.y*speed.y;
			//		if (dot>=0) System.out.printf("wtf %f\n", dot);
					speed.x -= 2*dot*normal.x;
					speed.y -= 2*dot*normal.y;
					speed.x *= .9f;
					speed.y *= .9f;

					float dist = (float)pl.location.distance(loc);

					pl.location.x = loc.x + .5f*dist*speed.x*time;
					pl.location.y = loc.y + .5f*dist*speed.y*time;

					final float eps = 1e-6f;
					pl.prevLocation.x = loc.x+speed.x*eps;
					pl.prevLocation.y = loc.y+speed.y*eps;

					coll = geometry.getCollision(pl.prevLocation, pl.location);
				}
			}
		}
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
	public float createLocalBullets(Player localPlayer, float time, float lastShot) {
		return 0;
	}

	private void checkHit(Point2D.Float p, Point2D.Float start, Point2D.Float end) {
	}

	public AreaGeometry getGeometry() {
		return geometry;
	}
	public ArrayList<Player> getPlayers() {
		return players;
	}
	public ArrayList<Bullet> getBullets() {
		return bullets;
	}
	public void deletePlayer(Player pl) {
		players.remove(pl);
	}
};
