package lento.gamestate;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.io.*;

/**
 * AreaGeometry pitää kirjaa alueen koosta ja esteistä, ja tarjoaa mahdollisuuden tarkistaa, osuuko viiva polygonin reunaan.
 */
public class AreaGeometry {

	private ArrayList<ColoredPolygon> polygons = new ArrayList<ColoredPolygon>();
	private ArrayList<Edge> edges = new ArrayList<Edge>();
	private int sizeW=0,sizeH=0;
	Color borderColor=null;

	/** Luo pelialueen lukemalla sen tiedot tiedostosta.
	 * @param file tiedosto, josta tiedot luetaan.
	 */
	AreaGeometry(File file) throws IOException {
		readFile(file);
	}
	/** Luo tyhjän 0x0-kokoisen pelialueen.
	 */
	AreaGeometry() {
	}

	/** Määrittää, törmääkö jana johonkin pelialueen esteeseen tai reunaan.
	 * @param a janan lähtöpiste
	 * @param b janan loppupiste
	 * @return pisteen, jossa a:sta b:hen kulkeva jana ensimmäisenä törmää johonkin ja sitä vastaavan normaalivektorin
	 * @return null, jos jana (a,b) ei leikkaa mitään kentän esteistä eikä kentän reunaa
	 */
	Collision getCollision(Point2D.Float a, Point2D.Float b) {
//		System.out.printf("testing: %f %f - %f %f\n", a.x,a.y,b.x,b.y);
		Collision res=null;
		for(Edge e : edges) {
			Point2D.Float c = e.start;
			Point2D.Float d = e.end;

			if (crosspf(a.x,a.y,b.x,b.y,c.x,c.y)*crosspf(a.x,a.y,b.x,b.y,d.x,d.y) <= 0
				&& crosspf(c.x,c.y,d.x,d.y,a.x,a.y)*crosspf(c.x,c.y,d.x,d.y,b.x,b.y) <= 0)
			{
				// Törmäys tapahtui, määritetään sen paikka
	//			System.out.println("hit");
				float dax=b.x-a.x;
				float day=b.y-a.y;
				float dbx=d.x-c.x;
				float dby=d.y-c.y;
				float n = dax*dby-day*dbx;
				float p = a.x*b.y-a.y*b.x;
				float q = c.x*d.y-c.y*d.x;
				Point2D.Float intersection = new Point2D.Float(-(p*dbx-q*dax)/n, -(p*dby-q*day)/n);
				if (res==null || a.distanceSq(intersection) < a.distanceSq(res.getLoc()))
					res = new Collision(intersection, e.normal);
			}
		}
		return res;
	}

	/**
	 * Sisältää tiedon yhdestä monikulmion särmästä.
	 */
	private class Edge {
		/** Alkupiste */
		Point2D.Float start;
		/** Loppupiste */
		Point2D.Float end;
		/** Normaalivektori */
		Point2D.Float normal;

		/** Luo uuden särmän.
		 * @param x1 alkupisteen x-koordinaatti
		 * @param y1 alkupisteen y-koordinaatti
		 * @param x2 loppupisteen x-koordinaatti
		 * @param y2 loppupisteen y-koordinaatti
		 * @param nx normaalivektorin x-koordinaatti
		 * @param ny normaalivektorin y-koordinaatti
		 */
		Edge(float x1, float y1, float x2, float y2, float nx, float ny) {
			start = new Point2D.Float(x1,y1);
			end = new Point2D.Float(x2,y2);
			float len = (float)Math.sqrt(nx*nx+ny*ny);
			normal = new Point2D.Float(nx/len,ny/len);
		}
	}

	/** Lukee pelialueen tiedot tiedostosta.
	 * @param file tiedosto, josta pelialueen tiedot luetaan.
	 */
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
			setSize(sizeW,sizeH);
		} catch(NumberFormatException e) {
			readError(file,lineCount,"Virheellinen numeroformaatti.");
		}
	}
	/** Heittää poikkeuksen, joka kertoo kenttätiedoston luvussa tapahtuneesta virheestä.
	 * @param file tiedosto, jota oltiin lukemassa
	 * @param line tiedoston rivi, jolla virhe tapahtui
	 * @param msg lisätietoa virheestä
	 */
	private void readError(File file, int line, String msg) throws IOException {
		throw new IOException("Tiedoton "+file.getAbsolutePath()+" luku epäonnistui rivillä "+line+": "+msg);
	}

	/** Palauttaa pelialueen polygonit. Tämä ei sisällä kentän reunoja.
	 * @return taulukko kentän polygoneista.
	 */
	public ArrayList<ColoredPolygon> getPolygons() {
		return polygons;
	}

	/** Generoi satunnaisen syntymispaikan pelaajalle.
	 * Generoitu paikka ei ole minkään polygonin sisällä.
	 * @return satunnainen tyhjä paikka pelialueen sisällä
	 */
	public Point2D.Float getSpawnPoint() {
		float x,y;
		boolean ok;
		do {
			x=(float)(Math.random()*sizeW);
			y=(float)(Math.random()*sizeH);

			ok = true;
			for(ColoredPolygon p : polygons) {
				if (p.contains(x,y)) {
					ok=false;
					break;
				}
			}
		} while(!ok);
		return new Point2D.Float(x,y);
	}

	/** Määrittää polygonin särmät ja lisää ne särmälistaan.
	 * @param p polygoni, jonka särmät määritetään
	 */
	private void addEdges(Polygon p) {
		// Testaa, ovatko kärkipisteet listattu myötä- vai vastapäivään
		long area=0;
		int x0=p.xpoints[0], y0=p.ypoints[0];
		for(int i=2; i<p.npoints; ++i)
			area += crossp(x0,y0,p.xpoints[i-1],p.ypoints[i-1],p.xpoints[i],p.ypoints[i]);
		int direction = area<0 ? 1 : -1;

		for(int i=1; i<p.npoints; ++i)
			addEdge(p.xpoints[i-1],p.ypoints[i-1],p.xpoints[i],p.ypoints[i], direction);
		addEdge(p.xpoints[p.npoints-1],p.ypoints[p.npoints-1],p.xpoints[0],p.ypoints[0], direction);
	}

	/** Lisää yhden särmän särmälistaan.
	 * @param x1 alkupisteen x-koordinaatti
	 * @param y1 alkupisteen y-koordinaatti
	 * @param x2 loppupisteen x-koordinaatti
	 * @param y2 loppupisteen y-koordinaatti
	 * @param direction -1 tai 1 riippuen siitä kumpaan suuntaan pinnan normaali on suuntaunut
	 */
	private void addEdge(int x1, int y1, int x2, int y2, int direction) {
		int nx = (y1-y2)*direction;
		int ny = (x2-x1)*direction;
		System.out.printf("adding edge: %d %d - %d %d ; %d %d\n", x1,y1,x2,y2,nx,ny);
		edges.add(new Edge(x1,y1,x2,y2,nx,ny));
	}

	/** Palauttaa pelialueen leveyden.
	 * @return pelialueen leveys.
	 */
	public int getWidth() {
		return sizeW;
	}
	/** Palauttaa pelialueen korkeuden.
	 * @return pelialueen korkeus.
	 */
	public int getHeight() {
		return sizeH;
	}
	/** Palauttaa pelialueen reunojen värin.
	 * @return reunojen väri
	 */
	public Color getBorderColor() {
		return borderColor;
	}

	/** Poistaa kentällä olevat esteet ja asettaa uuden koon kentälle.
	 * @param w kentän uusi leveys
	 * @param h kentän uusi korkeus
	 */
	public void resetArea(int w, int h) {
		edges.clear();
		polygons.clear();
		setSize(w,h);
	}
	/** Asettaa pelialueen koon ja luo reunojen särmät.
	 * @param w kentän leveys
	 * @param h kentän korkeus
	 */
	private void setSize(int w, int h) {
		sizeW = w;
		sizeH = h;
		addEdge(0,0,w,0,1);
		addEdge(w,0,w,h,1);
		addEdge(w,h,0,h,1);
		addEdge(0,h,0,0,1);
	}
	/** Asettaa reunojen värin.
	 * @param c uusi reunaväri
	 */
	public void setBorderColor(Color c) {
		borderColor = c;
	}
	/** Lisää polygonin alueen esteisiin.
	 * @param poly lisättävä polygoni
	 */
	public void addPolygon(ColoredPolygon poly) {
		polygons.add(poly);
		addEdges(poly);
	}

	/** Laskee ristitulon suuruuden kaksiulotteisille kokonaislukuvektoreille.
	 * @param x0 origin x-koordinaatti
	 * @param y0 origin y-koordinaatti
	 * @param x1 1. vektorin x-koordinaatti
	 * @param y1 1. vektorin y-koordinaatti
	 * @param x2 2. vektorin x-koordinaatti
	 * @param y2 2. vektorin y-koordinaatti
	 */
	private static long crossp(long x0, long y0, long x1, long y1, long x2, long y2) {
		x1-=x0; y1-=y0;
		x2-=x0; y2-=y0;
		return x1*y2-x2*y1;
	}
	/** Laskee ristitulon suuruuden kaksiulotteisille liukulukuvektoreille.
	 * @param x0 origin x-koordinaatti
	 * @param y0 origin y-koordinaatti
	 * @param x1 1. vektorin x-koordinaatti
	 * @param y1 1. vektorin y-koordinaatti
	 * @param x2 2. vektorin x-koordinaatti
	 * @param y2 2. vektorin y-koordinaatti
	 */
	private static float crosspf(float x0, float y0, float x1, float y1, float x2, float y2) {
		x1-=x0; y1-=y0;
		x2-=x0; y2-=y0;
		return x1*y2-x2*y1;
	}
}
