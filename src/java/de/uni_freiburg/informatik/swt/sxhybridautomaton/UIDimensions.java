package de.uni_freiburg.informatik.swt.sxhybridautomaton;

/**
 * Size of an element's visualization in the Model Editor.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class UIDimensions {
	private double mWidth, mHeight;

	public UIDimensions(double width, double height) {
		mWidth = width;
		mHeight = height;
	}

	public double getWidth() {
		return mWidth;
	}

	public void setWidth(double width) {
		mWidth = width;
	}

	public double getHeight() {
		return mHeight;
	}

	public void setHeight(double height) {
		mHeight = height;
	}
}
