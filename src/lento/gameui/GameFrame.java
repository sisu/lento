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

	private GamePhysics physics;
	private Player localPlayer;

	GameFrame(GamePhysics physics, Player localPlayer) {
		super("Lento");
		this.physics = physics;
		this.localPlayer = localPlayer;

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(800, 600);
		setVisible(true);
		createBufferStrategy(2);
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
		g2.setColor(Color.black);
		int w=getWidth(), h=getHeight();
		g2.fillRect(0,0,w,h);

		Point2D.Float loc = localPlayer.getLoc();
		float w2=w/2.f, h2=h/2.f;
		g2.translate(w2-loc.x, h2-loc.y);

		AreaGeometry geometry = physics.getGeometry();
		ArrayList<ColoredPolygon> polygons = geometry.getPolygons();
		for(Iterator<ColoredPolygon> i=polygons.iterator(); i.hasNext(); ) {
			ColoredPolygon cp = i.next();
			g2.setColor(cp.color);
			g2.fill(cp);
		}
		int areaW = geometry.getWidth(), areaH = geometry.getHeight();
		g2.setColor(geometry.getBorderColor());
		g2.fillRect(-BORDER_SIZE,-BORDER_SIZE, areaW+2*BORDER_SIZE, BORDER_SIZE);
		g2.fillRect(-BORDER_SIZE, 0, BORDER_SIZE, areaH);
		g2.fillRect(-BORDER_SIZE, areaH, areaW+2*BORDER_SIZE, BORDER_SIZE);
		g2.fillRect(areaW, 0, BORDER_SIZE, areaH);

		ArrayList<Player> players = physics.getPlayers();
		AffineTransform identity = new AffineTransform();
		for(Iterator<Player> i=players.iterator(); i.hasNext(); ) {
			Player pl = i.next();
			Point2D.Float ploc = pl.getLoc();
			g2.setTransform(identity);
			g2.translate(loc.x-ploc.x+w2, loc.y-ploc.y+h2);
			g2.rotate(-Math.PI/2-pl.getAngle());
			g2.setColor(pl.getColor());
			g2.fill(craftPolygon);
		}

		++frameCount;
	}
	int frameCount=0;
};
