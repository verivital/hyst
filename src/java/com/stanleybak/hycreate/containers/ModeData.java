package com.stanleybak.hycreate.containers;

import java.util.ArrayList;

/**
 * The data for each mode
 * 
 * @author Stanley Bak
 *
 */
public class ModeData
{
	private String gridSize = "";
	private String regridRatio = "";

	public final static String DEFAULT_DERIVATIVE = "return new Interval(0,0); // derivative is constant and zero";

	public final static String DEFAULT_MIN_MAX_POINTS = "return null; // derivative has no min/max points in any box";

	public final static String DEFAULT_INVARIANT = "return true; // any continuous state is permitted";

	public final static String DEFAULT_TIME_TRIGGER = "return null; // not time triggered";

	private String invariant = DEFAULT_INVARIANT;
	private String timeTrigger = DEFAULT_TIME_TRIGGER;

	private ArrayList<String> derivative = new ArrayList<String>();
	private ArrayList<String> minMaxPoints = new ArrayList<String>();

	public ModeData()
	{

	}

	public ModeData(int numDimensions)
	{
		for (int d = 0; d < numDimensions; ++d)
		{
			if (d != 0)
				regridRatio += ", ";

			regridRatio += "1.7";

			derivative.add(DEFAULT_DERIVATIVE);
			minMaxPoints.add(DEFAULT_MIN_MAX_POINTS);
		}
	}

	public ModeData(ModeData other)
	{
		this.gridSize = other.gridSize;
		this.regridRatio = other.regridRatio;
		this.invariant = other.invariant;

		for (int d = 0; d < other.derivative.size(); ++d)
		{
			this.derivative.add(other.derivative.get(d));
			this.minMaxPoints.add(other.minMaxPoints.get(d));
		}
	}

	public String getGridSize()
	{
		return gridSize;
	}

	public void setGridSize(String gridSize)
	{
		this.gridSize = gridSize;
	}

	public String getRegridRatio()
	{
		return regridRatio;
	}

	public void setRegridRatio(String regridRatio)
	{
		this.regridRatio = regridRatio;
	}

	public String getInvariant()
	{
		return invariant;
	}

	public void setInvariant(String invariant)
	{
		this.invariant = invariant;
	}

	public ArrayList<String> getDerivative()
	{
		return derivative;
	}

	public void setDerivative(ArrayList<String> derivative)
	{
		this.derivative = derivative;
	}

	public ArrayList<String> getMinMaxPoints()
	{
		return minMaxPoints;
	}

	public void setMinMaxPoints(ArrayList<String> minMaxPoints)
	{
		this.minMaxPoints = minMaxPoints;
	}

	public void resizeDimensions(int newNumDimensions)
	{
		// add if there were too few
		for (int d = derivative.size(); d < newNumDimensions; ++d)
		{
			if (d != 0)
			{
				if (gridSize.trim().length() > 0)
					gridSize += ", ";

				regridRatio += ", ";
			}

			if (gridSize.trim().length() > 0)
				gridSize += "0.1";

			regridRatio += "1.7";

			derivative.add(DEFAULT_DERIVATIVE);
			minMaxPoints.add(DEFAULT_MIN_MAX_POINTS);
		}

		// remove if there were too many
		while (derivative.size() > newNumDimensions)
		{
			int size = derivative.size();

			derivative.remove(size - 1);
			minMaxPoints.remove(size - 1);

			// find last comma and do substring operation if it exists
			int i = gridSize.lastIndexOf(',');

			if (i != -1)
				gridSize = gridSize.substring(0, i).trim();

			i = regridRatio.lastIndexOf(',');

			if (i != -1)
				regridRatio = regridRatio.substring(0, i).trim();
		}
	}

	public String getTimeTrigger()
	{
		return timeTrigger;
	}

	public void setTimeTrigger(String timeTrigger)
	{
		this.timeTrigger = timeTrigger;
	}
}
