/**
 * 
 */
package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import java.util.ArrayList;

import com.verivital.hyst.ir.AutomatonExportException;

/**
 * A Bind instantiates a subordinated automaton and binds its Params to values
 * or Params of the Component the Bind belongs to.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class Bind {
	private SpaceExNetworkComponent mParent;
	private String mComponent, mAs, mNote;
	private ArrayList<BindMap> mMaps = new ArrayList<BindMap>();
	private UIPosition mPosition;
	private UIDimensions mDimensions;
	
	public String toString()
	{
		return "[Bind component(type): " + mComponent + ", instanceName(as): " + mAs + "]";
	}
	
	/**
	 * Creates a new Bind and adds it to the parent Component
	 * @param parent The Component this Bind belongs to
	 */
	public Bind(SpaceExNetworkComponent parent) {
		mParent = parent;
		parent.addBind(this);
	}

	public Bind(SpaceExNetworkComponent parent, String component, String as,
			double x, double y, double w, double h) {
		mParent = parent;
		setComponent(component);
		setAs(as);
		setPosition(new UIPosition(x, y));
		setDimensions(new UIDimensions(w, h));
		parent.addBind(this);
	}
	
	/**
	 * A Bind's Component specifies which base automaton is instantiated.
	 * It references another Component.
	 * @return Component ID
	 */
	public String getComponent() {
		return mComponent;
	}

	/**
	 * A Bind's Component specifies which base automaton is instantiated
	 * It references another Component.
	 */
	public void setComponent(String component) {
		mComponent = component;
	}

	/**
	 * A Bind's As is a unique name given to it
	 * @return The Bind's name
	 */
	public String getAs() {
		return mAs;
	}

	/**
	 * A Bind's As is a unique name given to it
	 */
	public void setAs(String as) {
		mAs = as;
	}

	public String getNote() {
		return mNote;
	}

	public void setNote(String note) {
		mNote = note;
	}

	/**
	 * Get a map of this bind
	 * @param index The map's number, starting at 0
	 * @return The requested map or null if the index is out of bounds.
	 */
	public BindMap getMap(int index) {
		if ((index < 0) || (index >= mMaps.size())) return null;
		return mMaps.get(index);
	}
	
	public int getMapCount() {
		return mMaps.size();
	}

	/**
	 * Add a new map to this bind. The Bind becomes its parent.
	 * @param map The map to add.
	 */
	public void addMap(BindMap map) {
		map.setParent(this);
		
		for (BindMap bm : mMaps)
		{
			if (bm.getKey().equals(map.getKey()))
				throw new AutomatonExportException("Key was mapped to multiple values: '" + bm.getKey() 
						+ "' in instance " + getAs());
		}
		
		mMaps.add(map);
	}

	public UIPosition getPosition() {
		return mPosition;
	}

	public void setPosition(UIPosition position) {
		mPosition = position;
	}

	public UIDimensions getDimensions() {
		return mDimensions;
	}

	public void setDimensions(UIDimensions dimensions) {
		mDimensions = dimensions;
	}

	public SpaceExNetworkComponent getParent() {
		return mParent;
	}

	public void setParent(SpaceExNetworkComponent parent) {
		mParent = parent;
	}
}
