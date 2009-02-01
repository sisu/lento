package lento.menu;

import lento.gameui.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class MenuFrame extends JFrame implements ActionListener {

	private PlayerInfoPanel playerInfo;
	private HostPanel host;
	private JoinPanel join;

	public MenuFrame() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("Lento");

		Container content = getContentPane();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		add(playerInfo = new PlayerInfoPanel());
		add(host = new HostPanel(this));
		add(join = new JoinPanel(this));

		Border br = BorderFactory.createLineBorder(Color.black);
		playerInfo.setBorder(br);
		host.setBorder(br);
		join.setBorder(br);

		pack();
	}
	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		System.out.println(action);
		try {
			GameLoop loop=null;
			setVisible(false);
			if (action.equals("host")) {
				loop = new GameLoop(new File(host.filename.getText()));
			} else if (action.equals("join")) {
			}
			if (loop!=null) {
				new GameStarter(loop,this).start();
			}
		} catch(IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	private class GameStarter extends Thread {
		private GameLoop loop;
		MenuFrame menu;
		GameStarter(GameLoop loop, MenuFrame menu) {
			this.loop = loop;
			this.menu = menu;
		}
		public void run() {
			loop.start();
			menu.setVisible(true);
		}
	}
}
