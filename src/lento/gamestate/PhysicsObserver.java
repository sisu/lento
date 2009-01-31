package lento.gamestate;

/**
 *
 */
interface PhysicsObserver {
	/*
	 * 
	 */
	void hit(int player, int id);
	/*
	 * 
	 */
	void shoot(Bullet bullet, int id);
	/*
	 *
	 */
	void die(int killer);
}
