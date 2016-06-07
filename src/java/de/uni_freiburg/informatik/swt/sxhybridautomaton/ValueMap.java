/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import java.util.ArrayList;

/**
 * Maps a VariableParam of the bound Component to a value. The value consists of
 * multiple REAL or INT values matching the number of fields of the
 * VariableParam, as in D1*D2.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class ValueMap extends BindMap
{
	private ArrayList<Double> mValues = new ArrayList<Double>();

	/**
	 * Creates a new Map and adds it to the parent Bind
	 * 
	 * @param parent
	 *            The Bind this Map belongs to
	 */
	public ValueMap(Bind parent)
	{
		super(parent);
	}

	public ValueMap(Bind parent, String key, double... values)
	{
		super(parent, key);
		for (double value : values)
			addValue(value);
	}

	/**
	 * Use this for REAL Params
	 * 
	 * @param index
	 *            0 is the first value etc.
	 * @return If the index out of bounds the result will be zero
	 */
	public double getRealValue(int index)
	{
		if ((index < 0) || (index >= mValues.size()))
			return 0.0;
		return mValues.get(index).doubleValue();
	}

	/**
	 * Use this for INT Params
	 * 
	 * @param index
	 *            0 is the first value etc.
	 * @return If the values are referenced, the result will be zero
	 */
	public int getIntValue(int index)
	{
		return (int) getRealValue(index);
	}

	/**
	 * Add a new value to the list.
	 * 
	 * @param value
	 *            The value to add.
	 */
	public void addValue(double value)
	{
		mValues.add(new Double(value));
	}

	/**
	 * @return The number of values being mapped
	 */
	public int getValueCount()
	{
		return mValues.size();
	}
}
