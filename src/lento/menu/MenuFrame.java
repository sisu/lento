package lento.menu;

import lento.gameui.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * MenuFrame on pelin valikkoikkuna.
 */
public class MenuFrame extends JFrame implements ActionListener {

	/** Paneeli omien pelaajatietojen syöttämistä varten. */
	private PlayerInfoPanel playerInfo;
	/** Paneeli uuden pelin luomista varten. */
	private HostPanel host;
	/** Paneeli peliin liittymistä varten. */
	private JoinPanel join;
	/** Pelisilmukka, joka luodaan käyttäjän valittua pelin luonti tai peliin
	 * liittyminen paneeleista. */
	private GameLoop loop;

	/** Luo valikkoikkunan ja siihen kuuluvat paneelit.
	 */
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

	/** Huolehtii pelinaloittamisnapin painalluksesta.
	 * Luo uuden pelin tai yhdistää peliin riippuen siitä, mitä nappia painettiin.
	 * Siirtyy pelitilaan ja piilottaa valikon kunne pelitilasta poistutaan.
	 * @param ae tieto painetusta napista
	 */
	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		System.out.println(action);
		try {
			loop=null;
			String name=playerInfo.nameField.getText();
			Color color = playerInfo.color;
			if (action.equals("host")) {
				loop = new GameLoop(new File(host.filename.getText()), name, color);
			} else if (action.equals("join")) {
				InetAddress addr = InetAddress.getByName(join.hostField.getText());
				int port = Integer.parseInt(join.portField.getText());
				loop = new GameLoop(addr,port,name,color);
			}
			if (loop!=null) {
				// Java odottaa, että tästä metodista palataan ennen uusien
				// tapahtumien käsittelyä, joten avataan peli uuteen säikeeseen.
				new Thread(new Runnable() {
					public void run() {
						setVisible(false);
						try {
							loop.start();
						} catch(IOException e) {
							e.printStackTrace();
						}
						setVisible(true);
					}
				}).start();
			}
		} catch(Exception e) {
			// FIXME: näytä joku ruma dialogi
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/** Luo ja asettaa näkyville alkuvalikon.
	 */
	public static void main(String[] args) {
		new MenuFrame().setVisible(true);
	}
}
