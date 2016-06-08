/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import com.verivital.hyst.grammar.formula.Expression;

/**
 * A Transition connects two Locations within a Component. Locations are referenced by their
 * numerical IDs.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class Transition
{
	private SpaceExBaseComponent mParent;
	private int mSource, mTarget, mPriority;
	private boolean mAsap, mTimeDriven, mBezier;
	private String mLabel, mNote;
	private Expression mGuard, mAssignment;
	private UIPosition mLabelPosition, mMiddlepointPosition;
	private UIDimensions mLabelDimensions;
	private UIWaypoints mWaypoints;

	/**
	 * Creates a new Transition and adds it to the parent Component
	 * 
	 * @param parent
	 *            The Component this Transition belongs to
	 */
	public Transition(SpaceExBaseComponent parent)
	{
		mParent = parent;
		parent.addTransition(this);
	}

	public Transition(SpaceExBaseComponent parent, int source, int target, Expression guard,
			Expression assignment, double w, double h)
	{
		mParent = parent;
		setSource(source);
		setTarget(target);
		setGuard(guard);
		setAssignment(assignment);
		setLabelPosition(new UIPosition(-(w / 2), -h));
		setLabelDimensions(new UIDimensions(w, h));

		parent.addTransition(this);
	}

	public int getSource()
	{
		return mSource;
	}

	public void setSource(int source)
	{
		mSource = source;
	}

	public int getTarget()
	{
		return mTarget;
	}

	public void setTarget(int target)
	{
		mTarget = target;
	}

	public int getPriority()
	{
		return mPriority;
	}

	public void setPriority(int priority)
	{
		mPriority = priority;
	}

	public boolean isAsap()
	{
		return mAsap;
	}

	public void setAsap(boolean asap)
	{
		mAsap = asap;
	}

	public boolean isTimeDriven()
	{
		return mTimeDriven;
	}

	public void setTimeDriven(boolean timeDriven)
	{
		mTimeDriven = timeDriven;
	}

	public boolean isBezier()
	{
		return mBezier;
	}

	public void setBezier(boolean bezier)
	{
		mBezier = bezier;
	}

	/**
	 * A Transition's Label references a LabelParam
	 * 
	 * @return The Label
	 */
	public String getLabel()
	{
		return mLabel;
	}

	/**
	 * A Transition's Label references a LabelParam
	 * 
	 * @param label
	 *            The Label
	 */
	public void setLabel(String label)
	{
		mLabel = label;
	}

	public String getNote()
	{
		return mNote;
	}

	public void setNote(String note)
	{
		mNote = note;
	}

	public Expression getGuard()
	{
		return mGuard;
	}

	/**
	 * Set a new guard.
	 * 
	 * @param guard
	 *            The Expression should be a boolean Operation (EQUAL, LESS, GREATER, OR...)
	 */
	public void setGuard(Expression guard)
	{
		mGuard = guard;
	}

	public Expression getAssignment()
	{
		return mAssignment;
	}

	/**
	 * Set a new assignment.
	 * 
	 * @param assignment
	 *            The Expression should be an EQUAL Operation where the left term is a single
	 *            variable or AND Operations of such EQUAL Operations.
	 */
	public void setAssignment(Expression assignment)
	{
		mAssignment = assignment;
	}

	public UIPosition getLabelPosition()
	{
		return mLabelPosition;
	}

	public void setLabelPosition(UIPosition labelPosition)
	{
		mLabelPosition = labelPosition;
	}

	public UIPosition getMiddlepointPosition()
	{
		return mMiddlepointPosition;
	}

	public void setMiddlepointPosition(UIPosition middlepointPosition)
	{
		mMiddlepointPosition = middlepointPosition;
	}

	public UIDimensions getLabelDimensions()
	{
		return mLabelDimensions;
	}

	public void setLabelDimensions(UIDimensions labelDimensions)
	{
		mLabelDimensions = labelDimensions;
	}

	public UIWaypoints getWaypoints()
	{
		return mWaypoints;
	}

	public void setWaypoints(UIWaypoints waypoints)
	{
		mWaypoints = waypoints;
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
