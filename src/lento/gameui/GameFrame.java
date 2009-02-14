package lento.gameui;

import lento.gamestate.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

class GameFrame extends JFrame {

	private static final Polygon craftPolygon = new Polygon(new int[]{-10,0,10,0}, new int[]{-10,5,-10,10}, 4);
	private static final int BORDER_SIZE = 25;
	private static final int BAR_W_GAP = 20;
	private static final int BAR_HEIGHT = 10;
	private static final int BAR_H_GAP = 3;

	private GamePhysics physics;
	private LocalPlayer localPlayer;

	boolean scoreViewMode = false;
	int frameCount=0;

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
	public void repaint() {
//		System.out.println("jee");
		BufferStrategy bs = getBufferStrategy();
		Graphics g=null;
		try {
			g = bs.getDrawGraphics();
			paint(g);
		} finally {
			g.dispose();
		}
		bs.show();
		Toolkit.getDefaultToolkit().sync();
	}

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;

		if (scoreViewMode) {
			drawScoreTable(g2);
			return;
		}

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
		for(Bullet b : physics.getBullets()) {
			Point2D.Float bloc = b.getLoc();

			Rectangle r = new Rectangle((int)(bloc.x+midX), (int)(bloc.y+midY), 1, 1);
			g2.fill(r);
		}

		// Piirrä palkit
		g2.setColor(Color.red);
		int maxBarSize = w-2*BAR_W_GAP;;
		int lifeBarSize = (int)(maxBarSize * (float)localPlayer.getHealth()/Player.INITIAL_HEALTH);
		g2.fill(new Rectangle(BAR_W_GAP, h-BAR_HEIGHT-BAR_H_GAP, lifeBarSize, BAR_HEIGHT));

		g2.setColor(Color.yellow);
		int energyBarSize = (int)(maxBarSize * localPlayer.shootEnergy/LocalPlayer.MAX_SHOOT_ENERGY);
		g2.fill(new Rectangle(BAR_W_GAP, h-2*(BAR_HEIGHT+BAR_H_GAP), energyBarSize, BAR_HEIGHT));

		++frameCount;
	}
	private static final String[] titles = new String[]{"nimi","tapot","kuolemat","osumat","osuttu"};
	private static final int NAME_LEFT_SPACE = 20;
	private static final int NAME_FIELD_SIZE = 150;
	private static final int FIELD_W_SIZE = 100;
	private static final int FIELD_H_SIZE = 50;
	private static final int SCORE_UP_SPACE = 100;

	void drawScoreTable(Graphics2D g2) {
		g2.setColor(Color.black);
		int w=getWidth(), h=getHeight();
		g2.fillRect(0,0,w,h);

		g2.setColor(Color.white);
		g2.drawString(titles[0], NAME_LEFT_SPACE, SCORE_UP_SPACE);
		for(int i=1; i<5; ++i)
			g2.drawString(titles[i], NAME_LEFT_SPACE+NAME_FIELD_SIZE+(i-1)*FIELD_W_SIZE, SCORE_UP_SPACE);

		ArrayList<Player> players = new ArrayList<Player>(physics.getPlayers());
		Collections.sort(players, new Comparator() {
			public int compare(Object a, Object b) {
				int[] s1 = ((Player)a).getStats();
				int[] s2 = ((Player)b).getStats();
				return (int)((long)s1[1]*s2[0] - (long)s2[1]*s1[0]);
			}
		});

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
