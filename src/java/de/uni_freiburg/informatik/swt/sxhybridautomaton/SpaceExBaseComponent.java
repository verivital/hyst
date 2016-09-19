/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import java.util.ArrayList;

/**
 * A Base Component is a hybrid automaton. It consists of Locations and
 * transitions.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class SpaceExBaseComponent extends SpaceExComponent
{
	private ArrayList<Location> mLocations = new ArrayList<Location>();
	private ArrayList<Transition> mTransitions = new ArrayList<Transition>();

	/**
	 * Creates a new Component and adds it to the parent Document
	 * 
	 * @param parent
	 *            The Document this Component belongs to
	 */
	public SpaceExBaseComponent(SpaceExDocument parent)
	{
		super(parent);
	}

	/**
	 * Get a Location by it's index.
	 * 
	 * @param index
	 *            Number of the Location, starting at 0
	 * @return The requested Location OR null if the index is out of bounds.
	 */
	public Location getLocation(int index)
	{
		if ((index < 0) || (index >= mLocations.size()))
			return null;
		return mLocations.get(index);
	}

	/**
	 * Get a Location by it's ID.
	 * 
	 * @param id
	 *            The Location's ID.
	 * @return The requested Location OR null if there is no Location with the
	 *         wanted ID.
	 */
	public Location getLocationByID(int id)
	{
		for (Location l : mLocations)
		{
			if (l.getId() == id)
				return l;
		}
		return null;
	}

	/**
	 * Get a Location by it's name.
	 * 
	 * @param id
	 *            The Location's name.
	 * @return A Location with the given name (there may be several!) OR null if
	 *         there is no Location with the wanted name.
	 */
	public Location getLocation(String name)
	{
		for (Location l : mLocations)
		{
			if (l.getName().equalsIgnoreCase(name))
				return l;
		}
		return null;
	}

	public int getLocationCount()
	{
		return mLocations.size();
	}

	/**
	 * Add a Location. The Component will become it's parent.
	 * 
	 * @param location
	 *            The Location to add.
	 */
	public void addLocation(Location location)
	{
		location.setParent(this);
		mLocations.add(location);
	}

	/**
	 * Get a Transition by it's index.
	 * 
	 * @param index
	 *            Number of the Transition, starting at 0
	 * @return The requested Transition OR null if the index is out of bounds.
	 */
	public Transition getTransition(int index)
	{
		if ((index < 0) || (index >= mTransitions.size()))
			return null;
		return mTransitions.get(index);
	}

	/**
	 * Get a Transition by it's label.
	 * 
	 * @param id
	 *            The Transition's label.
	 * @return The requested Transition OR null if there is no Transition with
	 *         the wanted label.
	 */
	public Transition getTransition(String label)
	{
		for (Transition t : mTransitions)
		{
			if (t.getLabel().equals(label))
				return t;
		}
		return null;
	}

	public int getTransitionCount()
	{
		return mTransitions.size();
	}

	/**
	 * Add a Transition. The Component will become its parent.
	 * 
	 * @param transition
	 *            The Transition to add.
	 */
	public void addTransition(Transition transition)
	{
		transition.setParent(this);
		mTransitions.add(transition);
	}

	/**
	 * Arrange the transitions such that the unlabeled ones are before the
	 * labeled ones
	 */
	public void arrangeTransitions()
	{
		ArrayList<Transition> unlabeled = new ArrayList<Transition>();
		ArrayList<Transition> labeled = new ArrayList<Transition>();

		for (Transition t : mTransitions)
		{
			if (t.getLabel() == null)
				unlabeled.add(t);
			else
				labeled.add(t);
		}

		mTransitions.clear();

		mTransitions.addAll(unlabeled);
		mTransitions.addAll(labeled);
	}

	@Override
	public String toString()
	{
		StringBuffer rv = new StringBuffer();

		rv.append("[Base Component " + super.toString() + "\n");

		rv.append("with " + getLocationCount() + " locations and " + getTransitionCount()
				+ " transitions\n");

		for (int i = 0; i < getLocationCount(); ++i)
		{
			Location loc = getLocation(i);

			String flow = loc.getFlow() == null ? "<null>" : loc.getFlow().toDefaultString();
			String inv = loc.getInvariant() == null ? "<null>"
					: loc.getInvariant().toDefaultString();

			rv.append("Location index=" + i + ", name='" + loc.getName() + "', id=" + loc.getId()
					+ ", flow=" + flow + ", invariant=" + inv + "\n");
		}

		for (int i = 0; i < getTransitionCount(); ++i)
		{
			Transition t = getTransition(i);

			String from = getLocationByID((t.getSource())).getName();
			String to = getLocationByID((t.getTarget())).getName();

			String guard = t.getGuard() == null ? "<null>" : t.getGuard().toDefaultString();
			String reset = t.getAssignment() == null ? "<null>"
					: t.getAssignment().toDefaultString();

			rv.append("Transition index=" + i + ", " + from + " -> " + to + ", LABEL: "
					+ t.getLabel() + ", GUARD=" + guard + ", RESET=" + reset + "\n");
		}

		return rv.toString();
	}
}
