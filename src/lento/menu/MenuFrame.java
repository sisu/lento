package lento.menu;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class MenuFrame extends JFrame implements ActionListener {

	private PlayerInfoPanel playerInfo;
	private HostPanel host;
	private JoinPanel join;

	public MenuFrame() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("Lento");

		Container content = getContentPane();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.add(playerInfo = new PlayerInfoPanel());
		content.add(host = new HostPanel(this));
		content.add(join = new JoinPanel(this));

		Border br = BorderFactory.createLineBorder(Color.black);
		playerInfo.setBorder(br);
		host.setBorder(br);
		join.setBorder(br);

		pack();
	}
	public void actionPerformed(ActionEvent e) {
	}
}
