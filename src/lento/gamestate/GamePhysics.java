package lento.gamestate;

import java.util.*;
import java.awt.geom.*;
import java.io.*;

/**
 * GamePhysics pitää kirjaa pelialueesta ja kaikista pelin pelaajista ja ammuksista, ja huolehtii näiden päivityksistä.
 */
public class GamePhysics {

	static final float GRAVITY = 50.0f;
	static final float SLOWDOWN_FACTOR = .8f;
	static final float BULLET_HIT_RANGE = 8.f;
	static final float BULLET_HIT_RANGE_SQ = BULLET_HIT_RANGE*BULLET_HIT_RANGE;
	static final float BULLET_SPEED = 400.f;
	public static final long SHOOT_INTERVAL = (long)1e8;
	public static final int BULLET_DAMAGE = 100;
	public static final float COLLISION_DAMAGE_FACTOR = 0.1f;

	private AreaGeometry geometry;
	private ArrayList<Player> players = new ArrayList<Player>();
	private ArrayList<Bullet> bullets = new ArrayList<Bullet>();

	private int[] playerIndex = new int[256];

	PhysicsObserver observer;

	public GamePhysics(File file) throws IOException {
		geometry = new AreaGeometry(file);
	}
	public GamePhysics() {
		geometry = new AreaGeometry();
	}

	/** Päivittää pelaajien ja ammusten tilan. Tätä metodia kutsutaan kerran joka framen aikana.
	 * @param time edellisestä framesta kulunut aika sekunteina
	 * @param localPlayer paikallinen pelaaja
	 */
	public void update(float time, Player localPlayer) {
		int localHealth = localPlayer.health;
		boolean alive = localPlayer.alive;
		for(Player pl : players) {
			if (pl.isAlive()) {
				pl.update(time);
				Collision coll = geometry.getCollision(pl.prevLocation, pl.location);
				while(coll != null) {
					handleReflection(pl, coll);
					coll = geometry.getCollision(pl.prevLocation, pl.location);
				}
			}
		}
		localPlayer.damageTaken += localHealth-localPlayer.health;
		if (alive && localPlayer.health<=0) {
			localPlayer.alive = false;
			localPlayer.deaths++;
			observer.die(localPlayer.id, Player.INITIAL_HEALTH-localPlayer.health);
		}
		for(int i=0; i<bullets.size(); ++i) {
			Bullet b = bullets.get(i);
			Point2D.Float prevLoc = b.update(time);
			if (geometry.getCollision(prevLoc, b.location)!=null) {
				deleteBullet(i);
				--i;
				continue;
			}
			if (!localPlayer.alive)
				continue;
			Point2D.Float loc = localPlayer.location;
			float dist2 = pointLineDistSq(loc, prevLoc, b.location);
			if (dist2 < BULLET_HIT_RANGE_SQ) {
//				System.out.printf("bullet hit: %f ; %f %f ; %f %f\n", dist2, b.location.x, b.location.y, p.location.x, p.location.y);
				deleteBullet(i);
				if (observer!=null)
					observer.hit(b);
				Player shooter = getPlayer(b.getShooter());
				if (shooter!=null && shooter.id!=localPlayer.id)
					shooter.damageDone += BULLET_DAMAGE;
				--i;
				localPlayer.health -= BULLET_DAMAGE;
				localPlayer.damageTaken += BULLET_DAMAGE;
				if (localPlayer.health <= 0) {
					localPlayer.alive = false;
					localPlayer.deaths++;
					if (shooter!=null)
						shooter.kills++;
					if (observer!=null)
						observer.die(b.getShooter(), Player.INITIAL_HEALTH-localPlayer.health);
				}
				break;
			}
		}
	}

	private void handleReflection(Player pl, Collision coll) {
		Point2D.Float speed = pl.speedVec;
		Point2D.Float normal = coll.normal;
		Point2D.Float loc = coll.location;
		float dot = normal.x*speed.x + normal.y*speed.y;
		if (dot>=0) {
			System.out.println("phale");
			dot = -dot;
		}
		pl.health -= ((int)(-dot*COLLISION_DAMAGE_FACTOR));
		System.out.printf("%f\n", dot);
		final float asd = 3;
		if (dot >= -asd) dot -= asd;
		speed.x -= 2*dot*normal.x;
		speed.y -= 2*dot*normal.y;
		speed.x *= SLOWDOWN_FACTOR;
		speed.y *= SLOWDOWN_FACTOR;

		float dist = (float)pl.location.distance(loc);
		float dist2 = (float)pl.location.distance(pl.prevLocation);

		float diffX = pl.location.x-pl.prevLocation.x;
		float diffY = pl.location.y-pl.prevLocation.y;
		float diffDot = diffX*normal.x + diffY*normal.y;

		if (diffDot>=0) {
			System.out.println("wtf");
			diffDot = -diffDot;
		}
		if (diffDot >= -asd) diffDot -= asd;

		float factor = 2*diffDot*dist/dist2;
		pl.location.x -= factor*normal.x;
		pl.location.y -= factor*normal.y;

		final float eps = 1e-3f;
		pl.prevLocation.x = loc.x+speed.x*eps;
		pl.prevLocation.y = loc.y+speed.y*eps;
	}

	private void deleteBullet(int i) {
		int size = bullets.size();
		if (i>=size) return;
		Bullet last = bullets.get(size-1);
		bullets.set(i, last);
		bullets.remove(size-1);

		Player pl = getPlayer(last.getShooter());
		if (pl!=null)
			pl.bulletIndex[last.getID()] = i;
	}
	public void deleteBullet(Bullet b) {
		Player pl = players.get(playerIndex[b.getShooter()]);
		deleteBullet(pl.bulletIndex[b.getID()]);
	}

	/** Lisää pelaajan pelaajien listaan.
	 * @param pl lisättävä pelaaja
	 */
	public void addPlayer(Player pl) {
		playerIndex[pl.getID()] = players.size();
		players.add(pl);
		// TODO: allocate stuff for bullets
	}
	/** Lisää ammuksen ilmassa olevien ammusten listaan.
	 * @param b ammuttu ammus
	 */
	public void addBullet(Bullet b) {
		Player pl = players.get(playerIndex[b.getShooter()]);
		pl.bulletIndex[b.getID()] = bullets.size();
		bullets.add(b);
	}

	/** Generoi ammuksia paikalliselle pelaajalle.
	 * Kaikki parametreina annetut ajat ja palautettu aika ovat nanosekunteja.
	 * Jos ammuksia ammutaan useampia, niiden ID-numeroiksi valitaan peräkkäisiä lukuja parametrista nextID alkaen.
	 * @param player ampuva pelaaja
	 * @param prevTime aika edellisen framen alussa nanosekunteina
	 * @param curTime aika tämän framen alussa nanosekunteina
	 * @param nextShootTime aika, jolloin kyseinen pelaaja saa seuraavan kerran ampua
	 * @param nextID seuraavan ammuttavan ammuksen ID
	 * @param maxCount maksimimäärä, paljonko ammuksia voidaan tuottaa tällä kutsukerralla
	 * @return ammuttujen ammusten määrä
	 */
	public int createLocalBullets(Player player, long prevTime, long curTime, long nextShootTime, int nextID, int maxCount) {
		long tDiff = curTime-prevTime;
		int count=0;
		while(count < maxCount && nextShootTime < curTime) {

			float td = (float)(nextShootTime-prevTime)/tDiff;
			float locX = td*player.location.x + (1-td)*player.prevLocation.x;
			float locY = td*player.location.y + (1-td)*player.prevLocation.y;
			float sa = (float)Math.sin(player.angle);
			float ca = (float)Math.cos(player.angle);
			locX += ca*(BULLET_HIT_RANGE+5);
			locY -= sa*(BULLET_HIT_RANGE+5);
			float vx = ca*BULLET_SPEED + player.speedVec.x;
			float vy = -sa*BULLET_SPEED + player.speedVec.y;

			assert(td>=0 && td<=1);

//			System.out.printf("muu %f %f ; %f %f (%f %f) (%f ; %f)\n", locX,locY,vx,vy, player.location.x, player.location.y, td, (1-td)*tDiff/1e9f);
			Bullet b = new Bullet(locX,locY,vx,vy,player.id,nextID+count);
			++count;
			b.update((1-td)*tDiff/1e9f);
			addBullet(b);

			if (observer!=null) observer.shoot(b);

			nextShootTime += SHOOT_INTERVAL;
		}
		return count;
	}

	/** Laskee etäisyyden neliön pisteen ja janan välillä.
	 * Tätä funktiota käytetään apuna määrittäessä, osuuko ammus pelaajaan.
	 * @param p testattava piste
	 * @param start janan alkupiste
	 * @param end janan loppupiste
	 * @return pisteen p etäisyyden neliö jananasta (start,end)
	 */
	private static float pointLineDistSq(Point2D.Float p, Point2D.Float start, Point2D.Float end) {
		float dx=end.x-start.x, dy=end.y-start.y;
		float u = ((p.x-start.x)*dx + (p.y-start.y)*dy) / (dx*dx+dy*dy);

		if (u>=0 && u<=1)
			return (float)p.distanceSq(start.x+u*dx, start.y+u*dy);
		return (float)Math.min(p.distanceSq(start), p.distanceSq(end));
	}

	/** Palauttaa pelialueen tiedot AreaGeometry-oliona.
	 * @return nykyistä pelialuetta vastaava AreaGeometry-olio.
	 */
	public AreaGeometry getGeometry() {
		return geometry;
	}
	/** Palauttaa pelissä olevien pelaajien listan.
	 * @return taulukko joka sisältää liittyneet pelaajat ja paikallisen pelaajan.
	 */
	public ArrayList<Player> getPlayers() {
		return players;
	}
	/** Palauttaa ilmassa olevat ammukset.
	 * @return taulukko kaikista ilmassa olevista ammuksista.
	 */
	public ArrayList<Bullet> getBullets() {
		return bullets;
	}
	/** Poistaa pelaajan pelaajien listalta.
	 * @param pl pelistä poistuva pelaaja
	 */
	public void deletePlayer(Player pl) {
		int i = playerIndex[pl.id];
		int size = players.size();

		Player last = players.get(size-1);
		players.set(i, last);
		players.remove(size-1);

		playerIndex[last.getID()] = i;
	}
	/** Asettaa fysiikkatarkkailijan. Oliolle välitetään tiedot ainoastaan paikallisen
	 * pelaajan tilan muutoksista. Funktion kutsuminen poistaa käytöstä aiemmin
	 * asetetun tarkkailijan.
	 * @param observer uusi fysiikkatarkkailija
	 */
	public void setObserver(PhysicsObserver observer) {
		this.observer = observer;
	}
	/** Palauttaa ammuksen ampujan pelaaja-ID:n ja ammus-ID:n perusteella.
	 * @param shooter ampujan pelaaja-ID
	 * @param id ammuksen ammus-ID
	 * @return ID-numeroita vastaava ammus
	 * @return null, jos mikään ammus ei vastaa annettuja tietoja
	 */
	public Bullet getBullet(int shooter, int id) {
		Player pl = players.get(playerIndex[shooter]);
		if (pl==null)
			return null;
		int idx = pl.bulletIndex[id];
		if (idx >= bullets.size())
			return null;
		return bullets.get(idx);
	}
	/** Palauttaa pelaaja-ID:tä vastaavan Pelaaja-olion.
	 * @param id
	 * @return ID:tä vastaava pelaaja
	 * @return null, jos mikään pelaaja ei vastaa ID:tä
	 */
	public Player getPlayer(int id) {
		int num = playerIndex[id];
		if (num >= players.size())
			return null;
		Player pl = players.get(playerIndex[id]);
		if (pl.id != id)
			return null;
		return pl;
	}
};
