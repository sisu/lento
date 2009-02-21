package lento.gamestate;

import java.util.*;
import java.awt.geom.*;
import java.io.*;

/**
 * GamePhysics pitää kirjaa pelialueesta ja kaikista pelin pelaajista ja ammuksista,
 * ja huolehtii näiden päivityksistä.
 * <p>
 * Peliä varten luodaan aina tasan yksi GamePhysics-olio, jonka kautta suurin osa
 * pelin fysiikan hallinnasta tehdään.
 */
public class GamePhysics {

	/** Gravitaatiovoiman aiheuttama kiihtyvyys (pix/s^2) */
	static final float GRAVITY = 50.0f;
	/** Moninkokertaiseksi aluksen nopeus muuttuu seinään törmätessä */
	static final float SLOWDOWN_FACTOR = .6f;
	/** Etäisyys, jonka saavutettuaan ammus osuu pelaajaan. */
	static final float BULLET_HIT_RANGE = 8.f;
	/** Ammuksen lähtönopeus */
	static final float BULLET_SPEED = 400.f;
	/** Aika, joka on odotettava kahden ammuksen ampumisen välillä nanosekunteina. */
	public static final long SHOOT_INTERVAL = (long)1e8;
	/** Yhden ammuksen osumisesta aiheutuva vahinkomäärä pelaajalle. */
	public static final int BULLET_DAMAGE = 100;
	/** Paljonko pelaaja vahingoittuu seinään törmätessään.
	 * Todellinen vahinkomäärä saadaan kertomalla tämä seinän normaalin ja
	 * aluksen nopeusvektorin pistetulon vastaluvulla. */
	public static final float COLLISION_DAMAGE_FACTOR = 0.1f;

	/** Pelialueen geometria */
	private AreaGeometry geometry;
	/** Taulukko kaikista peliin liittyneistä pelaajista. */
	private ArrayList<Player> players = new ArrayList<Player>();
	/** Taulukko kaikista ilmassa olevista ammuksista. */
	private ArrayList<Bullet> bullets = new ArrayList<Bullet>();

	/** Taulukko pelaajien indekseistä players-taulukossa.
	 * Jos pelaajan ID on i, ja se sijaitsee players-taulukossa
	 * kohdassa a, niin playerIndex[i] = a. Tämän taulukon avulla
	 * löydetään siis pelaaja ID-numeron perusteella vakioajassa.
	 */
	private int[] playerIndex = new int[256];

	// FIXME: älä varaa koko taulukkoa? (vie 64Mt muistia)
	/** Taulukko siitä, missä indeksissä mikäkin kunkin pelaajan ampuma 
	 * ammus sijaitsee bullets-taulukossa.
	 * Jos ammuksen x ampujan ID on a ja ammus-ID b, niin pätee:
	 * bulletIndex[a][b] = x.
	 */
	private int[][] bulletIndex = new int[256][65536];

	/** Pelifysiikan tapahtumia tarkkailemaan asetettu olio. */
	PhysicsObserver observer;

	/** Luo uuden GamePhysics-olion lataamalla pelialueen tiedot tiedostosta.
	 * @param file tiedosto, josta kentän tiedot ladataan
	 */
	public GamePhysics(File file) throws IOException {
		geometry = new AreaGeometry(file);
	}
	/** Luo GamePhysics-olion alustamatta pelialuetta.
	 * Mitään seiniin törmäyksiä ei tunnisteta ennen kuin
	 * geometry-oliolle on muuten kerrottu kentän esteistä.
	 */
	public GamePhysics() {
		geometry = new AreaGeometry();
	}

	/** Päivittää pelaajien ja ammusten tilan. Tätä metodia kutsutaan kerran joka framen aikana.
	 * @param time edellisestä framesta kulunut aika sekunteina
	 * @param localPlayer paikallinen pelaaja
	 */
	public synchronized void update(float time, Player localPlayer) {
		int localHealth = localPlayer.health;
		boolean alive = localPlayer.alive;
		
		// päivitä kaikkien pelaajien sijainnit
		for(Player pl : players) {
			if (!pl.isAlive())
				continue;
			pl.update(time);

			// toista niin kauan kuin pelaaja on törmäämässä seinään
			while(true) {
				Collision coll = geometry.getCollision(pl.prevLocation, pl.location);
				if (coll==null)
					break;
				handleReflection(pl, coll);
			}
		}
		localPlayer.damageTaken += localHealth-localPlayer.health;
		if (alive && localPlayer.health<=0) {
			// pelaaja kuoli seinääntörmäyksen seurauksena
			localPlayer.alive = false;
			localPlayer.deaths++;
			if (observer != null)
				observer.die(localPlayer.id, Player.INITIAL_HEALTH-localPlayer.health);
		}

		// Päivitä ammusten sijainnit
		for(int i=0; i<bullets.size(); ++i) {
			Bullet b = bullets.get(i);
			Point2D.Float prevLoc = b.update(time);
			if (geometry.getCollision(prevLoc, b.location)!=null) {
				// ammus törmäsi seinään
				deleteBullet(i);
				--i; // taulukon kohtaan i tullut uusi ammus; ei jätetä sitä välistä
				continue;
			}
			if (!localPlayer.isAlive())
				continue;

			// tarkista osuiko ammus paikalliseen pelaajaan
			Point2D.Float loc = localPlayer.location;
			float dist2 = pointLineDistSq(loc, prevLoc, b.location);
			if (dist2 < BULLET_HIT_RANGE*BULLET_HIT_RANGE) {
				deleteBullet(i);
				// älä jätä lopusta kohtaan i siirrettyä ammusta päivittämättä
				--i;

				if (observer!=null)
					observer.hit(b);

				Player shooter = getPlayer(b.getShooter());
				if (shooter!=null && shooter.id!=localPlayer.id)
					shooter.damageDone += BULLET_DAMAGE;


				localPlayer.health -= BULLET_DAMAGE;
				localPlayer.damageTaken += BULLET_DAMAGE;
				if (localPlayer.health <= 0) {
					localPlayer.alive = false;
					localPlayer.deaths++;
					if (shooter!=null && shooter.id!=localPlayer.id)
						shooter.kills++;
					if (observer!=null)
						observer.die(b.getShooter(), Player.INITIAL_HEALTH-localPlayer.health);
				}
			}
		}
	}

	/** Määrittää pelaajan uuden sijainnin ja nopeusvektorin törmäyksen jälkeen.
	 * Uudet tiedot tallennetaan suoraan parametrina annettuun Player-olioon.
	 * @param pl seinään törmännyt pelaaja
	 * @param coll tapahtunut törmäys
	 */
	private void handleReflection(Player pl, Collision coll) {
		Point2D.Float speed = pl.speedVec;
		Point2D.Float normal = coll.getNormal();
		Point2D.Float loc = coll.getLoc();
		float dot = normal.x*speed.x + normal.y*speed.y;
		if (dot>=0) {
//			System.out.println("phale");
			dot = -dot;
		}
		pl.health -= ((int)(-dot*COLLISION_DAMAGE_FACTOR));
//		System.out.printf("%f\n", dot);
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
//			System.out.println("wtf");
			diffDot = -diffDot;
		}
		if (diffDot >= -asd) diffDot -= asd;

		float factor = 2*diffDot*dist/dist2;
		pl.location.x -= factor*normal.x;
		pl.location.y -= factor*normal.y;

		final float eps = 1e-5f;
		pl.prevLocation.x = loc.x+speed.x*eps;
		pl.prevLocation.y = loc.y+speed.y*eps;
	}

	/** Laskee etäisyyden neliön pisteen ja janan välillä.
	 * Tätä funktiota käytetään apuna määrittäessä, osuuko ammus pelaajaan.
	 * @param p testattava piste
	 * @param start janan alkupiste
	 * @param end janan loppupiste
	 * @return pisteen p etäisyyden neliö jananasta (start,end)
	 */
	private static float pointLineDistSq(Point2D.Float p, Point2D.Float start, Point2D.Float end) {
		float dx = end.x-start.x, dy = end.y-start.y;
		float u = ((p.x-start.x)*dx + (p.y-start.y)*dy) / (dx*dx+dy*dy);

		if (u>=0 && u<=1)
			return (float)p.distanceSq(start.x+u*dx, start.y+u*dy);
		return (float)Math.min(p.distanceSq(start), p.distanceSq(end));
	}

	/** Poistaa ammuksen bullets-taulukon indeksin perusteella.
	 * @param i poistettavan ammuksen indeksi bullets-taulukossa
	 */
	private void deleteBullet(int i) {
		synchronized(bullets) {
			// Siirretään taulukon viimeinen alkio poistettavan tilalle
			int size = bullets.size();
			if (i>=size) return;
			Bullet last = bullets.get(size-1);
			bullets.set(i, last);
			bullets.remove(size-1);

			bulletIndex[last.getShooter()][last.getID()] = i;
		}
	}
	/** Lisää pelaajan pelaajien listaan.
	 * @param pl lisättävä pelaaja
	 */
	public synchronized void addPlayer(Player pl) {
		playerIndex[pl.getID()] = players.size();
		players.add(pl);
	}
	/** Lisää ammuksen ilmassa olevien ammusten listaan.
	 * @param b ammuttu ammus
	 */
	public synchronized void addBullet(Bullet b) {
		Player pl = players.get(playerIndex[b.getShooter()]);
		bulletIndex[pl.id][b.getID()] = bullets.size();
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
	public synchronized int createLocalBullets(Player player, long prevTime, long curTime, long nextShootTime, int nextID, int maxCount) {
		long tDiff = curTime-prevTime;
		int count=0;
		while(count < maxCount && nextShootTime < curTime) {

			float locX = player.location.x;
			float locY = player.location.y;

			float sa = (float)Math.sin(player.angle);
			float ca = (float)Math.cos(player.angle);
			locX += ca*(BULLET_HIT_RANGE*2);
			locY -= sa*(BULLET_HIT_RANGE*2);

			float vx = ca*BULLET_SPEED + player.speedVec.x;
			float vy = -sa*BULLET_SPEED + player.speedVec.y;

//			System.out.printf("muu %f %f ; %f %f (%f %f) (%f ; %f)\n", locX,locY,vx,vy, player.location.x, player.location.y, td, (1-td)*tDiff/1e9f);
			Bullet b = new Bullet(locX,locY,vx,vy,player.id,nextID);
			nextID = (nextID+1) & 0xffff; // ammus-ID:t ovat väliltä 0-2^16
			++count;

			// lasketaan muuttujaan td missä kohtaa framen aikana ammus ammuttiin
			// td=0 -> framen alussa; td=1 -> framen lopussa
			float td = (float)(nextShootTime-prevTime)/tDiff;
			b.update((1-td)*tDiff/1e9f);
			addBullet(b);

			if (observer!=null) observer.shoot(b);

			nextShootTime += SHOOT_INTERVAL;
		}
		return count;
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
	public synchronized void deletePlayer(Player pl) {
		int i = playerIndex[pl.id];
		int size = players.size();

		if (i<0 || i>=size || players.get(i).id != pl.id) {
			// pelaaja jo poistettu
			return;
		}

		// Siirrä taulukon viimeinen poistuvan paikalle
		Player last = players.get(size-1);
		players.set(i, last);
		players.remove(size-1);

		playerIndex[last.getID()] = i;
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
		Player pl = players.get(num);
		if (pl.id != id)
			return null;
		return pl;
	}

	/** Asettaa fysiikkatarkkailijan. Oliolle välitetään tiedot ainoastaan paikallisen
	 * pelaajan tilan muutoksista. Funktion kutsuminen poistaa käytöstä aiemmin
	 * asetetun tarkkailijan.
	 * @param observer uusi fysiikkatarkkailija
	 */
	public void setObserver(PhysicsObserver observer) {
		this.observer = observer;
	}
	/** Poistaa ammuksen pelistä ja päivittää ampujan osumatilastot.
	 * @param shooter ammuksen ampujan pelaaja-ID
	 * @param id ammuksen ID
	 * @param selfHit tosi, joss ampuja osui itseensä
	 */
	public synchronized void deleteBullet(int shooter, int id, boolean selfHit) {
		if (!selfHit) {
			Player pl = getPlayer(id);
			if (pl!=null)
				pl.addHitDone();
		}
		int idx = bulletIndex[shooter][id];
		deleteBullet(idx);
	}
};
