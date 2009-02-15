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

	/** Luo olion paikalliselle pelaajalle.
	 * @param loop pelin päivityksestä huolehtiva GameLoop-olio
	 * @param name pelaajan nimi
	 * @param color pelaajan aluksen väri
	 */
	LocalPlayer(GameLoop loop, String name, Color color) {
		gameLoop = loop;
		this.name = name;
		this.color = color;

		id = 1;
	}

	// KeyListener-rajapinta

	/** Huolehtii näppäimen painalluksesta.
	 * @param e näppäimistötapahtuma
	 */
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		if (key==KeyEvent.VK_ESCAPE)
			gameLoop.done = true;
		else
			handleMove(key, true);
	}
	/** Huolehtii näppäimen irrotuksesta.
	 * @param e näppäimistötapahtuma
	 */
	public void keyReleased(KeyEvent e) {
		handleMove(e.getKeyCode(), false);
	}
	/** Kutsut tälle funktiolle jätetään huomiotta.
	 */
	public void keyTyped(KeyEvent e) {
	}
	// KeyListener loppu

	/** Päättää mitä tehdään, kun näppäintä aletaan painaa tai lakataan painamasta.
	 * @param key näppäin, jonka tila muuttui
	 * @param change true, jos näppäintä painettiin, false jos se päästettiin ylös
	 */
	private void handleMove(int key, boolean change) {
		int ic = change ? 1 : -1;
		switch(key) {
			case KeyEvent.VK_UP:
				accelerating=change;
				break;
			case KeyEvent.VK_LEFT:
				turning += ic;
				turning = Math.max(turning, -1);
				turning = Math.min(turning, 1);
				break;
			case KeyEvent.VK_RIGHT:
				turning -= ic;
				turning = Math.max(turning, -1);
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

	/** Herättää pelaajan henkiin.
	 * @param loc sijainti, johon pelaaja syntyy
	 */
	public void spawn(Point2D.Float loc) {
		super.spawn(loc);
		shootEnergy = MAX_SHOOT_ENERGY;
		nextShootTime = System.nanoTime();
	}

	/** Palauttaa ampumisenergiaa sen verran, kuin sitä on framen aikana ehtinyt tulla.
	 * @param time edellisestä palautuskerrasta kulunut aika sekunteina
	 */
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
