package lento.menu;

import javax.swing.*;
import java.awt.event.*;

class JoinPanel extends JPanel {

	JTextField hostField, portField;

	JoinPanel(ActionListener listener) {
		setLayout(new BoxLayout(this,BoxLayout.X_AXIS));

		JPanel netChoose = new JPanel();
		netChoose.setLayout(new BoxLayout(netChoose,BoxLayout.Y_AXIS));

		netChoose.add(new JLabel("Kirjoita domain-nimi tai IP-osoite"));
		netChoose.add(hostField = new JTextField());
		netChoose.add(new JLabel("Kirjoita porttinumero"));
		netChoose.add(portField = new JTextField("46972")); // FIXME: default-portti jostain definestä
		add(netChoose);

		JButton join = new JButton("Liity peliin");
		join.setActionCommand("join");
		join.addActionListener(listener);
		add(join);
	}
}
