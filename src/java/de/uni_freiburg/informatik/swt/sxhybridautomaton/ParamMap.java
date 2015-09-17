/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

/**
 * Maps a Param of the bound Component to a Param of the Bind's parent.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class ParamMap extends BindMap {
	private String mParamReference;

	/**
	 * Creates a new Map and adds it to the parent Bind
	 * @param parent The Bind this Map belongs to
	 */
	public ParamMap(Bind parent) {
		super(parent);
	}
	
	public ParamMap(Bind parent, String key, String reference) {
		super(parent, key);
		setParamReference(reference);
	}

	public String getParamReference() {
		return mParamReference;
	}

	public void setParamReference(String paramReference) {
		mParamReference = paramReference;
	}
}
