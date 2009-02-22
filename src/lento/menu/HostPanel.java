package lento.menu;

import javax.swing.*;
import java.awt.event.*;

/**
 * Paneeli, jonka kautta voi luoda uuden pelin.
 */
class HostPanel extends JPanel implements ActionListener {

	/** Tekstikenttä tiedostonimen lukua ja näyttämistä varten */
	JTextField filename;

	/** Luo paneelin.
	 * @param listener olio, jota kutsutaan, kun käyttäjä painaa "Luo uusi peli"-nappia
	 */
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

	/** Käsittelee tilanteen, jossa käyttäjä painaa tiedostonvalintanappia.
	 * Avaa tiedostonvalintaikkunan ja päivittää valitun kenttätiedoston.
	 * @param e tieto napista, jota käyttäjä painoi
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("browse")) {
			JFileChooser fc = new JFileChooser("./lev/");
			int retVal = fc.showOpenDialog(this);
			if (retVal==JFileChooser.APPROVE_OPTION) {
				String name = fc.getSelectedFile().getAbsolutePath();
				filename.setText(name);
			}
		}
	}
};
