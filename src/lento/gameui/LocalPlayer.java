package lento.gameui;

import lento.gamestate.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * LocalPlayer-luokka sisältää paikallisella koneella pelaavan pelaajan tiedot
 * ja huolehtii syötteen lukemisesta näppäimistöltä.
 * <p>
 * Syötteenluvusta huolehtimisen lisäksi LocalPlayer-olio joutuu pitämään
 * kirjaa niistä pelaajatiedoista, joista ei verkkopelaajien kohdalla
 * piitata.<br>
 * Näitä ovat aika, jolloin pelaaja saa syntyä uudelleen ja ampumisenergia.
 */
class LocalPlayer extends Player implements KeyListener {

	/** Aika, joka on kuluttava pelaajan kuoleman ja uudelleensyntymän
	 * välillä nanosekunteina. */
	static final long SPAWN_TIME = (long)1e9;
	/** Pelaajan ampumisenergian maksimimäärä */
	static final float MAX_SHOOT_ENERGY = 100;
	/** Kuinka nopeasti ampumisenergiat palautuvat (yksikköä/s) */
	static final float SHOOT_ENERGY_RECOVER_RATE = 20;
	/** Paljonko yhden ammuksen ampuminen kuluttaa ampumisenergiaa */
	static final float SHOOT_ENERGY_USE = 5;

	/** Pelin päivityksestä huolehtiva pääsilmukka */
	private GameLoop gameLoop;

	/** Tosi, joss pelaaja painaa ampumisnappia. */
	boolean shooting=false;
	/** Ajanhetki, jolloin pelaaja saa herätä seuraavan kerran henkiin. */
	long spawnTime = 0;
	/** Ajanhetki, jolloin pelaaja saa ampua seuraavan kerran. */
	long nextShootTime=0;
	/** ID-numero, joka annetaan seuraavalle ammuttavalle ammukselle. */
	int nextBulletID=0;
	/** Pelaajan jäljellä oleva ampumisenergia. */
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
