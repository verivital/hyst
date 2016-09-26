package com.verivital.hyst.passes.complex.pi;

import java.util.ArrayList;
import java.util.List;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.SymbolicStatePoint;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMixedTriggeredPass;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.StringOperations;

/**
 * This pass splits the initial mode into several using the technique of pseudo-invariants:
 * "Reducing the Wrapping Effect in Flowpipe Construction Using Pseudo-invariants" , CyPhy 2014, Bak
 * 2014
 * 
 * This pass only does a single splitting, on the initial mode, as soon as the constructed
 * hyperplane is completely on one side of the initial state box. The constructed hyperplane is
 * orthogonal to a simulation from the center of the initial states.
 * 
 * This is more light-weight than the other pseudo-invariant transformations as it doesn't require
 * modifying the initial states or adding an urgent timer.
 * 
 * @author Stanley Bak (Auhust 2016)
 *
 */
public class PseudoInvariantInitPass extends TransformationPass
{
	BaseComponent ha = null;

	@Override
	public String getCommandLineFlag()
	{
		return "pi_init";
	}

	@Override
	public String getName()
	{
		return "Pseudo-Invariant Near Initial States Pass";
	}

	@Override
	protected void runPass()
	{
		ha = (BaseComponent) config.root;

		HyperRectangle initBox = HybridizeMixedTriggeredPass.getInitialBox(config);
		final String initialMode = config.init.keySet().iterator().next();
		HyperPoint center = initBox.center();

		ArrayList<SymbolicStatePoint> startPoints = new ArrayList<SymbolicStatePoint>();
		startPoints.add(new SymbolicStatePoint(initialMode, center));

		double simTime = config.settings.spaceExConfig.timeHorizon;
		ArrayList<SymbolicStatePoint> trajectory = HybridizeMixedTriggeredPass
				.simMultiGetTrajectory(config, startPoints, simTime).get(0);

		SymbolicStatePoint piPoint = HybridizeMixedTriggeredPass.getPiPoint(ha, initBox,
				trajectory);

		if (piPoint == null)
			throw new AutomatonExportException(
					"Hyperplane from center trajectory never crossed past start box.");

		// make the hyperplane according to pi-point
		ArrayList<String> modes = new ArrayList<String>();
		ArrayList<HyperPoint> points = new ArrayList<HyperPoint>();
		ArrayList<HyperPoint> dirs = new ArrayList<HyperPoint>();

		double[] gradient = AutomatonUtil.getGradientAtPoint(ha.modes.get(piPoint.modeName),
				piPoint.hp);

		modes.add(initialMode);
		points.add(new HyperPoint(piPoint.hp));
		dirs.add(new HyperPoint(gradient));

		String paramString = PseudoInvariantPass.makeParamString(modes, points, dirs, true);
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
