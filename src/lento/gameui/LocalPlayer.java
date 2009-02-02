package lento.gameui;

import lento.gamestate.*;
import java.awt.event.*;
import java.awt.*;

/**
 * LocalPlayer-luokka sisältää paikallisella koneella pelaavan pelaajan tiedot ja huolehtii syötteen lukemisesta näppäimistöltä.
 */
class LocalPlayer extends Player implements KeyListener {

	static final float SPAWN_TIME = 1.0f;

	private GameLoop gameLoop;

	boolean shooting=false;
	long spawnTime = 0;

	LocalPlayer(GameLoop loop, String name, Color color) {
		gameLoop = loop;
		this.name = name;
		this.color = color;
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
		//FIXME: estä hajoaminen
		int ic = change ? 1 : -1;
		switch(key) {
			case KeyEvent.VK_UP:
				accelerating=change;
				break;
			case KeyEvent.VK_LEFT:
				turning += ic;
				break;
			case KeyEvent.VK_RIGHT:
				turning -= ic;
				break;
			default:
				break;
		}
	}
}
