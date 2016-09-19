package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.HyperRectangleCornerEnumerator;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.geometry.SymbolicStatePoint;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMTRawPass.SpaceSplittingElement;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMTRawPass.SplittingElement;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMTRawPass.TimeSplittingElement;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

/**
 * This is the simulation-based mixed-triggered hybridization pass. Its implementation matches with
 * the HSCC paper. It uses simulation to derive the hybridization parameters. After which, it calls
 * the HybridizeMTRawPass to actually do the hybridization.
 * 
 * The parameters of the pass are:
 * 
 * The maximum time, T
 * 
 * The simulation strategy, S, one of {center|star|corners|starcorners|rand10|rand#}
 * 
 * The time-triggered timestep, delta_tt
 * 
 * The number of space-triggered transitions, n_pi
 * 
 * The maximum simulation time before giving up on a space-triggered step, delta_pi
 * 
 * The bloating term, epsilon
 * 
 * @author Stanley Bak
 *
 */
public class HybridizeMixedTriggeredPass extends TransformationPass
{
	@Option(name = "-T", required = true, aliases = {
			"-maxtime" }, usage = "The simulation time", metaVar = "VAL")
	double timeMax;

	// The simulation strategy, S, one of
	// {center|star|corners|starcorners|rand10|rand#}
	@Option(name = "-S", aliases = {
			"-simstrat" }, usage = "The simulation strategy, S, one of {center|star|corners|starcorners|rand10|rand#}", metaVar = "SIMTYPE")
	String simTypeString = "center";

	// The time-triggered time step, delta_tt
	@Option(name = "-delta_tt", required = true, usage = "The time-triggered time step", metaVar = "TIME")
	double timeStep;

	// The number of space-triggered transitions, n_pi
	@Option(name = "-n_pi", usage = "The number of space-triggered transitions", metaVar = "NUM")
	int piCount = 0;

	// The maximum simulation time before giving up on a space-triggered step,
	// delta_pi
	@Option(name = "-delta_pi", usage = "The maximum simulation time before giving up on a space-triggered step", metaVar = "TIME")
	double piMaxTime = 0;

	// The bloating term, epsilon
	@Option(name = "-epsilon", usage = "The bloating term for each constructed domain", metaVar = "VAL")
	double epsilon;

	// O or optimization
	@Option(name = "-O", aliases = {
			"-opt" }, usage = "the optimization method, one of {basinhopping, kodiak, interval, interval#, "
					+ "where # is the max error, like 0.1}", metaVar = "METHOD")
	String opt = "basinhopping";

	// no error
	@Option(name = "-noerror", usage = "do not insert the forbidden DCEM mode (useful for plotting)")
	boolean noError = false;

	// derived params
	SimulationType simType = SimulationType.CENTER;
	int randCount = -1; // for SimulationType.RAND

	enum SimulationType
	{
		CENTER, // simulate from the center
		STAR, // simulate from 2*n+1 points (center point as well as limits in
				// each dimension),
		CORNERS, // simulate from n^2+1 points (center point as well as
					// corners),
		STARCORNERS, // both STAR and CORNERS
		RAND, // random points on boundary
	}

	// This is the output of the pass, used to run the raw hybridization pass
	List<SplittingElement> splitElements = new ArrayList<SplittingElement>();
	List<HyperRectangle> domains = new ArrayList<HyperRectangle>();

	// other global-like values
	BaseComponent ha = null;

	// unit testing functions
	public TestFunctions testFuncs = null;

	public interface TestFunctions
	{
		public void piSimPointsReached(List<SymbolicStatePoint> simPoints);

		public void initialSimPoints(List<HyperPoint> simPoints);

		public void piSucceeded(boolean rv);
	}

	@Override
	public String getName()
	{
		return "Mixed-Triggered Hybridization Pass";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "hybridizemt";
	}

	@Override
	protected void checkPreconditons(Configuration c, String name)
	{
		super.checkPreconditons(c, name);

		if (!PythonBridge.hasPython())
			throw new PreconditionsFailedException(
					"Python (and required libraries) needed to run Hybridize Mixed Triggered pass.");
	}

	public static String makeParamString(double T, String simType, double delta_tt, int n_pi,
			double delta_pi, double epsilon, String optType, boolean noError)
	{
		StringBuilder s = new StringBuilder();

		s.append("-T " + T);
		s.append(" -S " + simType);
		s.append(" -delta_tt " + delta_tt);
		s.append(" -n_pi " + n_pi);
		s.append(" -delta_pi " + delta_pi);
		s.append(" -epsilon " + epsilon);
		s.append(" -opt " + optType);

		if (noError)
			s.append(" -noerror");

		return s.toString();
	}

	private void makeParams()
	{
		if (simTypeString.equals("center"))
			simType = SimulationType.CENTER;
		else if (simTypeString.equals("star"))
			simType = SimulationType.STAR;
		else if (simTypeString.equals("corners"))
			simType = SimulationType.CORNERS;
		else if (simTypeString.equals("starcorners"))
			simType = SimulationType.STARCORNERS;
		else if (simTypeString.startsWith("rand") && simTypeString.length() > 4)
		{
			simType = SimulationType.RAND;
			randCount = Integer.parseInt(simTypeString.substring(4));

			Hyst.log("Using " + randCount + " random simulations on boundary");
		}
		else
			throw new AutomatonExportException(
					"Unknown simulation type parameter: " + simTypeString);

		if (piMaxTime <= 0)
		{
			piMaxTime = 4 * timeStep;
			Hyst.log("Using delta_pi = " + piMaxTime);
		}
	}

	@Override
	protected void runPass()
	{
		ha = (BaseComponent) config.root;
		makeParams();

		long start = System.currentTimeMillis();
		simulateAndConstruct();

		long middle = System.currentTimeMillis();
		long simMills = middle - start;
		Hyst.log("Simulate Runtime: " + simMills + "ms");

		String params = HybridizeMTRawPass.makeParamString(splitElements, domains, opt, null,
				noError);

		long end = System.currentTimeMillis();
		long optimizeMills = end - middle;
		Hyst.log("Optimize and Construct Runtime: " + optimizeMills + "ms");

		new HybridizeMTRawPass().runVanillaPass(config, params);
	}

	/**
	 * Get the initial set of states as a HyperRectangle.
	 * 
	 * @return the initial set of states
	 * @throws AutomatonExportException
	 *             if the initial set of states is not a box
	 */
	public static HyperRectangle getInitialBox(Configuration config)
	{
		ArrayList<String> variables = config.root.variables;

		int numDims = variables.size();
		HyperRectangle rv = new HyperRectangle(numDims);

		// start in the middle of the initial state set
		TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();

		if (config.init.size() != 1)
			throw new AutomatonExportException("Expected single initial mode");

		try
		{
			RangeExtractor.getVariableRanges(config.init.values().iterator().next(), ranges);
		}
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException(
					"Could not determine ranges for inital values (not rectangluar initial states).",
					e);
		}
		catch (ConstantMismatchException e)
		{
			throw new AutomatonExportException("Constant mismatch in initial values.", e);
		}
		catch (UnsupportedConditionException e)
		{
			throw new AutomatonExportException("Initial values were not a box", e);
		}

		int numVars = variables.size();

		for (int i = 0; i < numVars; ++i)
		{
			String var = variables.get(i);

			Interval dimRange = ranges.get(var);

			if (dimRange == null)
				throw new AutomatonExportException(
						"Range for '" + var + "' was not set (not rectangluar initial states).");
			else
				rv.dims[i] = dimRange;
		}

		return rv;
	}

	/**
	 * Gets the start of the simulation, depending on the simType parameter
	 * 
	 * @param initBox
	 *            the initial box of states (from getInitialBox())
	 * @return a set of points
	 */
	private ArrayList<SymbolicStatePoint> getSimulationStart()
	{
		HyperRectangle initBox = getInitialBox(config);
		final String initialMode = config.init.keySet().iterator().next();

		HyperPoint center = initBox.center();

		final ArrayList<SymbolicStatePoint> rv = new ArrayList<SymbolicStatePoint>();
		rv.add(new SymbolicStatePoint(initialMode, center)); // all sim types
																// include
																// center

		if (simType == SimulationType.STAR || simType == SimulationType.STARCORNERS)
		{
			for (HyperPoint hp : initBox.getStarPoints())
			{
				if (!hp.equals(center))
					rv.add(new SymbolicStatePoint(initialMode, hp));
			}
		}

		if (simType == SimulationType.CORNERS || simType == SimulationType.STARCORNERS)
		{
			initBox.enumerateCornersUnique(new HyperRectangleCornerEnumerator()
			{
				@Override
				protected void enumerate(HyperPoint p)
				{
					rv.add(new SymbolicStatePoint(initialMode, p));
				}
			});
		}

		if (simType == SimulationType.RAND)
		{
			Random rand = new Random(0); // use constant seed for
											// reproducibility
			int numDims = ha.variables.size();

			for (int i = 0; i < randCount; ++i)
			{
				HyperPoint hp = new HyperPoint(numDims);

				// on the boundary means that one of the variables is forced to
				// be at the minimum or maximum
				int boundaryVar = rand.nextInt(numDims - 1); // subtract one for
																// the time
																// triggered
																// variable

				// if (boundaryVar >= ha.variables.indexOf(ttVarIndex))
				// ++boundaryVar;

				for (int d = 0; d < numDims; ++d)
				{
					Interval dimInterval = initBox.dims[d];

					if (d == boundaryVar)
					{
						if (rand.nextBoolean())
							hp.dims[d] = dimInterval.max;
						else
							hp.dims[d] = dimInterval.min;
					}
					else
						hp.dims[d] = dimInterval.min + rand.nextDouble() * dimInterval.width();
				}

				rv.add(new SymbolicStatePoint(initialMode, hp));
			}
		}

		return rv;
	}

	/**
	 * Generate the simulation parameters
	 */
	private void simulateAndConstruct()
	{
		// simulate from initial state
		ArrayList<SymbolicStatePoint> simPoints = getSimulationStart();
		checkValidStartPoints(simPoints);

		Hyst.log("Initial simulation points (" + simPoints.size() + "): " + simPoints);

		PythonBridge.getInstance().setTimeout(-1);

		// run simulation with the given params
		runSimulation(simPoints);
	}

	/**
	 * Actually run the simulation. This is the high-level algorithm described in the paper
	 */
	private void runSimulation(ArrayList<SymbolicStatePoint> simPoints)
	{
		double TOL = 1e-12;
		double elapsed = 0;

		// pseudo-invariants (space-triggered transitions)
		double piStepTime = -1;
		double piNextTime = -1;

		if (piCount > 0)
		{
			piStepTime = timeMax / piCount;
			piNextTime = 0;
		}

		int step = 0;

		while (elapsed + TOL < timeMax)
		{
			++step;

			HyperRectangle simBox = boundingBox(points(simPoints));
			Hyst.logDebug("simulation bounding box at step " + step + " was " + simBox
					+ "; points were: " + simPoints);

			if (piNextTime >= 0 && elapsed + TOL > piNextTime)
			{
				// try a pseudo invariant step (no time elapse)
				piNextTime += piStepTime;
				HyperRectangle startBox = HyperRectangle.bloatAdditive(simBox, epsilon);

				if (advanceSimulationToPseudoInvariant(startBox, simPoints))
				{
					Hyst.log("Doing pseudo-invariant step at sim-time: " + elapsed);
					stepSpaceTrigger(startBox, simPoints);
					continue;
				}
				else
					Hyst.log("Skipping pseudo-invariant step at sim-time: " + elapsed);
			}

			// didn't do a pi-step, instead do a time-triggered step
			stepTimeTrigger(simPoints);
			elapsed += timeStep;
		}
	}

	/**
	 * Add a space-triggered mode using an auxiliary hyperplane (pseudo-invariant). There is no
	 * guarantee of time elapsing in the constructed mode. This should be called after the simPoints
	 * have already advanced onto the plane. The first simPoint is the one used to construct the
	 * hyperplane.
	 * 
	 * @param startBox
	 *            the bloated box surrounding the simPoints before they were advanced (the incoming
	 *            set)
	 * @param simPoints
	 *            the (already-advanced) simulation points. First is center point
	 */
	private void stepSpaceTrigger(HyperRectangle startBox, ArrayList<SymbolicStatePoint> simPoints)
	{
		HyperRectangle endBox = HyperRectangle.bloatAdditive(boundingBox(points(simPoints)),
				epsilon);
		HyperRectangle invariantBox = HyperRectangle.union(startBox, endBox);

		Hyst.logDebug("making space-triggered mode, startBox was " + startBox + "; " + "endbox was "
				+ endBox);

		domains.add(invariantBox);

		SymbolicStatePoint piPoint = simPoints.get(0);
		double[] piGradient = gradient(piPoint);

		splitElements.add(new SpaceSplittingElement(piPoint.hp, piGradient));
	}

	/**
	 * A space triggered-transition (pseudo-invariant) is being constructed. Advance the center
	 * point until all the corners of the start box are all one side. This can fail if the
	 * trajectory never meets this condition (piMaxTime is exceeded)
	 * 
	 * @param startBox
	 *            the start box
	 * @param centerTrajectory
	 *            the center point
	 * @return the discovered pi point, or null if failed
	 */
	public static SymbolicStatePoint getPiPoint(BaseComponent ha, HyperRectangle startBox,
			ArrayList<SymbolicStatePoint> centerTrajectory)
	{
		// the first point of simPoints is the center point we should simulate
		SymbolicStatePoint rv = null;

		// simulate up to piMaxTime, looking for a state where all the corners
		// of startBox
		// are on one side of p
		for (SymbolicStatePoint p : centerTrajectory)
		{
			HyperPoint hp = p.hp;
			AutomatonMode am = ha.modes.get(p.modeName);

			if (testHyperPlane(hp, startBox, am))
			{
				Hyst.log("Found pi point: " + p + " with gradient "
						+ Arrays.toString(gradient(hp, am)));
				rv = p;
				break;
			}
		}

		return rv;
	}

	/**
	 * Advance the simulated points for a pseudo-invariant step. This can fail if if the center
	 * point's simulation cannot go to a point where the constructed auxiliary hyperplane would be
	 * entirely in front of the startBox (within time piMaxTime), or if some of the simulated points
	 * never reach the decided-upon hyperplane (within time piMaxTime).
	 * 
	 * @param startBox
	 *            the incoming set of states
	 * @return true if succeeded (and simPoints is advanced in place), false otherwise (simPoints is
	 *         unmodified)
	 */
	private boolean advanceSimulationToPseudoInvariant(HyperRectangle startBox,
			ArrayList<SymbolicStatePoint> simPoints)
	{
		// first, get the trajectories for all the simPoints up piMaxtime
		ArrayList<ArrayList<SymbolicStatePoint>> trajectories = simMultiGetTrajectory(config,
				simPoints, piMaxTime);

		boolean rv = false;
		SymbolicStatePoint piPoint = getPiPoint(ha, startBox, trajectories.get(0));

		if (piPoint != null)
		{
			ArrayList<SymbolicStatePoint> newSimPoints = new ArrayList<SymbolicStatePoint>();
			newSimPoints.add(piPoint);

			double piGradient[] = gradient(piPoint);
			double piVal = dotProduct(piGradient, piPoint.hp);
			boolean quitEarly = false;

			// advance every simulation until it exceeds piVal
			for (int i = 1; i < simPoints.size(); ++i)
			{
				double prevVal = dotProduct(piGradient, simPoints.get(i).hp);

				if (prevVal > piVal)
					throw new AutomatonExportException(
							"While constructing PI, initial sim-point was on incorrect side of pi hyperplane (shouldn't occur)");

				// when we cross, we will interpolate between the straddling
				// simulation points
				SymbolicStatePoint prevPoint = simPoints.get(i);

				// simulate up to 2*piMaxTime
				for (SymbolicStatePoint p : trajectories.get(i))
				{
					// check if p crossed the hyperplane
					double val = dotProduct(piGradient, p.hp);

					if (val >= piVal)
					{
						// if prevPoint and p are in different mode, probably a
						// reset was used
						// this is a BAD case of pseudo-invariants, so we're
						// better off failing
						if (!p.modeName.equals(prevPoint.modeName))
							throw new AutomatonExportException("When detecting crossing of "
									+ "space-triggered boundary, different modes detected (was a "
									+ "reset used? This would be a BAD case for space-triggered"
									+ "contruction.");

						// it crossed! take the fraction
						double difVal = val - prevVal; // for example 3.0
														// difference in val
														// between steps
						double fracVal = piVal - prevVal; // for example 1.0
															// difference
															// between piVal and
															// previous step
						double frac = fracVal / difVal; // should be 1/3

						// now the point we want is prevPoint + 1/3 * (curPoint
						// - prevPoint)
						HyperPoint vector = HyperPoint.subtract(p.hp, prevPoint.hp);
						HyperPoint fracVector = HyperPoint.multiply(vector, frac);
						HyperPoint newPoint = HyperPoint.add(prevPoint.hp, fracVector);

						newSimPoints.add(new SymbolicStatePoint(p.modeName, newPoint));

						break;
					}

					prevVal = val;
					prevPoint = p;
				}

				if (quitEarly)
					break;
			}

			if (newSimPoints.size() == simPoints.size()) // every simulation
															// point intersected
															// with the
															// pi-hyperplane
			{
				simPoints.clear();
				simPoints.addAll(newSimPoints);
				rv = true;
			}
			else
				Hyst.log(
						"Pseudo-invariant construction failed because some simpoints didn't cross the hyperplane within piMaxTime");
		}
		else
			Hyst.log(
					"Pseudo-invariant construction failed because simulating center point didn't created valid PI within piMaxTime");

		if (testFuncs != null)
		{
			testFuncs.piSimPointsReached(simPoints);
			testFuncs.piSucceeded(rv);
		}

		return rv;
	}

	/**
	 * create one time-triggered mode. This advances the simulation points in the passed-in array,
	 * as well as updating the global splitelements and domains
	 * 
	 * @param simPoints
	 * @return
	 */
	private void stepTimeTrigger(ArrayList<SymbolicStatePoint> simPoints)
	{
		HyperRectangle simBox = boundingBox(points(simPoints));
		HyperRectangle startBox = HyperRectangle.bloatAdditive(simBox, epsilon);

		ArrayList<SymbolicStatePoint> newSimPoints = simAllPoints(config, simPoints, timeStep);
		simPoints.clear();
		simPoints.addAll(newSimPoints);

		// a time-triggered transition should occur here
		HyperRectangle endBox = HyperRectangle.bloatAdditive(boundingBox(points(simPoints)),
				epsilon);
		HyperRectangle invariantBox = HyperRectangle.union(startBox, endBox);

		domains.add(invariantBox);
		splitElements.add(new TimeSplittingElement(timeStep));
	}

	private void checkValidStartPoints(ArrayList<SymbolicStatePoint> simPoints)
	{
		HyperRectangle startBox = HyperRectangle.bloatAdditive(boundingBox(points(simPoints)),
				epsilon);
		TreeMap<String, Interval> bounds = RangeExtractor
				.getVariableRanges(config.init.values().iterator().next(), "initial states");

		for (Entry<String, Interval> e : bounds.entrySet())
		{
			String var = e.getKey();
			Interval i = e.getValue();
			int index = ha.variables.indexOf(var);

			if (i.min <= startBox.dims[index].min || i.max >= startBox.dims[index].max)
			{
				throw new AutomatonExportException(
						"Bloated initial bounding box of simulations does not contain initial states for variable "
								+ var + ". Consider increasing epsilon.");
			}
		}

		if (testFuncs != null)
			testFuncs.initialSimPoints(points(simPoints));
	}

	private static ArrayList<HyperPoint> points(ArrayList<SymbolicStatePoint> simPoints)
	{
		ArrayList<HyperPoint> rv = new ArrayList<HyperPoint>(simPoints.size());

		for (SymbolicStatePoint ssp : simPoints)
			rv.add(ssp.hp);

		return rv;
	}

	/**
	 * Get the bounding box of two sets of points
	 * 
	 * @param setA
	 *            one of the sets
	 * @param setB
	 *            the other set
	 * @return a hyperrectngle which tightly includes all the points
	 */
	@SafeVarargs
	private static HyperRectangle boundingBox(ArrayList<HyperPoint>... sets)
	{
		HyperPoint firstPoint = sets[0].get(0);
		int numDims = firstPoint.dims.length;
		HyperRectangle rv = firstPoint.toHyperRectangle();

		for (int d = 0; d < numDims; ++d)
		{
			for (int list = 0; list < sets.length; ++list)
			{
				for (HyperPoint hp : sets[list])
					rv.dims[d].expand(hp.dims[d]);
			}
		}

		return rv;
	}

	private double[] gradient(SymbolicStatePoint ssp)
	{
		HyperPoint hp = ssp.hp;
		AutomatonMode am = ha.modes.get(ssp.modeName);

		return gradient(hp, am);
	}

	private static double[] gradient(HyperPoint hp, AutomatonMode am)
	{
		double[] rv = new double[am.automaton.variables.size()];

		for (int vIndex = 0; vIndex < am.automaton.variables.size(); ++vIndex)
		{
			String var = am.automaton.variables.get(vIndex);
			Expression e = am.flowDynamics.get(var).getExpression();

			rv[vIndex] = AutomatonUtil.evaluateExpression(e, hp, am.automaton.variables);
		}

		return rv;
	}

	/**
	 * Evaluate a dot product of a gradient and a hyperpoint
	 * 
	 * @param gradient
	 * @param p
	 * @return the cross product
	 */
	private static double dotProduct(double[] gradient, HyperPoint p)
	{
		if (gradient.length != p.dims.length)
			throw new RuntimeException(
					"eval gradient requires gradient and point have same number of dimensions");

		double rv = 0;

		for (int d = 0; d < gradient.length; ++d)
			rv += gradient[d] * p.dims[d];

		return rv;
	}

	/**
	 * Do a simulation of the passed-in point list
	 * 
	 * @param c
	 *            the configuration
	 * @param simPoints
	 *            the list of points
	 * @param time
	 *            the time to run the simulation
	 * @return the resultant points
	 */
	public static ArrayList<SymbolicStatePoint> simAllPoints(Configuration config,
			ArrayList<SymbolicStatePoint> simPoints, double time)
	{
		for (SymbolicStatePoint ssp : simPoints)
		{
			if (ssp.hp.dims.length != config.root.variables.size())
				throw new AutomatonExportException("start point had " + ssp.hp.dims.length
						+ " dimensions; expected " + config.root.variables.size());
		}

		PythonBridge pb = PythonBridge.getInstance();
		pb.send("from pythonbridge.pysim_utils import simulate_set_time");

		StringBuilder s = new StringBuilder();
		s.append(PySimPrinter.automatonToString(config));

		String points = makePointsString(simPoints);
		String modes = makeModeString(simPoints);

		s.append("print simulate_set_time(define_ha(), " + modes + ", " + points + ", " + time
				+ ")");

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

	private static String makeModeString(ArrayList<SymbolicStatePoint> simPoints)
	{
		StringBuilder rv = new StringBuilder();
		rv.append("[");
		boolean first = true;

		for (SymbolicStatePoint s : simPoints)
		{
			if (first)
				first = false;
			else
				rv.append(",");

			rv.append("'" + s.modeName + "'");
		}

		rv.append("]");

		return rv.toString();
	}

	private static String makePointsString(ArrayList<SymbolicStatePoint> simPoints)
	{
		StringBuilder rv = new StringBuilder();
		rv.append("[");
		boolean first = true;

		for (SymbolicStatePoint s : simPoints)
		{
			if (first)
				first = false;
			else
				rv.append(",");

			rv.append(makeHpString(s.hp));
		}

		rv.append("]");
		return rv.toString();
	}

	/**
	 * Test if all the points of box are on one side of a hyperplane derived from the given
	 * simulation point
	 * 
	 * @param simPoint
	 *            the simulation point
	 * @param box
	 *            the box to test against
	 * @return true if the box point are all behind the hyperplane
	 */
	public static boolean testHyperPlane(HyperPoint simPoint, HyperRectangle box, AutomatonMode am)
	{
		List<String> varNames = am.automaton.variables;

		if (simPoint.dims.length != box.dims.length)
			throw new RuntimeException("simpoint numdims must be same as box numdims");

		if (am.flowDynamics.size() != varNames.size())
			throw new RuntimeException("varnames size must be same as dynamics size");

		if (simPoint.dims.length != varNames.size())
			throw new RuntimeException("simpoint numdims must be same varNames size");

		double[] gradient = gradient(simPoint, am);
		double val = dotProduct(gradient, simPoint);

		double maxVal = 0;

		for (int d = 0; d < gradient.length; ++d)
		{
			double factor = gradient[d];

			if (factor < 0)
				maxVal += box.dims[d].min * factor;
			else
				maxVal += box.dims[d].max * factor;
		}

		return val > maxVal;
	}

	private static String makeHpString(HyperPoint hp)
	{
		StringBuilder rv = new StringBuilder();
		rv.append("[");
		boolean first = true;

		for (double d : hp.dims)
		{
			if (first)
				first = false;
			else
				rv.append(",");

			rv.append(d);
		}

		rv.append("]");

		return rv.toString();
	}

	/**
	 * Simulate from multiple points, returning the trajectories
	 * 
	 * @param config
	 *            the automaton
	 * @param startPoints
	 *            the points where each simulation starts
	 * @param time
	 *            the desired simulation time
	 * @return the resultant trajectories (each trajectory is a list of points)
	 */
	public static ArrayList<ArrayList<SymbolicStatePoint>> simMultiGetTrajectory(
			Configuration config, ArrayList<SymbolicStatePoint> startPoints, double time)
	{
		PythonBridge pb = PythonBridge.getInstance();
		pb.send("from pythonbridge.pysim_utils import simulate_multi_trajectory_time");

		StringBuilder s = new StringBuilder();
		s.append(PySimPrinter.automatonToString(config));

		String modes = makeModeString(startPoints);
		String points = makePointsString(startPoints);

		s.append("print simulate_multi_trajectory_time(define_ha(), " + modes + ", " + points + ", "
				+ time + ")");

		String result = pb.send(s.toString());

		// parse result into SymbolicState objects
		// result is semi-colon separated lists, first is the mode name, rest is
		// the point
		ArrayList<ArrayList<SymbolicStatePoint>> rv = new ArrayList<ArrayList<SymbolicStatePoint>>();

		for (String traj : result.split("\\|"))
		{
			ArrayList<SymbolicStatePoint> trajStates = new ArrayList<SymbolicStatePoint>();

			for (String part : traj.split(";"))
			{
				String[] comma_parts = part.split(",");

				String mode = comma_parts[0]; // first one is the mode
				HyperPoint pt = new HyperPoint(comma_parts.length - 1);

				for (int i = 1; i < comma_parts.length; ++i)
					pt.dims[i - 1] = Double.parseDouble(comma_parts[i]);

				trajStates.add(new SymbolicStatePoint(mode, pt));
			}

			rv.add(trajStates);
		}

		return rv;
	}
}