/**
 * XOJ
 * 
 */
package mj.ocraptor.extraction.tika.parser.xoj.format;

import java.awt.Color;
import java.util.HashMap;

/**
 * Handles parsing of colour names/values
 * 
 * @author droberts
 */
public class ColourParser 
{
	/**
	 * Map of colour values
	 */
	private HashMap<String,Color> namingMap;

	/**
	 * Constructor
	 */
	public ColourParser()
	{
		// Build the colour map
		namingMap = new HashMap<String, Color>();
		
		// Add all supported colours
		namingMap.put("yellow", new Color(255,255,0));
		namingMap.put("red", new Color(255,0,0));
		namingMap.put("green", new Color(0,255,0));
		namingMap.put("blue", new Color(0,0,255));
		namingMap.put("white", new Color(255,255,255));
		namingMap.put("black", new Color(0,0,0));
	}
	
	/**
	 * Parse the given colour name into an RGB colour
	 * @param colourName colour name to obtain
	 * @return colour value
	 */
	public Color parse(String colourName)
	{
		String lowerCase = colourName.toLowerCase();
		
		// In our map?
		if (namingMap.containsKey(lowerCase))
		{
			return namingMap.get(lowerCase);
		} else {
			
			// Wanring error.
			System.err.println("Warning: Ignored unknown colour '" + colourName + "'");
			
			// Return black as fallback
			return new Color(0,0,0);
		}
	}
}
