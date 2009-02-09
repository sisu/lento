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
	GameFrame frame;
	boolean done=false;

	public GameLoop(File file, String name, Color color) throws IOException {
		localPlayer = new LocalPlayer(this, name, color);
		physics = new GamePhysics(file);
		net = new NetListener(physics, localPlayer);
		physics.addPlayer(localPlayer);
		physics.setObserver(net);
	}
	public GameLoop(InetAddress addr, int port, String name, Color color) throws IOException {
		localPlayer = new LocalPlayer(this, name, color);
		physics = new GamePhysics();
		net = new NetListener(physics, localPlayer, addr, port);
		physics.addPlayer(localPlayer);
		physics.setObserver(net);
	}

	public void start() throws IOException {
		System.out.println("creating game frame");
		frame = new GameFrame(physics, localPlayer);
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

			boolean spawned = false;
			if (!localPlayer.isAlive() && time>=localPlayer.spawnTime) {
				localPlayer.spawn(physics.getGeometry().getSpawnPoint());
				spawned = true;
				System.out.println("local spawn!");
				net.sendSpawn();
			}

			boolean alive = localPlayer.isAlive();
			physics.update(diff, localPlayer);
			if (alive && !localPlayer.isAlive()) {
				localPlayer.spawnTime = time + LocalPlayer.SPAWN_TIME;
			}
			if (localPlayer.isAlive() && localPlayer.shooting) {
				int count = physics.createLocalBullets(localPlayer, prevTime, time, localPlayer.nextShootTime, localPlayer.nextBulletID);
				if (count!=0) System.out.println("cnt "+count);
				localPlayer.nextBulletID += count;
				localPlayer.nextShootTime += count*GamePhysics.SHOOT_INTERVAL;
			}

			net.updateChanges();

			frame.repaint();

			prevTime = time;
			Thread.sleep(10);
//			if (false) throw new InterruptedException();
		}
		}catch(InterruptedException e){}

		net.cleanUp();

		frame.setVisible(false);
	}
}
