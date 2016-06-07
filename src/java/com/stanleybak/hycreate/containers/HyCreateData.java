package com.stanleybak.hycreate.containers;

import java.util.ArrayList;
import java.util.TreeMap;

public class HyCreateData
{
	private String automatonName = "unnamed";
	private String dimensions = "x, y";
	private String initialStates = "0,0;0,0";

	private String selectedMode = null;
	private TreeMap<String, ModeData> modes = new TreeMap<String, ModeData>();

	private int selectedTransition = -1;
	private ArrayList<TransitionData> transitions = new ArrayList<TransitionData>();

	private String versionString = "File Version 1";

	public static final String DEFAULT_GLOBAL = "/* define global values here */";
	private String globalText = DEFAULT_GLOBAL;

	private ModelOptions options = new ModelOptions();

	public HyCreateData()
	{

	}

	public HyCreateData(String versionString)
	{
		this.setVersionString(versionString);
	}

	public String getInitialStates()
	{
		return initialStates;
	}

	public void setInitialStates(String initialStates)
	{
		this.initialStates = initialStates;
	}

	public String getSelectedMode()
	{
		return selectedMode;
	}

	public void setSelectedMode(String selectedMode)
	{
		this.selectedMode = selectedMode;
	}

	public int getSelectedTransition()
	{
		return selectedTransition;
	}

	public void setSelectedTransition(int selectedTransition)
	{
		this.selectedTransition = selectedTransition;
	}

	public String getAutomatonName()
	{
		return automatonName;
	}

	public void setAutomatonName(String automatonName)
	{
		this.automatonName = automatonName;
	}

	public String getDimensions()
	{
		return dimensions;
	}

	public void setDimensions(String dimensions)
	{
		this.dimensions = dimensions;
	}

	public TreeMap<String, ModeData> getModes()
	{
		return modes;
	}

	public void setModes(TreeMap<String, ModeData> modes)
	{
		this.modes = modes;
	}

	public ArrayList<TransitionData> getTransitions()
	{
		return transitions;
	}

	public void setTransitions(ArrayList<TransitionData> transitions)
	{
		this.transitions = transitions;
	}

	public void setVersionString(String versionString)
	{
		this.versionString = versionString;
	}

	public String getVersionString()
	{
		return versionString;
	}

	public ModelOptions getOptions()
	{
		return options;
	}

	public void setOptions(ModelOptions options)
	{
		this.options = options;
	}

	public String getGlobalText()
	{
		return globalText;
	}

	public void setGlobalText(String globalText)
	{
		this.globalText = globalText;
	}
}
