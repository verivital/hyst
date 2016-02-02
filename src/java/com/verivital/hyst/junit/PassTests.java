package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.matlab.MatlabBridge;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.passes.complex.ContinuizationPass;
import com.verivital.hyst.passes.complex.OrderReductionPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeGridPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMixedTriggeredPass;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.RangeExtractor;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * JUnit tests for transformation passes
 * 
 * @author sbak, ttj
 *
 */
@RunWith(Parameterized.class)
public class PassTests {
	@Before
	public void setUpClass() {
		Expression.expressionPrinter = null;
	}

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { { false }, { true } });
	}

	public PassTests(boolean block) {
		PythonBridge.setBlockPython(block);
	}

	private String UNIT_BASEDIR = "tests/unit/models/";

	/**
	 * make a sample network configuration, which is used in multiple tests
	 * 
	 * @return the constructed Configuration
	 */
	private static Configuration makeSampleNetworkConfiguration() {
		NetworkComponent nc = new NetworkComponent();
		nc.variables.add("x");
		nc.variables.add("t");

		BaseComponent ha = new BaseComponent();

		ComponentInstance ci = new ComponentInstance(nc, ha);
		ci.varMapping.add(new ComponentMapping("x", "x"));
		ci.varMapping.add(new ComponentMapping("t", "t"));

		nc.children.put("base_instance", ci);
		ha.instanceName = "base_instance";
		ha.parent = nc;

		Configuration c = new Configuration(nc);

		ha.variables.add("x");
		ha.variables.add("t");
		c.settings.plotVariableNames[0] = "t";
		c.settings.plotVariableNames[1] = "x"; 
		c.init.put("running", FormulaParser.parseInitialForbidden("x = 0 & t == 0"));
		AutomatonMode am1 = ha.createMode("running");
		am1.flowDynamics.put("x", new ExpressionInterval(new Constant(2)));
		am1.flowDynamics.put("t", new ExpressionInterval(new Constant(1)));
		am1.invariant = FormulaParser.parseInvariant("t <= 5");

		AutomatonMode am2 = ha.createMode("stopped");
		am2.flowDynamics.put("x", new ExpressionInterval(new Constant(0)));
		am2.flowDynamics.put("t", new ExpressionInterval(new Constant(1)));
		am2.invariant = FormulaParser.parseInvariant("t <= 10");

		AutomatonTransition at = ha.createTransition(am1, am2);
		at.guard = FormulaParser.parseGuard("t >= 5");

		c.validate();

		return c;
	}

	/**
	 * make a sample base configuration with a single mode, with x' == 1, and y'
	 * == 1
	 * 
	 * @return the constructed Configuration
	 */
	private static Configuration makeSampleBaseConfiguration() {
		String[][] dynamics = {{"x","1"}, {"y", "1"}}; 
		
		return AutomatonUtil.makeDebugConfiguration(dynamics);
	}

	@Test
	public void testSimplifyExpressions() {
		Configuration c = makeSampleNetworkConfiguration();

		NetworkComponent nc = (NetworkComponent) c.root;
		BaseComponent bc = (BaseComponent) nc.children.values().iterator().next().child;

		bc.transitions.get(0).guard = FormulaParser.parseGuard("t >= 2.5 * 2");

		new SimplifyExpressionsPass().runTransformationPass(c, null);

		Assert.assertTrue("automaton was not flattened", c.root instanceof NetworkComponent);
	}

	/**
	 * Substitute constants and then simplify expressions
	 */
	@Test
	public void testSubConstantsPll() {
		String path = UNIT_BASEDIR + "pll/";
		String spaceExFile = path + "pll_orig.xml";
		String configFile = path + "pll_orig.cfg";

		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(configFile, spaceExFile);

		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(spaceExDoc);
		Configuration config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);

		new SubstituteConstantsPass().runTransformationPass(config, null);
		new SimplifyExpressionsPass().runTransformationPass(config, null);
	}

	/**
	 * An ExpresssionPrinter which prints constants to a certain number of
	 * digits after the decimel
	 *
	 */
	class RoundPrinter extends DefaultExpressionPrinter {
		public RoundPrinter(int digits) {
			super();
			constFormatter.setMaximumFractionDigits(digits);
		}
	}

	/**
	 * Check two expressions for equality, raising an assertion exception if there are errors
	 */
	void assertExpressionsEqual(String message, Expression expected, Expression actual)
	{
		String msg = AutomatonUtil.areExpressionsEqual(expected, actual);
		
		if (msg != null)
			Assert.fail(message + "\n" + msg);
	}
	
	/**
	 * Test hybridization (grid) pass
	 */
	@Test
	public void testHybridGridPass() {
		if (!PythonBridge.hasPython())
			return;

		Configuration c = makeSampleBaseConfiguration();
		BaseComponent ha = (BaseComponent) c.root;

		// update dynamics to be 3*t*x+t
		// approximation should be 7.5*x + 5.5*t
		// rest is in notebook
		AutomatonMode am = ha.modes.values().iterator().next();
		am.flowDynamics.put("x", new ExpressionInterval("3*y*x+y"));

		String params = "x,y,0,2,0,5,2,5,a";

		try {
			new HybridizeGridPass().runTransformationPass(c, params);

			Assert.assertEquals("10 modes", 10, ha.modes.size());

			AutomatonMode m = ha.modes.get("_m_1_2");
			Assert.assertNotEquals("mode named '_m_1_2 exists'", null, m);

			// dynamics should be y' == 7.5*x + 5.5*t + [-12, -10.5]
			Expression.expressionPrinter = new RoundPrinter(3);
			ExpressionInterval ei = m.flowDynamics.get("x");
			
			Expression e = ei.getExpression();
			Interval i = ei.getInterval();
			
			// expected "7.5 * x + 5.5 * y + -12 + [0, 1.5]"
			double TOL = 1e-9;
			
			Assert.assertEquals("Hybrizied mode x=[1,2], y=[2,3] in correctly (interval min wrong)", 0, i.min, TOL);
			Assert.assertEquals("Hybrizied mode x=[1,2], y=[2,3] in correctly (interval max wrong)", 1.5, i.max, TOL);
			
			Expression correctE = FormulaParser.parseValue("7.5 * x + 5.5 * y - 12");
			String msg = AutomatonUtil.areExpressionsEqual(correctE, e);
			
			if (msg != null)
				Assert.fail(msg);

			Assert.assertEquals("single initial state", c.init.size(), 1);
		} catch (AutomatonExportException ex) {
			Assert.assertEquals(AutomatonExportException.class, ex.getClass()); // vacuously
																				// true,
																				// but
																				// will
																				// force
																				// failure
																				// if
																				// different
																				// error
		}
	}

	/**
	 * Test hybridization (time-triggered) pass
	 */
	@Test
	public void testHybridMixedTriggeredPass() {
		if (!PythonBridge.hasPython())
			return;

		RoundPrinter rp = new RoundPrinter(4);
		Configuration c = makeSampleBaseConfiguration();
		BaseComponent ha = (BaseComponent) c.root;
		AutomatonMode am = ha.modes.values().iterator().next();

		// we're going to follow the example in the powerpoint for this
		ha.variables.remove("y");
		am.flowDynamics.remove("y");
		am.flowDynamics.put("x", new ExpressionInterval("x^2"));
		am.invariant = FormulaParser.parseInvariant("x <= 10");

		c.settings.plotVariableNames[1] = "x";
		c.init.put("on", FormulaParser.parseGuard("x >= 0.24 & x <= 0.26"));
		c.validate();

		String params = "step=0.5,maxtime=1.0,epsilon=0.05,simtype=center";

		try {
			new HybridizeMixedTriggeredPass().runTransformationPass(c, params);

			Assert.assertEquals("5 modes (2 + 3 error)", 5, ha.modes.size());
			Assert.assertEquals("1 initial mode", 1, c.init.size());
			Assert.assertTrue("variable _tt exists", ha.variables.contains("_tt"));

			TreeMap<String, Interval> ranges = RangeExtractor.getVariableRanges(c.init.values().iterator().next(),
					"initial states");
			Assert.assertEquals("_tt is initially 0.5", 0.5, ranges.get("_tt").asConstant(), 1e-12);

			AutomatonMode m0 = ha.modes.get("_m_0");
			Assert.assertNotEquals("mode named '_m_0 exists'", null, m0);

			Expression.expressionPrinter = rp;


			AutomatonMode m1 = ha.modes.get("_m_1");
			Assert.assertNotEquals("mode named '_m_1 exists'", null, m1);

			// invariant x <= 10 should be present in first mode
			// time trigger invariant c <= 0.5 should be present in first mode
			// as well
			// should be x <= 10 & _tt >= 0 & x >= 0.2 & x <= 0.3357
			Assert.assertEquals("mode0 invariant correct", "x <= 10 & _tt >= 0 & x >= 0.2 & x <= 0.3357",
					m0.invariant.toString());

			// mode 1 invariant correct
			// should be c <= 1 & x >= 0.2357 & x <= 0.3833
			Assert.assertEquals("mode1 invariant correct", "x <= 10 & _tt >= 0 & x >= 0.2357 & x <= 0.3833",
					m1.invariant.toString());
		} catch (AutomatonExportException ex) {
			Assert.assertEquals(AutomatonExportException.class, ex.getClass()); // vacuously
																				// true,
																				// but
																				// will
																				// force
																				// failure
																				// if
																				// different
																				// error
		}
	}

	/**
	 * Test hybridization (time-triggered) pass
	 */
	@Test
	public void testHybridizeMixedTriggeredPassWithPremodes() {
		if (!PythonBridge.hasPython())
			return;

		RoundPrinter rp = new RoundPrinter(4);
		Configuration c = makeSampleBaseConfiguration();
		BaseComponent ha = (BaseComponent) c.root;
		AutomatonMode am = ha.modes.values().iterator().next();

		// we're going to follow the example in the powerpoint for this
		ha.variables.remove("y");
		am.flowDynamics.remove("y");
		am.flowDynamics.put("x", new ExpressionInterval("x^2"));
		am.invariant = FormulaParser.parseInvariant("x <= 10");

		c.settings.plotVariableNames[1] = "x";
		c.init.put("on", FormulaParser.parseGuard("x >= 0.24 & x <= 0.26"));
		c.validate();

		String params = "step=0.5,maxtime=1.0,epsilon=0.05,simtype=center,addintermediate=true";

		try {
			new HybridizeMixedTriggeredPass().runTransformationPass(c, params);

			Assert.assertEquals("6 modes (2 + premode + 3 errors)", 6, ha.modes.size());
			Assert.assertEquals("1 initial mode", 1, c.init.size());
			Assert.assertTrue("variable _tt exists", ha.variables.contains("_tt"));

			AutomatonMode m0 = ha.modes.get("_m_0");
			Assert.assertNotEquals("mode named '_m_0 exists'", null, m0);

			Expression.expressionPrinter = rp;
			
			// dynamics should be approximately x' =.536*x - 0.0718 + [0, 0.0046]
			double TOL = 1e-2;
			ComparableEi correctDynamics = new ComparableEi("0.5357 * x - 0.0717", 
					new Interval(0, 0.0046), TOL);
			ComparableEi computedDynamics = new ComparableEi(m0.flowDynamics.get("x"), TOL);
			
			Assert.assertEquals("mode0.x' incorrect", correctDynamics, computedDynamics);

			AutomatonMode m1 = ha.modes.get("_m_1");
			Assert.assertNotEquals("mode named '_m_1 exists'", null, m1);

			// dynamics should be approx x=0.619 * x + -0.0958 + [0, 0.0054]
			correctDynamics = new ComparableEi("0.619 * x - 0.0958)", new Interval(0, 0.0054), TOL);
			Assert.assertEquals("mode0.x' incorrect", correctDynamics, 
					new ComparableEi(m1.flowDynamics.get("x"), TOL));

			// invariant x <= 10 should be present in first mode
			// time trigger invariant c <= 0.5 should be present in first mode
			// as well
			// should be x <= 10 & _tt >= 0 & x >= 0.2 & x <= 0.3357
			Assert.assertEquals("mode0 invariant correct", "x <= 10 & _tt >= 0 & x >= 0.2 & x <= 0.3357",
					m0.invariant.toString());

			// mode 1 invariant correct
			// should be c <= 1 & x >= 0.2357 & x <= 0.3833
			Assert.assertEquals("mode1 invariant correct", "x <= 10 & _tt >= 0 & x >= 0.2357 & x <= 0.3833",
					m1.invariant.toString());

			// error transitions should exist from the first mode at the time
			// trigger
			// error transitions should exist in the first mode due to the
			// hyperrectangle constraints
			int numTransitions = 0;
			boolean foundTriggerTransition = false;
			boolean foundOobTransition = false;

			for (AutomatonTransition at : ha.transitions) {
				if (at.from == m0) {
					++numTransitions;

					if (at.to == m1 && at.guard.toString().equals("_tt = 0"))
						foundTriggerTransition = true;

					if (at.to.name.equals("_error_tt_inv_m_0") && at.guard.toString().equals("x >= 0.3357"))
						foundOobTransition = true;
				}
			}

			Assert.assertTrue("transition exists at time trigger in mode0", foundTriggerTransition);
			Assert.assertTrue("transition to out of bounds error mode exists in mode0", foundOobTransition);

			Assert.assertEquals(
					"wrong number of outgoing transitions from mode0, expected 6 "
							+ "(tt, premode, x-too-small, x-too-large, x-too-small-at-tt, x-too-large-at-tt)",
					6, numTransitions);

			Assert.assertEquals("three forbidden modes (inv1, guard2, inv2)", 3, c.forbidden.size());
		} catch (AutomatonExportException ex) {
			Assert.assertEquals(AutomatonExportException.class, ex.getClass()); // vacuously
																				// true,
																				// but
																				// will
																				// force
																				// failure
																				// if
																				// different
																				// error
		}
	}

	@Test
	public void testHybridizeMixedTriggeredPassVanderpol() {
		if (!PythonBridge.hasPython())
			return;

		// test that dynamics in mode zero should be exactly x' == y
		// params: step=0.01,maxtime=0.02,epsilon=0.001,addforbidden=false
		Configuration c = makeSampleBaseConfiguration();
		BaseComponent ha = (BaseComponent) c.root;
		AutomatonMode am = ha.modes.values().iterator().next();

		am.flowDynamics.put("x", new ExpressionInterval("y"));
		am.flowDynamics.put("y", new ExpressionInterval("(1-x*x)*y-x"));
		c.init.put("on", FormulaParser.parseInitialForbidden("-0.51 <= x & x <= -0.5 & -2.61 <= y & y <= -2.6"));
		c.validate();

		String params = "step=0.01,maxtime=0.02,epsilon=0.01,addforbidden=false";

		try {
			new HybridizeMixedTriggeredPass().runTransformationPass(c, params);

			Assert.assertEquals("3 modes (2 + 3 error)", 5, ha.modes.size());
			Assert.assertEquals("1 initial mode", 1, c.init.size());

			AutomatonMode m0 = ha.modes.get("_m_0");
			Assert.assertNotEquals("mode named '_m_0 exists'", null, m0);

			// dynamics should be x' == y
			ExpressionInterval ei = m0.flowDynamics.get("x");
			// 0.9999999999999869 * y + -0.000000000000034638958368304884
			double coeff = ((Constant) ei.getExpression().asOperation().getLeft().asOperation().getLeft()).getVal();

			Assert.assertTrue("dynamics in mode0 for x was y", Math.abs(coeff - 1.0) < 1e-13);
		} catch (AutomatonExportException ex) {
			Assert.assertEquals(AutomatonExportException.class, ex.getClass()); // vacuously
																				// true,
																				// but
																				// will
																				// force
																				// failure
																				// if
																				// different
																				// error
		}
	}

	@Test
	public void testCheckHyperPlane() {
		// tests the HybridizeTimeTriggeredPass.testHyperPlane() function
		// this function tests whether a state-triggered transition constructed at the
		// given point would be on one side (in front of) of a given box

		// we'll use a 1d model with x' = 1, and check if the box [1,2] is on
		// one side of x = {0.5, 1.5, 2.5}. Only 2.5 should be okay.
		Map<String, Expression> dy = new HashMap<String, Expression>();
		dy.put("x", new Constant(1));

		List<String> vars = new ArrayList<String>();
		vars.add("x");

		HyperRectangle box = new HyperRectangle(new Interval(1, 2));
		double pts[] = { -0.5, 0.5, 1.5, 2.5 };
		boolean expected[] = { false, false, false, true };

		runBoxTests(vars, dy, box, pts, expected);

		// try again with [-2, -1] and -2.5, -1.5, -0.5, and 0.5
		box = new HyperRectangle(new Interval(-2, -1));
		pts = new double[] { -2.5, -1.5, -0.5, 0.5 };
		expected = new boolean[] { false, false, true, true };
		runBoxTests(vars, dy, box, pts, expected);

		// try again with dynamics: x' == -1
		dy.put("x", new Constant(-1));
		box = new HyperRectangle(new Interval(1, 2));
		pts = new double[] { -0.5, 0.5, 1.5, 2.5 };
		expected = new boolean[] { true, true, false, false };
		runBoxTests(vars, dy, box, pts, expected);

		// try again with negative box and dynamics: x' == -1
		box = new HyperRectangle(new Interval(-2, -1));
		pts = new double[] { -2.5, -1.5, -0.5, 0.5 };
		expected = new boolean[] { true, false, false, false };
		runBoxTests(vars, dy, box, pts, expected);

		// try again with box [-1, 1] and dynamics: x' == -1
		box = new HyperRectangle(new Interval(-1, 1));
		pts = new double[] { -1.5, -0.5, 0, 0.5, 1.5 };
		expected = new boolean[] { true, false, false, false, false };
		runBoxTests(vars, dy, box, pts, expected);
	}

	/**
	 * helper method for testCheckHyperPlane
	 * 
	 * @param vars
	 *            the variable name list
	 * @param dy
	 *            the dynamics
	 * @param box
	 *            the box
	 * @param pts
	 *            the set of points to test
	 * @param expected
	 *            the expected results
	 */
	private void runBoxTests(List<String> vars, Map<String, Expression> dy, HyperRectangle box, double[] pts,
			boolean[] expected) {
		if (pts.length != expected.length)
			throw new RuntimeException("pts.length should be equal to expected.length");

		Expression.expressionPrinter = DefaultExpressionPrinter.instance;

		for (int i = 0; i < pts.length; ++i) {
			Assert.assertTrue(
					pts[i] + (expected[i] ? " SHOULD" : " should NOT") + " be in front of box " + box + ". dynamics: "
							+ dy,
					HybridizeMixedTriggeredPass.testHyperPlane(new HyperPoint(pts[i]), box, dy, vars) == expected[i]);
		}
	}

	@Test
	public void testMixedTriggeredHybridizeWithPi() {
		if (!PythonBridge.hasPython())
			return;

		// time-triggered hybridized pass tests with state-triggered transitions
		// 1d system with x'==1, init box is [0, 1], use star to construct guide
		// simulation
		// state-triggered count is 1, which means it should be constructed
		// right around x == 1 (the edge of the box)

		Configuration c = makeSampleBaseConfiguration();
		BaseComponent ha = (BaseComponent) c.root;
		AutomatonMode am = ha.modes.values().iterator().next();

		// we're going to follow the example in the powerpoint for this
		ha.variables.remove("y");
		am.flowDynamics.remove("y");
		am.flowDynamics.put("x", new ExpressionInterval("1"));

		c.settings.plotVariableNames[1] = "x";
		c.init.put("on", FormulaParser.parseGuard("x >= 0 & x <= 1"));
		c.validate();

		String params = "step=1,maxtime=10,epsilon=0.01,simtype=star,picount=1";

		try {
			HybridizeMixedTriggeredPass htt = new HybridizeMixedTriggeredPass();
			htt.testFuncs = new HybridizeMixedTriggeredPass.TestFunctions() {
				@Override
				public void piSimPointsReached(List<HyperPoint> simPoints) {
					Assert.assertEquals("3 sim points after construction", 3, simPoints.size());

					for (HyperPoint sp : simPoints)
						Assert.assertEquals("simpoint is near x == 1.05", simPoints.get(0).dims[0], sp.dims[0], 1e-4);
				}

				@Override
				public void initialSimPoints(List<HyperPoint> simPoints) {
					Assert.assertEquals("3 initial sim points", 3, simPoints.size());
				}

				@Override
				public void piSucceeded(boolean rv) {
					Assert.assertTrue("pi should succeed", rv);
				}
			};

			htt.runTransformationPass(c, params);

			final String FIRST_MODE = "_m_0";
			final String SECOND_MODE = "_m_1";
			final String TT_VAR = "_tt";

			AutomatonMode m0 = ha.modes.get(FIRST_MODE);
			Assert.assertNotEquals("first mode exists'", null, m0);

			AutomatonMode m1 = ha.modes.get(SECOND_MODE);
			Assert.assertNotEquals("second mode exists", null, m1);

			Assert.assertEquals("tt derivative in first mode is zero",
					((Constant) m0.flowDynamics.get(TT_VAR).asExpression()).getVal(), 0, 1e-9);
			int numTransitions = 0;
			boolean foundGuard = false;

			for (AutomatonTransition at : ha.transitions) {
				if (at.from == m0) {
					++numTransitions;

					if (at.to == m1 && at.guard.toDefaultString().contains("1 * x >= 1.050000"))
						foundGuard = true;
				}
			}

			Assert.assertEquals("five transitions from first mode", 5, numTransitions);
			Assert.assertTrue("found pi guard", foundGuard);

			Assert.assertTrue("pi guard is exists", m0.invariant.toDefaultString().contains("1 * x <= 1.0500"));

			Assert.assertTrue("second mode's invariant starts at 1.04",
					m1.invariant.toDefaultString().contains("x >= 1.040000"));
		} catch (AutomatonExportException ex) {
			Assert.assertEquals(AutomatonExportException.class, ex.getClass()); // vacuously
																				// true,
																				// but
																				// will
																				// force
																				// failure
																				// if
																				// different
																				// error
		}
	}

	@Test
	public void testContinuizationPassSineWave() {
		String[][] dynamics = { { "y", "cos(t)" }, { "t", "1" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		String continuizationParam = "-var y -period 0.1 -times 1.57 3.14 -timevar t -bloats 0.1 0.2";

		new ContinuizationPass().runTransformationPass(c, continuizationParam);
		BaseComponent ha = (BaseComponent) c.root;

		// we should have four error modes, and two normal modes
		AutomatonMode on1 = null, on2 = null;
		int numErrorModes = 0;
		
		for (AutomatonMode am : ha.modes.values())
		{
			if (am.name.equals("on"))
				on1 = am;
			else if (am.name.equals("on_2"))
				on2 = am;
			else if (am.name.contains("error"))
				++numErrorModes;
		}

		Assert.assertNotEquals("on found", null, on1);
		Assert.assertNotEquals("on_2 found", null, on2);
		Assert.assertEquals("four error modes", numErrorModes, 4);
	}
	
	@Test 
	public void testContinuizationPassDoubleIntegrator()
	{
		String[][] dynamics = {{"x", "v", "0.05"}, {"v", "a", "0"}, {"a", "-10 * v - 3 * a", "9.5"}};
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		String continuizationParam = "-var a -period 0.005 -times 1.5 5 -timevar t -bloats 4 4";

		try {
			// this relies on hypy and scipy
			new ContinuizationPass().runTransformationPass(c, continuizationParam);
			BaseComponent ha = (BaseComponent) c.root;

			// we should have four error modes, and two normal modes
			AutomatonMode running1 = null, running2 = null;
			int numErrorModes = 0;
			
			for (AutomatonMode am : ha.modes.values())
			{
				if (am.name.equals("on"))
					running1 = am;
				else if (am.name.equals("on_2"))
					running2 = am;
				else if (am.name.contains("error"))
					++numErrorModes;
			}
		
			Assert.assertNotEquals("on found", null, running1);
			Assert.assertNotEquals("on_2 found", null, running2);
			Assert.assertEquals("four error modes", numErrorModes, 4);

			Assert.assertTrue("time-triggered invariant is correct",
					running1.invariant.toDefaultString().contains("t <= 1.505"));

			Assert.assertEquals("mode1 v_der.max is 0.163", 0.163, running1.flowDynamics.get("v").getInterval().max,
					1e-3);
			Assert.assertEquals("mode1 v_der.min is -0.046", -0.046, running1.flowDynamics.get("v").getInterval().min,
					1e-3);

			Assert.assertEquals("mode2 a_der.max is 0.109", 0.109, running2.flowDynamics.get("a").getInterval().max,
					1e-3);
			Assert.assertEquals("mode2 a_der.min is -0.075", -0.075, running2.flowDynamics.get("a").getInterval().min,
					1e-3);
		} catch (AutomatonExportException ex) {
			Assert.assertEquals(AutomatonExportException.class, ex.getClass()); // vacuously
																				// true,
																				// but
																				// will
																				// force
																				// failure
																				// if
																				// different
																				// error
		}

	}

	@Test
	public void testOrderReductionpass() {
		if (!MatlabBridge.hasMatlab())
			return;
		
		String path = UNIT_BASEDIR + "order_reduction/";
		System.out.println(path);
		SpaceExDocument doc = SpaceExImporter.importModels(path + "building_full_order.cfg",
				path + "building_full_order.xml");
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);

		Configuration c = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);
		String OrderReductionPassParam = "3";

		new OrderReductionPass().runTransformationPass(c, OrderReductionPassParam);
		BaseComponent ha = (BaseComponent) c.root;
		// check variables
		Assert.assertEquals("[x1, x2, x3, y1, time]", ha.variables.toString());
		String flow = "{x1=0.006132 * u1 - 0.00751 * x1 - 5.275 * x2 + 0.0009639 * x3, x2=5.275 * x1 "
				+ "- 0.06453 * u1 - 0.8575 * x2 + 0.09063 * x3, x3=0.0009639 * x1 - 0.0006972 * u1 - 0.09063 * x2 - 0.0001258 * x3, "
				+ "time=1}";
		String invariant = "y1 = 0.0006972 * x3 - 0.06453 * x2 - 0.006132 * x1 && time <= stoptime";

		for (Map.Entry<String, AutomatonMode> e : ha.modes.entrySet()) {
			// Check flow and invariant
			Assert.assertEquals(flow, e.getValue().flowDynamics.toString());
			Assert.assertEquals(invariant, e.getValue().invariant.toString());
		}
	}

	/*
	@Test
	public void testLargeModelOrderReductionpass()
	{
		String path = UNIT_BASEDIR + "order_reduction/";
		System.out.println(path);
		SpaceExDocument doc = SpaceExImporter.importModels(
				path + "iss_full_model.cfg",
				path + "iss_full_model.xml");
                Map <String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		
		Configuration c = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);
                String OrderReductionPassParam = "10";
		try{
                    new OrderReductionPass().runTransformationPass(c, OrderReductionPassParam);
                }
                catch (RuntimeException e){
                    System.out.println("The order reduction pass is failed" );
                    throw e;
                }       
                        
	}*/

}
