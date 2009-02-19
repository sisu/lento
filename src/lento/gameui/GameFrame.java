package lento.gameui;

import lento.gamestate.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

/**
 * GameFrame avaa ikkunan ja päivittää peliruutua pelin ollessa käynnissä.
 * Luokka huolehtii sekä tavallisen peliruudun että pistenäytön piirtämisestä.
 * <p>
 * Avatun ikkunan kokoa voi muuttaa vapaasti, mutta pistenäyttö ei välttämättä
 * näy kokonaan kovin pienellä näytöllä.
 * <p>
 * GameFrame käyttää kaksoispuskurointia piirtoon.
 */
class GameFrame extends JFrame {

	/** Aluksen piirtämiseen käytetty polygoni. */
	private static final Polygon craftPolygon = new Polygon(new int[]{-10,0,10,0}, new int[]{-10,5,-10,10}, 4);

	/** Alueen reunuksen koko pikseleinä */
	private static final int BORDER_SIZE = 25;
	/** Energiapalkkien etäisyys ruudun reunoista vaakasuunnassa */
	private static final int BAR_W_GAP = 20;
	/** Energiapalkkien väliin jäävä tila pystysuunnassa */
	private static final int BAR_H_GAP = 3;
	/** Energiapalkkien korkeus */
	private static final int BAR_HEIGHT = 10;

	/** Pelin fysiikasta huolehtiva olio */
	private GamePhysics physics;
	/** Paikallista pelaajaa vastaava olio */
	private LocalPlayer localPlayer;

	/** Tosi, joss ollaan pistenäytössä */
	boolean scoreViewMode = false;

	/** Montako framea ollaan piirretty tämän sekunnin aikana. */
	int frameCount=0;

	/** Luo GameFrame-olion ja avaa uuden ikkunan piirtoa varten.
	 * @param physics Pelin fysiikasta huolehtiva olio
	 * @param localPlayer paikallinen pelaaja
	 */
	GameFrame(GamePhysics physics, LocalPlayer localPlayer) {
		super("Lento");
		this.physics = physics;
		this.localPlayer = localPlayer;

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(800, 600);
		setVisible(true);
		createBufferStrategy(2);

		// tab-näppäimen luku ei onnistu ilman tätä
		setFocusTraversalKeysEnabled(false);
	}

	/** Piirtää yhden framen.
	 * Valitsee, piirretäänkö normaali pelinäkymä vai pistenäyttö ja
	 * huolehtii kaksoispuskuroinnista.
	 */
	public void repaint() {
//		System.out.println("jee");
		BufferStrategy bs = getBufferStrategy();
		Graphics g=null;
		try {
			g = bs.getDrawGraphics();
			if (scoreViewMode)
				drawScoreTable(g);
			else
				paint(g);
			++frameCount;
		} finally {
			g.dispose();
		}
		bs.show();
		Toolkit.getDefaultToolkit().sync();
	}

	/** Piirtää normaalin peliruudun.
	 * Kuvakulma valitaan paikallisen pelaajan sijainnin perusteella.
	 * @param g grafiikkaolio, jonka kautta piirto tehdään
	 */
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;

		g2.setColor(Color.black);
		int w=getWidth(), h=getHeight();
		g2.fillRect(0,0,w,h);

		Point2D.Float loc = localPlayer.getLoc();
		float w2=w/2.f, h2=h/2.f;

		float midX = w2-loc.x, midY = h2-loc.y;

		AreaGeometry geometry = physics.getGeometry();
		int areaW = geometry.getWidth(), areaH = geometry.getHeight();

		if (w >= areaW)
			midX = w2-areaW/2;
		else
			midX = Math.max(Math.min(midX, BORDER_SIZE), w-areaW-BORDER_SIZE);
		if (h >= areaH)
			midY = h2-areaH/2;
		else 
			midY = Math.max(Math.min(midY, 2*BORDER_SIZE), h-areaH-BORDER_SIZE);

		g2.translate(midX, midY);

		// Piirrä kentän esteet
		for(ColoredPolygon cp : geometry.getPolygons()) {
			g2.setColor(cp.color);
			g2.fill(cp);
		}

		// Piirrä kentän reunat
		g2.setColor(geometry.getBorderColor());
		g2.fillRect(-BORDER_SIZE,-BORDER_SIZE, areaW+2*BORDER_SIZE, BORDER_SIZE);
		g2.fillRect(-BORDER_SIZE, 0, BORDER_SIZE, areaH);
		g2.fillRect(-BORDER_SIZE, areaH, areaW+2*BORDER_SIZE, BORDER_SIZE);
		g2.fillRect(areaW, 0, BORDER_SIZE, areaH);

		// Piirrä pelaajat
		AffineTransform identity = new AffineTransform();
		for(Player pl : physics.getPlayers()) {
			if (!pl.isAlive())
				continue;
			Point2D.Float ploc = pl.getLoc();
			g2.setTransform(identity);
			g2.translate(ploc.x+midX, ploc.y+midY);
			g2.rotate(-Math.PI/2-pl.getAngle());
			g2.setColor(pl.getColor());
			g2.fill(craftPolygon);
		}

		// Piirrä ammukset
		g2.setTransform(identity);
		g2.setColor(Color.white);
		ArrayList<Bullet> bullets = physics.getBullets();
		synchronized(bullets) {
			for(Bullet b : bullets) {
				Point2D.Float bloc = b.getLoc();

				Rectangle r = new Rectangle((int)(bloc.x+midX), (int)(bloc.y+midY), 1, 1);
				g2.fill(r);
			}
		}

		// Piirrä palkit
		g2.setColor(Color.red);
		int maxBarSize = w-2*BAR_W_GAP;;
		int lifeBarSize = (int)(maxBarSize * (float)localPlayer.getHealth()/Player.INITIAL_HEALTH);
		g2.fill(new Rectangle(BAR_W_GAP, h-BAR_HEIGHT-BAR_H_GAP, lifeBarSize, BAR_HEIGHT));

		g2.setColor(Color.yellow);
		int energyBarSize = (int)(maxBarSize * localPlayer.shootEnergy/LocalPlayer.MAX_SHOOT_ENERGY);
		g2.fill(new Rectangle(BAR_W_GAP, h-2*(BAR_HEIGHT+BAR_H_GAP), energyBarSize, BAR_HEIGHT));
	}

	// Pistenäytön piirtoon liittyvät vakiot

	/** Pistenäytön arvojen nimet */
	private static final String[] titles = new String[]{"nimi","tapot","kuolemat","osumat","osuttu"};
	/** Nimen vasemmalle puolelle jätettävä tila pistenäytössä */
	private static final int NAME_LEFT_SPACE = 20;
	/** Nimikentän koko pikseleinä pistenäytössä */
	private static final int NAME_FIELD_SIZE = 150;
	/** Muiden kenttien koko pikseleinä pistenäytössä */
	private static final int FIELD_W_SIZE = 100;
	/** Rivin korkeus pistenäytössä */
	private static final int FIELD_H_SIZE = 50;
	/** Paljonko tilaa jätetään kaikkien rivien yläpuolelle pistenäytössä */
	private static final int SCORE_UP_SPACE = 100;

	/** Piirtää pistetilaston.
	 * @param g grafiikkaolio, jonka kautta piirto tehdään
	 */
	void drawScoreTable(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;

		g2.setColor(Color.black);
		int w=getWidth(), h=getHeight();
		g2.fillRect(0,0,w,h);

		g2.setColor(Color.white);
		g2.drawString(titles[0], NAME_LEFT_SPACE, SCORE_UP_SPACE);
		for(int i=1; i<5; ++i)
			g2.drawString(titles[i], NAME_LEFT_SPACE+NAME_FIELD_SIZE+(i-1)*FIELD_W_SIZE, SCORE_UP_SPACE);

		ArrayList<Player> players = new ArrayList<Player>(physics.getPlayers());
		Collections.sort(players);

		int x=1;
		for(Player pl : players) {
			g2.setColor(pl.getColor());
			g2.drawString(pl.getName(), NAME_LEFT_SPACE, SCORE_UP_SPACE+x*FIELD_H_SIZE);
			int[] stats = pl.getStats();
			for(int i=0; i<4; ++i)
				g2.drawString(""+stats[i], NAME_LEFT_SPACE+NAME_FIELD_SIZE+i*FIELD_W_SIZE, SCORE_UP_SPACE+x*FIELD_H_SIZE);
			++x;
		}
	}
};
