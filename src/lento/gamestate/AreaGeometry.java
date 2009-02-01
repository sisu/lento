package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;

/*
 * AreaGeometry pitää kirjaa alueen koosta ja esteistä, ja tarjoaa mahdollisuuden tarkistaa, osuuko viiva polygonin reunaan.
 */
public class AreaGeometry {

	private ArrayList<ColoredPolygon> polygons = new ArrayList<ColoredPolygon>();
	private ArrayList<Edge> edges = new ArrayList<Edge>();
	private int sizeW,sizeH;
	Color borderColor;

	AreaGeometry(File file) throws IOException {
		readFile(file);
	}
	AreaGeometry() {
	}

	Collision getCollision(Point2D.Float start, Point2D.Float end) {
		return null;
	}

	private class Edge {
		Point2D start,end,normal;
	}

	private void readFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
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
					String[] parts = line.split("\\s");
					if (parts.length!=3)
						readError(file, lineCount, "Rivillä oltava alueen korkeus, leveys ja reunojen väri.");

					sizeW = Integer.parseInt(parts[0]);
					sizeH = Integer.parseInt(parts[1]);
					borderColor = new Color(Integer.parseInt(parts[2],16), false);
				} else {
					// lue polygonin väri ja kärjet
					String[] parts = line.split("\\s");
					if (parts.length%2==0)
						readError(file,lineCount,"Polygonin x- ja y-koordinaattien määrä ei täsmää.");

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
			readError(file,lineCount,"Virheellinen numeroformaatti.");
		}
	}
	private void readError(File file, int line, String msg) throws IOException {
		throw new IOException("Tiedoton "+file.getAbsolutePath()+" luku epäonnistui rivillä "+line+": "+msg);
	}
	public ArrayList<ColoredPolygon> getPolygons() {
		return polygons;
	}
	public Point2D.Float getSpawnPoint() {
		float x,y;
		boolean ok;
		do {
			x=(float)(Math.random()*sizeW);
			y=(float)(Math.random()*sizeH);

			ok = true;
			for(Iterator<ColoredPolygon> i=polygons.iterator(); i.hasNext(); ) {
				Polygon p = i.next();
				if (p.contains(x,y)) {
					ok=false;
					break;
				}
			}
		} while(!ok);
		return new Point2D.Float(x,y);
	}
}
