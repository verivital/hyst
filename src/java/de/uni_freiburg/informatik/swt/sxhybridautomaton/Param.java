package de.uni_freiburg.informatik.swt.sxhybridautomaton;

/**
 * Params are like variables of automatons which are connected to values by
 * Binds of a Component.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public abstract class Param {
	private SpaceExComponent mParent;
	private String mName, mNote;
	private boolean mLocal;

	/**
	 * Creates a new Param and adds it to the parent Component
	 * @param parent The Component this Param belongs to
	 */
	public Param(SpaceExComponent parent) {
		mParent = parent;
		setLocal(false);
		
		// check that no param already added has the same name
		// TODO: there are reasons we might want to disable this effectively static analysis check, e.g., for testing how
		// tools handle these slightly ill-formatted examples
		
		boolean duplicate = false;
		
		for (Param v : parent.getParams()) {
			if (v.mName == this.mName) {
				// TODO: throw appropriate error here if this is attempted instead of just printing
				System.err.println("ERROR: tried to add duplicate parameter to automaton (parameter with same name already exists). Ignoring this addition for now (throw error eventually).");
				duplicate = true;
			}
		}
		
		if (!duplicate) {
			parent.addParam(this);
		}
	}

	public String getName() {
		return mName;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public String getNote() {
		return mNote;
	}
	
	public void setNote(String note) {
		mNote = note;
	}
	
	public boolean getLocal() {
		return mLocal;
	}
	
	public void setLocal(Boolean local) {
		mLocal = local;
	}

	public SpaceExComponent getParent() {
		return mParent;
	}

	public void setParent(SpaceExComponent parent) {
		mParent = parent;
	}
	
	public abstract ParamType getType();
}
