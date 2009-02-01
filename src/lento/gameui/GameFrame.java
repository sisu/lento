package lento.gameui;

import lento.gamestate.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

class GameFrame extends JFrame {

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
		BufferStrategy bs = getBufferStrategy();
		Graphics g=null;
		try {
			g = bs.getDrawGraphics();
			paint(g);
		} finally {
			g.dispose();
		}
		bs.show();
	}
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.black);
		int w=getWidth(), h=getHeight();
		g2.fillRect(0,0,w,h);

		AreaGeometry geometry = physics.getGeometry();
		ArrayList<ColoredPolygon> polygons = geometry.getPolygons();
		for(Iterator<ColoredPolygon> i=polygons.iterator(); i.hasNext(); ) {
			ColoredPolygon cp = i.next();
			g2.setColor(cp.color);
			g2.fill(cp);
		}
	}
};
