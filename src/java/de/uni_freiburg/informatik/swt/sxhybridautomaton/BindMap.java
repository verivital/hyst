package de.uni_freiburg.informatik.swt.sxhybridautomaton;

/**
 * Maps a Param to a value or a Param of a higher level Component
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 * 
 */
public abstract class BindMap
{
	private Bind mParent;
	private String mKey;
	
	/**
	 * Creates a new Map and adds it to the parent Bind
	 * @param parent The Bind this Map belongs to
	 */
	public BindMap(Bind parent) {
		setParent(parent);
		parent.addMap(this);
	}

	public BindMap(Bind parent, String key) {
		setParent(parent);
		setKey(key);
		parent.addMap(this); 
	}
	
	public String getKey() {
		return mKey;
	}
	
	public void setKey(String key) {
		mKey = key;
	}

	public Bind getParent() {
		return mParent;
	}

	public void setParent(Bind parent) {
		mParent = parent;
	}
}
