package com.verivital.hyst.passes.complex.pi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.SymbolicStatePoint;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.DoubleArrayOptionHandler;
import com.verivital.hyst.util.StringOperations;

/**
 * This pass splits the initial mode into several using the technique of pseudo-invariants:
 * "Reducing the Wrapping Effect in Flowpipe Construction Using Pseudo-invariants" , CyPhy 2014, Bak
 * 2014
 * 
 * The parameter is a list of times. The center of the initial set is simulated for these times
 * before splitting orthogonal to the gradient.
 * 
 * Currently, the simulatio is only done from the initial states, staying in the initial mode. If
 * there's a demand, this should be extended to work across discrete transitions.
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class PseudoInvariantSimulatePass extends TransformationPass
{
	@Option(name = "-times", required = true, handler = DoubleArrayOptionHandler.class, usage = "simulation times", metaVar = "TIME1 TIME2 ...")
	private List<Double> times;

	@Override
	public String getCommandLineFlag()
	{
		return "pi_sim";
	}

	@Override
	public String getName()
	{
		return "Pseudo-Invariant Simulation Pass";
	}

	@Override
	protected void runPass()
	{
		Collections.sort(times);
		Hyst.log("Simulation times: " + times);
		BaseComponent ha = (BaseComponent) config.root;

		SymbolicStatePoint init = new SymbolicStatePoint();
		init.modeName = config.init.entrySet().iterator().next().getKey();
		init.hp = AutomatonUtil.getInitialPoint(ha, config);
		List<SymbolicStatePoint> states = pythonSimulate(config, init, times);

		List<String> modes = new ArrayList<String>(times.size());
		List<HyperPoint> points = new ArrayList<HyperPoint>(times.size());
		List<HyperPoint> dirs = new ArrayList<HyperPoint>(times.size());

		for (SymbolicStatePoint ss : states)
		{
			AutomatonMode mode = ha.modes.get(ss.modeName);
			double[] gradient = AutomatonUtil.getGradientAtPoint(mode, ss.hp);

			modes.add(mode.name);
			points.add(new HyperPoint(ss.hp));
			dirs.add(new HyperPoint(gradient));
		}

		String paramString = PseudoInvariantPass.makeParamString(modes, points, dirs, false);
		Hyst.log("Calling hyperplane pseudo-invariant pass with params: " + paramString);

		// run the traditional pseudo-invariants pass
		new PseudoInvariantPass().runTransformationPass(config, paramString);
	}

	/**
	 * Simulate the automaton, getting the state at a series of times
	 * 
	 * @param automaton
	 * @param start
	 *            the start state
	 * @param times
	 *            the times where to return the state
	 * @return the state at each of the times
	 */
	public static List<SymbolicStatePoint> pythonSimulate(Configuration automaton,
			SymbolicStatePoint start, List<Double> times)
	{
		if (start.hp.dims.length != automaton.root.variables.size())
			throw new AutomatonExportException("start point had " + start.hp.dims.length
					+ " dimensions; expected " + automaton.root.variables.size());

		PythonBridge pb = PythonBridge.getInstance();
		pb.send("from pythonbridge.pysim_utils import simulate_times");

		StringBuilder s = new StringBuilder();
		s.append(PySimPrinter.automatonToString(automaton));

		String point = "[" + StringOperations.join(",", start.hp.dims) + "]";
		String timesStr = "[" + StringOperations.join(",", times.toArray(new Double[0])) + "]";

		s.append("print simulate_times(define_ha(), '" + start.modeName + "', " + point + ", "
				+ timesStr + ")");

		String result = pb.send(s.toString());

		// parse result into SymbolicState objects
		// result is semi-colon separated lists, first is the mode name, rest is
		// the point
		ArrayList<SymbolicStatePoint> rv = new ArrayList<SymbolicStatePoint>();

		for (String part : result.split(";"))
		{
			String[] comma_parts = part.split(",");

			String mode = comma_parts[0]; // first one is the mode
			HyperPoint pt = new HyperPoint(comma_parts.length - 1);

			for (int i = 1; i < comma_parts.length; ++i)
				pt.dims[i - 1] = Double.parseDouble(comma_parts[i]);

			rv.add(new SymbolicStatePoint(mode, pt));
		}

		return rv;
	}

	public static String makeParamString(double... times)
	{
		StringBuilder rv = new StringBuilder();
		rv.append("-times");

		for (double t : times)
			rv.append(" " + t);

		return rv.toString();
	}
}
