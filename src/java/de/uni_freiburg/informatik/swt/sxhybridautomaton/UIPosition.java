package de.uni_freiburg.informatik.swt.sxhybridautomaton;

/**
 * Position of an element's visualization in the Model Editor.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class UIPosition {
	private double mX, mY;
	
	public UIPosition(double X, double Y) {
		mX = X;
		mY = Y;
	}

	public double getX() {
		return mX;
	}

	public void setX(double x) {
		mX = x;
	}

	public double getY() {
		return mY;
	}

	public void setY(double y) {
		mY = y;
	}
}
