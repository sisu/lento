package lento.menu;

import javax.swing.*;
import java.awt.event.*;

class HostPanel extends JPanel implements ActionListener {

	JTextField filename;

	HostPanel(ActionListener listener) {
		setLayout(new BoxLayout(this,BoxLayout.X_AXIS));

		JPanel fileChoose = new JPanel();
		fileChoose.setLayout(new BoxLayout(fileChoose,BoxLayout.Y_AXIS));

		fileChoose.add(new JLabel("Valitse karttatiedosto"));

		JButton browse = new JButton("Selaa");
		browse.setActionCommand("browse");
		browse.addActionListener(this);
		fileChoose.add(browse);

		fileChoose.add(filename = new JTextField());

		add(fileChoose);

		JButton b = new JButton("Luo uusi peli");
		b.setActionCommand("host");
		b.addActionListener(listener);
		add(b);
	}
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("browse")) {
			JFileChooser fc = new JFileChooser("./lev/");
			int retVal = fc.showOpenDialog(this);
			if (retVal==JFileChooser.APPROVE_OPTION) {
				String name = fc.getSelectedFile().getAbsolutePath();
				System.out.println("jee "+retVal+" "+name);
				filename.setText(name);
			}
		}
	}
};
