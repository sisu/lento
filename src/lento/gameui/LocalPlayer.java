package lento.gameui;

import lento.gamestate.*;
import java.awt.event.*;

/**
 * LocalPlayer-luokka sisältää paikallisella koneella pelaavan pelaajan tiedot ja huolehtii syötteen lukemisesta näppäimistöltä.
 */
class LocalPlayer extends Player implements KeyListener {

	private GameLoop gameLoop;

	boolean shooting=false;

	LocalPlayer(GameLoop loop) {
		gameLoop = loop;
	}

	public void keyPressed(KeyEvent e) {
		System.out.println("pressed");
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
		switch(key) {
			case KeyEvent.VK_UP:
				accelerating=change;
				break;
			default:
				break;
		}
	}
}
