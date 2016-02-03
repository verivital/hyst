package com.verivital.hyst.passes.complex.pi;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.python.PythonBridge;
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
	@Option(name="-times", required=true, handler=StringArrayOptionHandler.class,usage="simulation times", metaVar="TIME1 TIME2 ...")
	List<String> timesStr;
	
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
	
	private ArrayList <Double> convertTimesStr()
	{
		ArrayList <Double> rv = new ArrayList <Double>(timesStr.size());
		double lastTime = 0;
		
		for (String time : timesStr)
		{
			try
			{
				double d = Double.parseDouble(time);
				
				if (d < lastTime)
					throw new AutomatonExportException("times should be greater than zero and in increasing order:" +
							d);
				
				rv.add(d);
			}
			catch (NumberFormatException e)
			{
				throw new AutomatonExportException("Error parsing pseudo-invariant time: " + e);
			}
		}
		
		return rv;
	}
	
	@Override
	protected void runPass()
	{
		BaseComponent ha = (BaseComponent)config.root;

		// construct the param string for the static-based pseudo-invariant pass
		String piParams = null;
		SymbolicStatePoint init = new SymbolicStatePoint();
		init.modeName = config.init.entrySet().iterator().next().getKey();
		init.hp = AutomatonUtil.getInitialPoint(ha, config);
		
		List <SymbolicStatePoint> states = pythonSimulate(config, init, convertTimesStr());
		
		for (SymbolicStatePoint ss : states)
		{
			AutomatonMode mode = ha.modes.get(ss.modeName);
			double[] gradient = AutomatonUtil.getGradientAtPoint(mode, ss.hp);
			double[] invariantDir = negate(gradient);
		
			if (piParams == null)
				piParams = "";
			else
				piParams += "|";
			
			piParams += ss.modeName + ";" + commaSeparated(ss.hp.dims) + ";" + commaSeparated(invariantDir);
		}
		
		// run the traditional pseudo-invariants pass
		System.out.println(". piParams = " + piParams);
		System.out.print("This is where we left off for this. We need to convert pi-pass to " +
				"use args4j, and maybe have a programatic (static) interface for it.");
		new PseudoInvariantPass().runTransformationPass(config, piParams);
	}

	/**
	 * Simulate the automaton, getting the state at a series of times
	 * @param automaton
	 * @param start the start state
	 * @param times the times where to return the state
	 * @return the state at each of the times
	 */
	public static List<SymbolicStatePoint> pythonSimulate(Configuration automaton, SymbolicStatePoint start, 
			List<Double> times)
	{
		if (start.hp.dims.length != automaton.root.variables.size())
			throw new AutomatonExportException("start point had " + start.hp.dims.length + 
					" dimensions; expected " + automaton.root.variables.size());
		
		PythonBridge pb = PythonBridge.getInstance();
		pb.send("from pythonbridge.pysim_utils import simulate_times");
		
		StringBuilder s = new StringBuilder();
		s.append(PySimPrinter.automatonToString(automaton));
		
		String point = "[" + commaSeparated(start.hp.dims) + "]";
		String timesStr = "(" + commaSeparated(times) +")";
		
		s.append("print simulate_times(define_ha(), '" + start.modeName + "', " 
				+ point + ", " + timesStr + ")");
		
		String result = pb.send(s.toString());
		
		// parse result into SymbolicState objects
		// result is semi-colon separated lists, first is the mode name, rest is the point 
		ArrayList <SymbolicStatePoint> rv = new ArrayList<SymbolicStatePoint>();
		
		for (String part : result.split(";"))
		{
			String[] comma_parts = part.split(",");
			
			String mode = comma_parts[0]; // first one is the mode
			HyperPoint pt = new HyperPoint(comma_parts.length - 1);
			
			for (int i = 1; i < comma_parts.length; ++i)
				pt.dims[i-1] = Double.parseDouble(comma_parts[i]);
			
			rv.add(new SymbolicStatePoint(mode, pt));
		}
		
		return rv;
	}

	private double[] negate(double[] input)
	{
		double[] rv = new double[input.length];
		
		for (int i = 0; i < input.length; ++i)
			rv[i] = -input[i];
		
		return rv;
	}

	private static String commaSeparated(List <Double> pt)
	{
		StringBuilder rv = new StringBuilder("");
		
		for (double d : pt)
		{
			if (rv.length() > 0)
				rv.append(",");
			
			rv.append(d);
		}
		
		return rv.toString();
	}
	
	private static String commaSeparated(double[] pt)
	{
		ArrayList <Double> a = new ArrayList<Double>(pt.length);
		
		for (double d : pt)
			a.add(d);
		
		return commaSeparated(a);
	}
}
