package lento.gameui;

import lento.net.*;
import lento.gamestate.*;
import java.awt.*;

/** Apuluokka, jonka avulla voi luoda LocalPlayer-luokan instansseja
 * lento.gameui-paketin ulkopuolella.
 */
public class PlayerCreator {

	/** Luo uuden Player-olion.
	 *
	 * @param name pelaajan nimi
	 * @param color pelaajan aluksen väri
	 * @return uusi Player-olio
	 */
	public static Player makeLocalPlayer(String name, Color color, int id) {
		LocalPlayer pl = new LocalPlayer(null, name, color);
		pl.setID(id);
		return pl;
	}

	/** Asettaa LocalPlayer-oliolle pelaaja-ID:n.
	 *
	 * @param pl pelaaja, jolle ID asetetaan
	 * @param id asetettava ID
	 */
	public static void setLocalPlayerID(Player pl, int id) {
		LocalPlayer loc = (LocalPlayer)pl;
		loc.setID(id);
	}
}
