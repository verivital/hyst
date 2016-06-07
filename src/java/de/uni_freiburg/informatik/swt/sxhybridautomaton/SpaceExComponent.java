/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import java.util.ArrayList;

/**
 * A Component consists of - Locations with Transitions (Base Component) or -
 * other Components (Network Component)
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public abstract class SpaceExComponent
{
	private SpaceExDocument mParent;
	private String mID, mNote;
	private ArrayList<Param> mParams = new ArrayList<Param>();

	@Override
	public String toString()
	{
		StringBuilder rv = new StringBuilder("[Component id=" + mID + "\n");

		rv.append(" Params:\n");
		for (Param p : mParams)
		{
			String suffix = "";

			if (p.getType() == ParamType.REAL)
			{
				VariableParam vp = (VariableParam) p;

				suffix = "(" + vp.getDynamics().name() + ", "
						+ (vp.getControlled() ? "CONTROLLED" : "UNCONTROLLED") + ")";
			}

			rv.append(" " + p.getName() + " (type=" + p.getType().name() + ") " + suffix + "\n");
		}

		rv.append("]");

		return rv.toString();
	}

	/**
	 * Creates a new Component and adds it to the parent Document
	 * 
	 * @param parent
	 *            The Document this Component belongs to
	 */
	public SpaceExComponent(SpaceExDocument parent)
	{
		mParent = parent;
		parent.addComponent(this);
	}

	public void removeParam(String name)
	{
		for (int i = 0; i < mParams.size(); /* increment in loop */)
		{
			Param p = mParams.get(i);

			if (p.getName().equals(name))
				mParams.remove(i);
			else
				++i;
		}
	}

	public SpaceExDocument getParent()
	{
		return mParent;
	}

	public void setParent(SpaceExDocument parent)
	{
		mParent = parent;
	}

	public String getID()
	{
		return mID;
	}

	public void setID(String iD)
	{
		mID = iD;
	}

	public String getNote()
	{
		return mNote;
	}

	public void setNote(String note)
	{
		mNote = note;
	}

	public ArrayList<Param> getParams()
	{
		return this.mParams;
	}

	/**
	 * Get a Param by it's index.
	 * 
	 * @param index
	 *            Number of the Param, starting at 0
	 * @return The requested Param OR null if the index is out of bounds.
	 */
	public Param getParam(int index)
	{
		if ((index < 0) || (index >= mParams.size()))
			return null;
		return mParams.get(index);
	}

	/**
	 * Get a Param by it's name.
	 * 
	 * @param id
	 *            The Param's name.
	 * @return The requested Param OR null if there is no Param with the wanted
	 *         name.
	 */
	public Param getParam(String name)
	{
		for (Param p : mParams)
		{
			if (p.getName().equals(name))
				return p;
		}
		return null;
	}

	public int getParamCount()
	{
		return mParams.size();
	}

	/**
	 * Add a Param. The Component will become its parent.
	 * 
	 * @param param
	 *            The Param to add.
	 */
	public void addParam(Param param)
	{
		param.setParent(this);
		mParams.add(param);
	}
}
