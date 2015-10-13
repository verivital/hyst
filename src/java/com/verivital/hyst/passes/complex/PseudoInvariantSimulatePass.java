package com.verivital.hyst.passes.complex;

import java.util.Map;
import java.util.TreeSet;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.simulation.RungeKutta;
import com.verivital.hyst.simulation.Simulator;
import com.verivital.hyst.util.AutomatonUtil;


/**
 * This pass splits the initial mode into several using the technique of pseudo-invariants:
 * "Reducing the Wrapping Effect in Flowpipe Construction Using Pseudo-invariants", CyPhy 2014, Bak 2014
 * 
 * The parameter is a list of times. The center of the initial set is simulated for 
 * these times before splitting orthogonal to the gradient.
 * 
 * Currently, the simulatio is only done from the initial states, staying in the initial mode. If there's
 * a demand, this should be extended to work across discrete transitions.
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class PseudoInvariantSimulatePass extends TransformationPass
{
	BaseComponent ha;
	TreeSet <Double> simTimes;
	AutomatonMode mode;
	HyperPoint initPt;
	int numVars;
	
	@Override
	public String getCommandLineFlag()
	{
		return "-pass_pi_sim";
	}
	
	@Override
	public String getName()
	{
		return "Pseudo-Invariant Simulation Pass";
	}
	
	@Override
	public String getParamHelp()
	{
		return "[time1;time2;...]";
	}
	
	@Override
	protected void runPass(String params)
	{
		BaseComponent ha = (BaseComponent)config.root;
		initialize(ha, params);

		// construct the param string for the static-based pseudo-invariant pass
		String piParams = null;
		
		for (Double time : simTimes)
		{
			int NUM_STEPS = 500; // constant for now, might make it a param later
			
			double[] pt = Simulator.simulateFor(time, initPt, NUM_STEPS, mode.flowDynamics, ha.variables, null);

			Map<String, Expression> dy = Simulator.centerDynamics(mode.flowDynamics);
			double[] gradient = RungeKutta.getGradientAtPoint(dy, ha.variables, new HyperPoint(pt));
			double[] invariantDir = negate(gradient);
		
			if (piParams == null)
				piParams = "";
			else
				piParams += "|";
			
			piParams += commaSeparated(pt) + ";" + commaSeparated(invariantDir);
		}
		
		//System.out.println("params = " + piParams);
		//System.exit(1);
		
		// run the traditional pseudo-invariants pass
		new PseudoInvariantPass().runTransformationPass(config, piParams);
	}
	
	private double[] negate(double[] input)
	{
		double[] rv = new double[input.length];
		
		for (int i = 0; i < input.length; ++i)
			rv[i] = -input[i];
		
		return rv;
	}

	private static String commaSeparated(double[] pt)
	{
		String rv = "";
		
		for (int i = 0; i < pt.length; ++i)
		{
			if (rv.length() > 0)
				rv += ",";
			
			rv += pt[i];
		}
		
		return rv;
	}

	private void initialize(BaseComponent ha, String params)
	{
		String[] parts = params.split(",");
		
		if (parts.length < 1)
			throw new AutomatonExportException("Expected param with 'time1;time2;...'");
		
		simTimes = new TreeSet <Double>();
		
		for (int p = 0; p < parts.length; ++p)
		{
			try
			{
				double time = Double.parseDouble(parts[p]);
				
				if (time < 0)
					throw new AutomatonExportException("Pseudo-invariant time was negative: " + time);
				
				simTimes.add(time);
			}
			catch (NumberFormatException e)
			{
				throw new AutomatonExportException("Error parsing pseudo-invariant time: " + e);
			}
		}
		
		this.ha = ha;
		mode = ha.modes.get(config.init.keySet().iterator().next());
		numVars = ha.variables.size();

		initPt = AutomatonUtil.getInitialPoint(ha, config);
	}
}
