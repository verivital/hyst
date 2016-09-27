package com.verivital.hyst.passes.complex.pi;

import java.util.ArrayList;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.SymbolicStatePoint;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMixedTriggeredPass;
import com.verivital.hyst.util.AutomatonUtil;

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
	@Option(name = "-skip_error", usage = "skip the pass if invariant errors occur (rather than raising an exception)")
	public boolean skipError = false;

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
		{
			if (!skipError)
				throw new AutomatonExportException(
						"Hyperplane from center trajectory never crossed past start box.");
			else
				Hyst.log("Hyperplane from center trajectory never crossed past start box. "
						+ "-skip_error was set skipping pass");
		}
		else
		{
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
	}
}
