/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import java.util.ArrayList;

/**
 * A Network Component is a system of automata. It consists of one or several Binds which
 * instantiate Base Components and bind their Params.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class SpaceExNetworkComponent extends SpaceExComponent
{
	private ArrayList<Bind> mBinds = new ArrayList<Bind>();

	/**
	 * Creates a new Component and adds it to the parent Document
	 * 
	 * @param parent
	 *            The Document this Component belongs to
	 */
	public SpaceExNetworkComponent(SpaceExDocument parent)
	{
		super(parent);
	}

	/**
	 * Get a Bind by it's index.
	 * 
	 * @param index
	 *            Number of the Bind, starting at 0
	 * @return The requested Bind OR null if the index is out of bounds.
	 */
	public Bind getBind(int index)
	{
		if ((index < 0) || (index >= mBinds.size()))
			return null;
		return mBinds.get(index);
	}

	/**
	 * Get a Bind by it's "As" name.
	 * 
	 * @param id
	 *            The Bind's "As" name.
	 * @return The requested Bind OR null if there is no Bind with the wanted name.
	 */
	public Bind getBind(String as)
	{
		for (Bind b : mBinds)
		{
			if (b.getAs().equals(as))
				return b;
		}
		return null;
	}

	public int getBindCount()
	{
		return mBinds.size();
	}

	/**
	 * Add a Bind. The Component will become its parent.
	 * 
	 * @param bind
	 *            The Bind to add.
	 */
	public void addBind(Bind bind)
	{
		bind.setParent(this);
		mBinds.add(bind);
	}

	public String toString()
	{
		StringBuilder rv = new StringBuilder(
				"[Network Component with " + mBinds.size() + " binds:\n");

		int index = 0;

		for (Bind b : mBinds)
			rv.append(index + ": " + b + "\n");

		rv.append("\n");

		return rv.toString();
	}
}
