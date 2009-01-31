package lento.menu;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

class PlayerInfoPanel extends JPanel implements ActionListener {

	JTextField nameField;

	/**
	 * Nykyinen värivalinta.
	 * Alkuperäinen väri valitaan HSV-paletista valitsemalla hue satunnaisesti ja
	 * asettamalla saturation ja value maksimiin, jolloin tuloksena on kirkas väri.
	 */
	Color color=Color.getHSBColor((float)Math.random(), 1, 1);

	JPanel colorViewPanel;

	PlayerInfoPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(new JLabel("Pelaajanimi"));
		add(nameField = new JTextField());
		add(new JLabel("Aluksen väri"));

		JPanel colorPanel = new JPanel();
		colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.X_AXIS));

		JButton colorButton = new JButton("Värin valintaan");
		colorButton.setActionCommand("color");
		colorButton.addActionListener(this);
		colorPanel.add(colorButton);

		colorViewPanel = new JPanel();
		colorViewPanel.setBackground(color);
		colorPanel.add(colorViewPanel);

		add(colorPanel);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("color")) {
			Color newColor = JColorChooser.showDialog(this, "Valitse aluksen väri", color);
			if (newColor != null)
				color = newColor;
			colorViewPanel.setBackground(color);
		}
	}
};
