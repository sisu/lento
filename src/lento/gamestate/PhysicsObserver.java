package lento.gamestate;

/**
 * Rajapinta paikallisen pelaajan tapahtumien seuraamiseen.
 */
public interface PhysicsObserver {
	/** Kutsutaan, kun pelaajaan osuu ammus.
	 * @param player ammuksen ampujan pelaaja-ID
	 * @param id ammuksen ID
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
