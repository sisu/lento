package lento.net;

import java.net.*;
import java.util.*;
import java.io.*;

/** Kirjoituspuskurin suoran käytön salliva wrapper-luokka ByteArrayOutputStream-luokalle.
 * <p>
 * Luokka ei varsinaisesti laajenna ByteArrayOutputStream-luokan toiminnallisuutta,
 * mutta se mahdollistaa kirjoituspuskurin epäturvallisen suoran lukemisen
 * tehokkuussyistä.
 */
class PacketOutputStream extends ByteArrayOutputStream {

	/** Luo uusi olio ja varaa sille kirjoituspuskuri.
	 * @param size kirjoituspuskurin haluttu koko
	 */
	PacketOutputStream(int size) {
		super(size);
	}

	/** Palauttaa viitteen kirjoituspuskurin dataan tekemättä kopiota siitä.
	 */
	byte[] getData() {
		return buf;
	}
};
