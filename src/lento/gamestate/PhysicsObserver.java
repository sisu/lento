package lento.gamestate;

/**
 *
 */
public interface PhysicsObserver {
	/*
	 * 
	 */
	public void hit(int player, int id);
	/*
	 * 
	 */
	public void shoot(Bullet bullet, int id);
	/*
	 *
	 */
	public void die(int killer);
}
