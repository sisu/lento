package lento.gamestate;

/**
 * Rajapinta paikallisen pelaajan tapahtumien seuraamiseen.
 */
public interface PhysicsObserver {
	/** Kutsutaan, kun pelaajaan osuu ammus.
	 * @param bullet pelaajaan osunut ammus
	 */
	public void hit(Bullet bullet);
	/** Kutsutaan, kun paikallinen pelaaja ampuu.
	 * @param bullet ammuttu ammus
	 */
	public void shoot(Bullet bullet);
	/** Kutsutaan, kun paikallinen pelaaja kuolee.
	 * @param killer paikallisen pelaajan tappaneen pelaajan pelaaja-ID
	 */
	public void die(int killer, int damage);
}
