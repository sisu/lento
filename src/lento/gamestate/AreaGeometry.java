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
	private int sizeW=0,sizeH=0;
	Color borderColor=null;

	AreaGeometry(File file) throws IOException {
		readFile(file);
	}
	AreaGeometry() {
	}

	Collision getCollision(Point2D.Float a, Point2D.Float b) {
//		System.out.printf("testing: %f %f - %f %f\n", a.x,a.y,b.x,b.y);
		Collision res=null;
		for(Iterator<Edge> i=edges.iterator(); i.hasNext(); ) {
			Edge e = i.next();
			Point2D.Float c = e.start;
			Point2D.Float d = e.end;

			if (crosspf(a.x,a.y,b.x,b.y,c.x,c.y)*crosspf(a.x,a.y,b.x,b.y,d.x,d.y) < 0
				&& crosspf(c.x,c.y,d.x,d.y,a.x,a.y)*crosspf(c.x,c.y,d.x,d.y,b.x,b.y) < 0)
			{
				System.out.println("hit");
				float dax=b.x-a.x;
				float day=b.y-a.y;
				float dbx=d.x-c.x;
				float dby=d.y-c.y;
				float n = dax*dby-day*dbx;
				float p = a.x*b.y-a.y*b.x;
				float q = c.x*d.y-c.y*d.x;
				Point2D.Float intersection = new Point2D.Float(-(p*dbx-q*dax)/n, -(p*dby-q*day)/n);
				if (res==null || a.distanceSq(intersection) < a.distanceSq(res.location))
					res = new Collision(intersection, e.normal);
			}
		}
		return res;
	}
	private static float dist2(Point2D.Float a, Point2D.Float b) {
		float dx=b.x-a.x;
		float dy=b.y-a.y;
		return dx*dx+dy*dy;
	}

	private class Edge {
		Point2D.Float start,end,normal;
		Edge(float x1, float y1, float x2, float y2, float nx, float ny) {
			start = new Point2D.Float(x1,y1);
			end = new Point2D.Float(x2,y2);
			float len = (float)Math.sqrt(nx*nx+ny*ny);
			normal = new Point2D.Float(nx/len,ny/len);
		}
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
					addEdges(p);
				}
			}
			// Lisää kentän reunat särmiin
			addEdge(0,0,sizeW,0,1);
			addEdge(sizeW,0,sizeW,sizeH,1);
			addEdge(sizeW,sizeH,0,sizeH,1);
			addEdge(0,sizeH,0,0,1);
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
	private void addEdges(Polygon p) {
		// Testaa, ovatko kärkipisteet listattu myötä- vai vastapäivään
		int area=0;
		int x0=p.xpoints[0], y0=p.ypoints[0];
		for(int i=2; i<p.npoints; ++i)
			area += crossp(x0,y0,p.xpoints[i-1],p.ypoints[i-1],p.xpoints[i],p.ypoints[i]);
		int direction = area<0 ? 1 : -1;

		for(int i=1; i<p.npoints; ++i)
			addEdge(p.xpoints[i-1],p.ypoints[i-1],p.xpoints[i],p.ypoints[i], direction);
		addEdge(p.xpoints[p.npoints-1],p.ypoints[p.npoints-1],p.xpoints[0],p.ypoints[0], direction);
	}
	private void addEdge(int x1, int y1, int x2, int y2, int direction) {
		int nx = (y1-y2)*direction;
		int ny = (x2-x1)*direction;
		System.out.printf("adding edge: %d %d - %d %d ; %d %d\n", x1,y1,x2,y2,nx,ny);
		edges.add(new Edge(x1,y1,x2,y2,nx,ny));
	}
	private static int crossp(int x0, int y0, int x1, int y1, int x2, int y2) {
		x1-=x0; y1-=y0;
		x2-=x0; y2-=y0;
		return x1*y2-x2*y1;
	}
	private static float crosspf(float x0, float y0, float x1, float y1, float x2, float y2) {
		x1-=x0; y1-=y0;
		x2-=x0; y2-=y0;
		return x1*y2-x2*y1;
	}

	public int getWidth() {
		return sizeW;
	}
	public int getHeight() {
		return sizeH;
	}
	public Color getBorderColor() {
		return borderColor;
	}

	public void setSize(int w, int h) {
		sizeW = w;
		sizeH = h;
	}
	public void setBorderColor(Color c) {
		borderColor = c;
	}
	public void addPolygon(ColoredPolygon poly) {
		polygons.add(poly);
		addEdges(poly);
	}
}
