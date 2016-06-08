/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import com.verivital.hyst.grammar.formula.Expression;

/**
 * A Location in an automaton Component. It is identified by a unique numerical ID and is also given
 * a name. Transitions reference Locations by their ID numbers.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class Location
{
	private SpaceExBaseComponent mParent;
	private int mID;
	private String mName, mNote;
	private Expression mInvariant, mFlow;
	private UIPosition mPosition;
	private UIDimensions mDimensions;

	/**
	 * Creates a new Location and adds it to the parent Component
	 * 
	 * @param parent
	 *            The Component this Location belongs to
	 */
	public Location(SpaceExBaseComponent parent)
	{
		mParent = parent;
		parent.addLocation(this);
	}

	public Location(SpaceExBaseComponent parent, int ID, String name, Expression invariant,
			Expression flow, double x, double y, double w, double h)
	{
		mParent = parent;
		setId(ID);
		setName(name);
		setInvariant(invariant);
		setFlow(flow);
		setPosition(new UIPosition(x, y));
		setDimensions(new UIDimensions(w, h));

		parent.addLocation(this);
	}

	public int getId()
	{
		return mID;
	}

	public void setId(int id)
	{
		mID = id;
	}

	public String getName()
	{
		return mName;
	}

	public void setName(String name)
	{
		mName = name;
	}

	public String getNote()
	{
		return mNote;
	}

	public void setNote(String note)
	{
		mNote = note;
	}

	public Expression getInvariant()
	{
		return mInvariant;
	}

	/**
	 * Set a new invariant.
	 * 
	 * @param invariant
	 *            The Expression should be a boolean Operation (EQUAL, LESS, GREATER, OR...)
	 * @param varInts
	 */
	public void setInvariant(Expression invariant)
	{
		mInvariant = invariant;
	}

	public Expression getFlow()
	{
		return mFlow;
	}

	/**
	 * Set a new flow.
	 * 
	 * @param assignment
	 *            The Expression should be an EQUAL Operation where the left term is a single
	 *            variable or AND Operations of such EQUAL Operations.
	 */
	public void setFlow(Expression flow)
	{
		mFlow = flow;
	}

	public UIPosition getPosition()
	{
		return mPosition;
	}

	public void setPosition(UIPosition position)
	{
		mPosition = position;
	}

	public UIDimensions getDimensions()
	{
		return mDimensions;
	}

	public void setDimensions(UIDimensions dimensions)
	{
		mDimensions = dimensions;
	}

	public SpaceExBaseComponent getParent()
	{
		return mParent;
	}

	public void setParent(SpaceExBaseComponent parent)
	{
		mParent = parent;
	}
}
