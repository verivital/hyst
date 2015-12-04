/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

/**
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class VariableParam extends Param {
	private ParamType mType;
	private ParamDynamics mDynamics;
	private boolean mControlled;
	private int[] mConcreteDimensions = new int[2];
	private String[] mReferencedDimensions = new String[2];

	/**
	 * Creates a new Param and adds it to the parent Component
	 * @param parent The Component this Param belongs to
	 */
	public VariableParam(SpaceExComponent parent) {
		super(parent);
		
		setType(ParamType.REAL);
		setDynamics(ParamDynamics.ANY);
		setControlled(false);
		setDimensionSize(1, 1);
		setDimensionSize(2, 1);
	}
	
	public VariableParam(SpaceExComponent parent, String Name, ParamType type,
			ParamDynamics dynamics) {
		super(parent);
		setName(Name);
		setType(type);
		setDynamics(dynamics);
		setControlled(false);
		setDimensionSize(1, 1);
		setDimensionSize(2, 1);
	}
	
	public VariableParam(SpaceExComponent parent, String Name, ParamType type,
			ParamDynamics dynamics, boolean local) {
		super(parent);
		setName(Name);
		setType(type);
		setDynamics(dynamics);
		setLocal(local);
		setControlled(true);
		setDimensionSize(1, 1);
		setDimensionSize(2, 1);
	}
	
	@Override
	public ParamType getType() {
		return mType;
	}
	
	/**
	 * Set the type of the Param
	 * @param type Either INT or REAL. Do not use LABEL.
	 */
	public void setType(ParamType type) {
		if (type != ParamType.LABEL) mType = type;
	}
	
	public ParamDynamics getDynamics() {
		return mDynamics;
	}
	
	public void setDynamics(ParamDynamics dynamics) {
		mDynamics = dynamics;
	}

	public boolean getControlled() {
		return mControlled;
	}
	
	public void setControlled(Boolean controlled) {
		mControlled = controlled;
	}

	/**
	 * Check if a dimension's size is given by another Param
	 * @param dimension d1 (1) or d2 (2)
	 * @return true if the dimension size is given by another Param
	 */
	public boolean isDimensionSizeReferenced(int dimension) {
		if ((dimension < 1) || (dimension > mConcreteDimensions.length))
			return false;
		return mConcreteDimensions[dimension - 1] <= 0;
	}

	/**
	 * Get a dimension size IF it is not referenced
	 * @param dimension d1 (1) or d2 (2)
	 * @return The dimension size OR -1 if it is a referenced value
	 */
	public int getDimensionSize(int dimension) {
		if ((dimension < 1) || (dimension > mConcreteDimensions.length))
			return 0;
		if (!isDimensionSizeReferenced(dimension))
			return mConcreteDimensions[dimension - 1];
		return -1;
	}

	/**
	 * Get the name of the referenced Param IF there is one
	 * @param dimension d1 (1) or d2 (2)
	 * @return The name of said Param OR "" if there is no reference
	 */
	public String getDimensionSizeReference(int dimension) {
		if ((dimension < 1)
			|| (dimension > mReferencedDimensions.length)
			|| !isDimensionSizeReferenced(dimension))
			return "";
		return mReferencedDimensions[dimension - 1];
	}
	
	/**
	 * Set the size of a dimension to the given value
	 * @param dimension d1 (1) or d2 (2)
	 * @param size The new size. Should be 1 or larger
	 */
	public void setDimensionSize(int dimension, int size) {
		if ((dimension < 1) || (dimension > mConcreteDimensions.length))
			return;
		if (size < 1) {
			mConcreteDimensions[dimension - 1] = 1;
		} else {
			mConcreteDimensions[dimension - 1] = size;
		}
	}
	
	/**
	 * Set the size of a dimension as a reference to another Param
	 * @param dimension d1 (1) or d2 (2)
	 * @param sizeReference The Name of the Param which shall be referenced
	 */
	public void setDimensionSize(int dimension, String sizeReference) {
		if ((dimension < 1) || (dimension > mReferencedDimensions.length))
			return;
		mReferencedDimensions[dimension - 1] = sizeReference;
		mConcreteDimensions[dimension - 1] = 0;
	}
}
