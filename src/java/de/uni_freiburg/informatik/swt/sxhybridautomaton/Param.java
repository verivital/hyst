package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import com.verivital.hyst.ir.AutomatonExportException;

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
		
		for (Param v : parent.getParams()) {
			if (v.mName == this.mName) {
				throw new AutomatonExportException("ERROR: tried to add duplicate parameter to automaton (parameter with same name already exists).");
			}
		}
		
		parent.addParam(this);
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
