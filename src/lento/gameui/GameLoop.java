package lento.gameui;

import lento.gamestate.*;
import java.io.*;

public class GameLoop {

	private GamePhysics physics;
	private LocalPlayer localPlayer;
	boolean done=false;

	public GameLoop(File file) throws IOException {
		localPlayer = new LocalPlayer(this);
		physics = new GamePhysics(file);
		physics.addPlayer(localPlayer);
	}

	public void start() {
		System.out.println("creating game frame");
		GameFrame frame = new GameFrame(physics, localPlayer);
		frame.addKeyListener(localPlayer);

		System.out.println("starting game loop");

		long prevTime = System.nanoTime();
		try{
		while(!done) {
//			System.out.println("loop");
			long time = System.nanoTime();
			float diff = (time-prevTime)/1e9f;
			prevTime = time;

			physics.update(diff, localPlayer);
			if (localPlayer.shooting)
				physics.createLocalBullets(localPlayer, diff, 0);

			frame.repaint();

			Thread.sleep(10);
		}
		}catch(InterruptedException e){}

		frame.setVisible(false);
	}
}
