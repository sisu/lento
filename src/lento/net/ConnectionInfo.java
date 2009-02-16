package lento.net;

import java.net.*;

/**
 * ConnectionInfo-luokka pitää kirjaa yhden UDP-yhteyden osoitteesta ja
 * portista.
 * Luokka toteuttaa hashCode- ja equals-metodit, joten sitä voidaan käyttää
 * hajautustaulussa hakemaan tehokkaasti pelaajatieto paketin verkko-osoitteen
 * perusteella.
 */
class ConnectionInfo {

	/** Yhteyden etäkoneen verkko-osoite */
	InetAddress address;
	/** Etäkoneen käyttämä portti */
	int port;

	/** Muodosta olio osoitteen ja portin perusteella.
	 * @param address etäkoneen verkko-osoite
	 * @param port etäkoneen käyttämä portti
	 */
	ConnectionInfo(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	/** Muodosta olio pelaajan tietojen perusteella.
	 * @param player etäpelaaja, jonka yhteyden tiedoja käytetään
	 */
	ConnectionInfo(NetPlayer player) {
		address = player.socket.getInetAddress();
		port = player.udpPort;
	}

	/** Palauttaa tämän olion hajautuskoodin.
	 * @return osoitteeseen ja porttiin perustuva hajautuskoodi
	 */
	public int hashCode() {
		return address.hashCode()^port;
	}
	/** Testaa, viittaako parametrina annettu ConnectionInfo-olio samaan
	 * UDP-yhteyteen kuin tämä.
	 * @param obj ConnectionInfo-olio, jota verrataan tähän
	 * @return true, jos verkko-osoitteet ja portit ovat samat
	 * @return false muuten
	 */
	public boolean equals(Object obj) {
		ConnectionInfo info = (ConnectionInfo)obj;
		return port==info.port && address.equals(info.address);
	}
};

