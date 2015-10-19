/**
 * XOJ
 * 
 */
package mj.ocraptor.extraction.tika.parser.xoj.format;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the data for a stroke
 * (line/path)
 * 
 * @author droberts
 *
 */
public class Stroke 
{
	/**
	 * Colour parser common to all strokes
	 */
	private static ColourParser colourParser = new ColourParser();
	
	/**
	 * Width of the path as it varies over time
	 */
	private double[] pathWidth;
	
	/**
	 * Position of the path as it varies over time
	 */
	private List<PagePoint> pathPosition;
	
	/**
	 * Name of the tool this stroke is from
	 */
	private String Tool;
	
	/**
	 * Colour to draw the stroke with
	 */
	private Color DrawColor;
	
	/**
	 * Create a stroke with the given attributes (presumed to be string sequences
	 * from XML)
	 * @param tool tool name reflecting stroke tag's tool name 
	 * @param colour colour of the stroke
	 * @param widthSequence space-separated width sequence
	 * @param posSequence space spearated position sequence
	 */
	public Stroke(String tool, String colour, String widthSequence, String posSequence)
	{
		// Record tool
		Tool = tool;
		
		// Obtain appropriate colour
		DrawColor = colourParser.parse(colour);
	
		// Save path width and position
		pathWidth = recalculateWidths(getSequence(widthSequence));
		pathPosition = getPointSequence(posSequence);
	}
	
	/**
	 * Get the colour in use
	 * @return colour to draw the stroke as
	 */
	public Color getColour()
	{
		return DrawColor;
	}
	
	/**
	 * Get the name of the tool in use
	 * @return tool name
	 */
	public String getTool()
	{
		return Tool;
	}
	
	/**
	 * Get width time series
	 * @return width data
	 */
	public double[] getWidth()
	{
		return pathWidth;
	}
	
	/**
	 * Get position time series data
	 * @return position data
	 */
	public List<PagePoint> getPosition()
	{
		return pathPosition;
	}
	
	/**
	 * Reclaulcate widths as per docs of Xournal:
	 * " succession of floating-point values: first the nominal brush width, and then the width of each successive segment forming the stroke."
	 * 
	 * @param widths original width values
	 * @return recalculated widths (in relation to nominal)
	 */
	private double[] recalculateWidths(double[] widths)
	{
		// Store nominal
		double nominal = widths[0];
		
		// Run through remainder
		for (int i = 1; i < widths.length; i++)
		{
			widths[i] /= nominal; 
		}
		
		return widths;
		
	}
	
	/**
	 * Create a fixed size array of values from a space-separated
	 * sequence in XML
	 * @param spaceSeparated space separated string
	 * @return array of values
	 */
	private double[] getSequence(String spaceSeparated)
	{
		// Split by spaces
		String[] sequence = spaceSeparated.split(" ");
		
		// Annoyingly, since result may end be shorter than original,
		// initially we need a collection to avoid blanks
		// (see loop)
		ArrayList<Double> numericSequence = new ArrayList<Double>();
		
		// Iterate the split sequence and shove into collection
		for (int i = 0; i < sequence.length; i++)
		{
			// If it is NOT empty
			if (!sequence[i].trim().isEmpty())
			{
				numericSequence.add(Double.parseDouble(sequence[i]));
			}
		}
		
		// Create the numeric result
		double result[] = new double[numericSequence.size()];
		
		// Dump collection -> array
		for (int i = 0; i < numericSequence.size(); i++)
		{
			result[i] = numericSequence.get(i);
		}
		
		return result;
	}
	
	/**
	 * Get a set of X/Y pairs from a space separated sequence in XML
	 * @param spaceSeparated space separated string
	 * @return collection of 2D coordinates
	 */
	private List<PagePoint> getPointSequence(String spaceSeparated)
	{
		// Get the sequence of doubles
		double[] values = getSequence(spaceSeparated);
		
		// List we return
		ArrayList<PagePoint> pairs = new ArrayList<PagePoint>();
	
		// Iterate the numeric sequence
		for (int i = 0; i < values.length; i++)
		{
			// If there is a valid item BEYOND us
			if (i+1 < values.length)
			{
				// Create point with X and Y 
				pairs.add(new PagePoint(values[i], values[i+1]));
				
				// Increment position to skip over Y
				i++;
			}
		}
		
		return pairs;
	}
}
