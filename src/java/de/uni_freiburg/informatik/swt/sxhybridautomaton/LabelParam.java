/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

/**
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class LabelParam extends Param {

	/**
	 * Creates a new Param and adds it to the parent Component
	 * @param parent The Component this Param belongs to
	 */
	public LabelParam(SpaceExComponent parent) {
		super(parent);
	}

	/**
	 * A label param is always of type LABEL
	 */
	@Override
	public ParamType getType() {
		return ParamType.LABEL;
	}

}
