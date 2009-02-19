package lento.gamestate;

import java.awt.*;

/**
 * ColoredPolygon on polygoni, johon on liitetty mukaan tieto sen piirtoväristä.
 * <p>
 * ColoredPolygon ei määrittele itse mitään metodeja, vaan toimii ulospäin
 * täysin kuten peritty java.awt.Polygon-luokkakin, paitsi että luokalla
 * on julkinen väriattribuutti.
 */
public class ColoredPolygon extends Polygon {

	/** Polygonin piirtoväri. */
	public Color color;
};
