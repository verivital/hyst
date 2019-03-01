/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import java.util.ArrayList;

import com.verivital.hyst.grammar.formula.Expression;

/**
 * A SpaceEx automaton consists of one or more Components which represent automata or systems of
 * automata.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class SpaceExDocument
{
	private String mVersion = "0.2";
	private String mMathFormat = "SpaceEx";
	private ArrayList<SpaceExComponent> mComponents = new ArrayList<SpaceExComponent>();

	private Expression mInitialStateConditions;
	private Expression mForbiddenStateConditions;

	private SpaceExConfigValues mConfig = new SpaceExConfigValues();

	public SpaceExConfigValues getConfig()
	{
		return mConfig;
	}

	public String getVersion()
	{
		return mVersion;
	}

	public void setVersion(String version)
	{
		mVersion = version;
	}

	public void setSamplingTime(double samplingTime)
	{
		mConfig.samplingTime = samplingTime;
	}

	public String getMathFormat()
	{
		return mMathFormat;
	}

	public void setMathFormat(String mathFormat)
	{
		mMathFormat = mathFormat;
	}

	public void setTimeHorizon(double timeHorizon)
	{
		mConfig.timeHorizon = timeHorizon;
	}

	public void setMaxIterations(int maxIterations)
	{
		mConfig.maxIterations = maxIterations;
	}

	public void setTimeTriggered(boolean tt)
	{
		mConfig.timeTriggered = tt;
	}

	public void setSystemID(String systemID)
	{
		mConfig.systemID = systemID;
	}

	/**
	 * Get a Component by it's index.
	 * 
	 * @param index
	 *            Number of the Component, starting at 0
	 * @return The requested Component OR null if the index is out of bounds.
	 */
	public SpaceExComponent getComponent(int index)
	{
		if ((index < 0) || (index >= mComponents.size()))
			return null;
		return mComponents.get(index);
	}

	/**
	 * Get a Component by it's ID.
	 * 
	 * @param id
	 *            The Component's ID.
	 * @return The requested Component OR null if there is no Component with the wanted ID.
	 */
	public SpaceExComponent getComponent(String id)
	{
		for (SpaceExComponent c : mComponents)
		{
			if (c.getID().equalsIgnoreCase(id))
				return c;
		}

		return null;
	}

	public int getComponentCount()
	{
		return mComponents.size();
	}

	/**
	 * Add a Component. The SpaceEx Document will become its parent.
	 * 
	 * @param component
	 *            The Component to add.
	 */
	public void addComponent(SpaceExComponent component)
	{
		component.setParent(this);
		mComponents.add(component);
	}

	public Expression getInitialStateConditions()
	{
		return mInitialStateConditions;
	}

	public void setInitialStateConditions(Expression initialStateConditions)
	{
		mInitialStateConditions = initialStateConditions;
	}

	public Expression getForbiddenStateConditions()
	{
		return mForbiddenStateConditions;
	}

	public void setForbiddenStateConditions(Expression forbiddenStateConditions)
	{
		mForbiddenStateConditions = forbiddenStateConditions;
	}

	public void addOutputVar(String varName)
	{

		if (!mConfig.outputVars.contains(varName))
			mConfig.outputVars.add(varName);
	}

	public void setOutputFormat(String outputFormat)
	{
		mConfig.outputFormat = outputFormat;
	}

	public void setDirections(String dirs)
	{
		mConfig.directions = dirs;
	}

	public void setAggregation(String agg)
	{
		mConfig.aggregation = agg;
	}

	public void setForbiddenOverride(String f)
	{
		mConfig.forbidden = f;
	}

	public void setFlowpipeTolerance(double tol)
	{
		mConfig.flowpipeTol = tol;
	}

	public String toString()
	{
		StringBuilder rv = new StringBuilder();

		rv.append("[SpaceExDocument " + mConfig.systemID + " with " + mComponents.size()
				+ " components:\n");

		int count = 0;

		for (SpaceExComponent c : mComponents)
			rv.append((count++) + ": " + c.toString() + "\n");

		return rv.toString();
	}

	public void setScenario(String value)
	{
		mConfig.scenario = value;
	}
}
