/**
 * XOJ
 * 
 */
package mj.ocraptor.extraction.tika.parser.xoj.format;

/**
 * An extremely simple point class
 * @author droberts
 *
 */
public class PagePoint
{
	private double X;
	private double Y;
	
	/**
	 * Constructor
	 */
	public PagePoint()
	{
	}
	
	/**
	 * Constructor
	 * @param x initial X
	 * @param y initial Y
	 */
	public PagePoint(double x, double y)
	{
		X = x;
		Y = y;
	}
	
	/**
	 * Set X
	 * @param x x coordinate
	 */
	public void setX(double x)
	{
		X = x;
	}
	
	/**
	 * Set Y
	 * @param y y coordinate
	 */
	public void setY(double y)
	{
		Y = y;
	}
	
	/**
	 * Set coordinate
	 * @param x x coord
	 * @param y y coord
	 */
	public void set(double x, double y)
	{
		X = x;
		Y = y;
	}
	
	/**
	 * Get X coord
	 * @return x coord
	 */
	public double getX()
	{
		return X;
	}
	
	/**
	 * Get Y coord
	 * @return y coord
	 */
	public double getY()
	{
		return Y;
	}
	
	/**
	 * String form
	 */
	public String toString()
	{
		return "[" + X + "/" + Y + "]";
	}
}
