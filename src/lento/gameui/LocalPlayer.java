package lento.gameui;

import lento.gamestate.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * LocalPlayer-luokka sisältää paikallisella koneella pelaavan pelaajan tiedot ja huolehtii syötteen lukemisesta näppäimistöltä.
 */
class LocalPlayer extends Player implements KeyListener {

	static final long SPAWN_TIME = (long)1e9;
	static final float MAX_SHOOT_ENERGY = 100;
	static final float SHOOT_ENERGY_RECOVER_RATE = 20;
	static final float SHOOT_ENERGY_USE = 5;

	private GameLoop gameLoop;

	boolean shooting=false;
	long spawnTime = 0;
	long nextShootTime=0;
	int nextBulletID=0;
	float shootEnergy;

	LocalPlayer(GameLoop loop, String name, Color color) {
		gameLoop = loop;
		this.name = name;
		this.color = color;

		id = 1;
	}

	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		if (key==KeyEvent.VK_ESCAPE)
			gameLoop.done = true;
		else
			handleMove(key, true);
	}
	public void keyReleased(KeyEvent e) {
		handleMove(e.getKeyCode(), false);
	}
	public void keyTyped(KeyEvent e) {
	}

	void handleMove(int key, boolean change) {
		int ic = change ? 1 : -1;
		switch(key) {
			case KeyEvent.VK_UP:
				accelerating=change;
				break;
			case KeyEvent.VK_LEFT:
				turning += ic;
				turning = Math.max(turning, -1);
				break;
			case KeyEvent.VK_RIGHT:
				turning -= ic;
				turning = Math.min(turning, 1);
				break;
			case KeyEvent.VK_SPACE:
				nextShootTime = Math.max(nextShootTime,System.nanoTime());
				shooting = change;
				break;
			case KeyEvent.VK_TAB:
				gameLoop.frame.scoreViewMode = change;
				break;
			default:
				break;
		}
	}
	public void spawn(Point2D.Float loc) {
		super.spawn(loc);
		shootEnergy = MAX_SHOOT_ENERGY;
		nextShootTime = System.nanoTime();
	}
	void recoverShootEnergy(float time) {
		shootEnergy = Math.min(shootEnergy+time*SHOOT_ENERGY_RECOVER_RATE, MAX_SHOOT_ENERGY);
	}
	/** Asettaa uuden ID-numeron pelaajalle.
	 * @param id uusi pelaaja-ID
	 */
	void setID(int id) {
		this.id=id;
	}

}
