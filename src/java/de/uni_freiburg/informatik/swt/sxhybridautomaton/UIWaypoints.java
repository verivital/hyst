package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import java.util.ArrayList;

/**
 * Waypoints are used by the Model Editor to further specify the shape of a Transition's arrow. They
 * consist of a series of points before and after the middle point of the Transition arrow given as
 * double x double coordinates.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class UIWaypoints
{

	private ArrayList<UIPosition> mBeforeMiddle = new ArrayList<UIPosition>();
	private ArrayList<UIPosition> mAfterMiddle = new ArrayList<UIPosition>();

	/**
	 * Get a waypoint position
	 * 
	 * @param index
	 *            Number of the waypoint (starting at 0)
	 * @param isBefore
	 *            Get a point BEFORE (true) or AFTER (false) the arrow's middle point
	 * @return The waypoint's position or (0, 0) if the index is out of bounds.
	 */
	public UIPosition getWaypoint(int index, boolean isBefore)
	{
		ArrayList<UIPosition> positions = isBefore ? mBeforeMiddle : mAfterMiddle;
		if ((index < 0) || (index >= positions.size()))
			return new UIPosition(0, 0);
		return positions.get(index);
	}

	/**
	 * Get the number of waypoints in a list
	 * 
	 * @param isBefore
	 *            Get the number of points BEFORE (true) or AFTER (false) the arrow's middle point
	 * @return The number of waypoints in the requested list.
	 */
	public int getCount(boolean isBefore)
	{
		if (isBefore)
			return mBeforeMiddle.size();
		return mAfterMiddle.size();
	}

	/**
	 * Add a new waypoint
	 * 
	 * @param waypoint
	 *            The new waypoint's position
	 * @param insertBefore
	 *            Insert into the list of points BEFORE (true) or AFTER (false) the arrow's middle
	 *            point
	 */
	public void addWaypoint(UIPosition waypoint, boolean insertBefore)
	{
		ArrayList<UIPosition> positions = insertBefore ? mBeforeMiddle : mAfterMiddle;
		positions.add(waypoint);
	}

	/**
	 * Add a new waypoint
	 * 
	 * @param X
	 *            The new waypoint's X position
	 * @param Y
	 *            The new waypoint's Y position
	 * @param insertBefore
	 *            Insert into the list of points BEFORE (true) or AFTER (false) the arrow's middle
	 *            point
	 */
	public void addWaypoint(double X, double Y, boolean insertBefore)
	{
		addWaypoint(new UIPosition(X, Y), insertBefore);
	}
}
