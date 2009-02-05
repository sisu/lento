package lento.gameui;

import lento.gamestate.*;
import lento.net.*;
import java.io.*;
import java.awt.*;
import java.net.*;

public class GameLoop {

	private GamePhysics physics;
	private LocalPlayer localPlayer;
	private NetListener net;
	boolean done=false;

	public GameLoop(File file, String name, Color color) throws IOException {
		localPlayer = new LocalPlayer(this, name, color);
		physics = new GamePhysics(file);
		physics.addPlayer(localPlayer);
		net = new NetListener(physics, localPlayer);
	}
	public GameLoop(InetAddress addr, int port, String name, Color color) throws IOException {
		localPlayer = new LocalPlayer(this, name, color);
		physics = new GamePhysics();
		physics.addPlayer(localPlayer);
		net = new NetListener(physics, localPlayer, addr, port);
	}

	public void start() throws IOException {
		System.out.println("creating game frame");
		GameFrame frame = new GameFrame(physics, localPlayer);
		frame.addKeyListener(localPlayer);

		new Thread(net).start();

		System.out.println("starting game loop");

		long prevTime = System.nanoTime();
		try{
		while(!done) {
//			System.out.println("loop");
			long time = System.nanoTime();
			float diff = (time-prevTime)/1e9f;

			if (time/(1000*1000*1000) > prevTime/(1000*1000*1000)) {
				System.out.println(frame.frameCount+"frames");
				frame.frameCount=0;
			}

			prevTime = time;

			boolean spawned = false;
			if (!localPlayer.isAlive() && time>=localPlayer.spawnTime) {
				localPlayer.spawn(physics.getGeometry().getSpawnPoint());
				spawned = true;
				System.out.println("local spawn!");
			}

			physics.update(diff, localPlayer);
			if (localPlayer.shooting)
				physics.createLocalBullets(localPlayer, diff, 0);

			frame.repaint();

			Thread.sleep(10);
//			if (false) throw new InterruptedException();
		}
		}catch(InterruptedException e){}

		net.cleanUp();

		frame.setVisible(false);
	}
}
