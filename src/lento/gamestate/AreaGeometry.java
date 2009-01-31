package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;

/*
 * AreaGeometry pitää kirjaa alueen koosta ja esteistä, ja tarjoaa mahdollisuuden tarkistaa, osuuko viiva polygonin reunaan.
 */
class AreaGeometry {

	private ArrayList<ColoredPolygon> polygons = new ArrayList<ColoredPolygon>();
	private ArrayList<Edge> edges = new ArrayList<Edge>();
	private int sizeW,sizeH;
	Color borderColor;

	AreaGeometry(String filename) throws IOException {
		readFile(filename);
	}
	AreaGeometry() {
	}

	Collision getCollision(Point2D.Float start, Point2D.Float end) {
		return null;
	}

	private class Edge {
		Point2D start,end,normal;
	}

	private void readFile(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename))));
		String line;
		
		sizeW=sizeH=0;
		int lineCount=0;
		try {
			while((line=reader.readLine())!=null) {
				++lineCount;
				if (line.length()==0 || line.charAt(0)=='#')
					continue;

				if (sizeW==0) {
					// lue sizeW, sizeH, borderColor;
					String[] parts = line.split("[ \t]");
					if (parts.length!=3)
						readError(filename, lineCount, "Rivillä oltava alueen korkeus, leveys ja reunojen väri.");

					sizeW = Integer.parseInt(parts[0]);
					sizeH = Integer.parseInt(parts[1]);
					borderColor = new Color(Integer.parseInt(parts[2],16), false);
				} else {
					// lue polygonin väri ja kärjet
					String[] parts = line.split("[ \t]");
					if (parts.length%2==1)
						readError(filename,lineCount,"Polygonin x- ja y-koordinaattien määrä ei täsmää.");

					ColoredPolygon p = new ColoredPolygon();
					p.color = new Color(Integer.parseInt(parts[0], 16), false);

					for(int i=1; i<parts.length; i+=2) {
						int x = Integer.parseInt(parts[i]), y = Integer.parseInt(parts[i+1]);
						p.addPoint(x,y);
					}
					polygons.add(p);
				}
			}
		} catch(NumberFormatException e) {
			readError(filename,lineCount,"Virheellinen numeroformaatti.");
		}
	}
	private void readError(String filename, int line, String msg) throws IOException {
		throw new IOException("Tiedoton "+filename+" luku epäonnistui rivillä "+line+": "+msg);
	}
}
