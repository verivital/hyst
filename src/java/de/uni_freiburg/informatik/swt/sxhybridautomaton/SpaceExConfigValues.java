package de.uni_freiburg.informatik.swt.sxhybridautomaton;

import java.util.ArrayList;

public class SpaceExConfigValues
{
	public ArrayList<String> outputVars = new ArrayList<String>();
	public double timeHorizon = 10;
	public int maxIterations = 10;
	public double samplingTime = 0.1;
	public String systemID;
	public String outputFormat = "GEN";
	public String scenario = "stc";
	
	public SpaceExConfigValues copy()
	{
		SpaceExConfigValues rv = new SpaceExConfigValues();
		
		rv.outputVars.addAll(outputVars);
		rv.timeHorizon = timeHorizon;
		rv.maxIterations = maxIterations;
		rv.samplingTime = samplingTime;
		rv.systemID = systemID;
		rv.outputFormat = outputFormat;
		rv.scenario = scenario;
		
		return rv;
	}
}
