package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.SymbolicStatePoint;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantInitPass;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantPass;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantSimulatePass;
import com.verivital.hyst.printers.FlowstarPrinter;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;

@RunWith(Parameterized.class)
public class PseudoInvariantTest
{
	@Before
	public void setUpClass()
	{
		Expression.expressionPrinter = null;
	}

	@Parameters
	public static Collection<Object[]> data()
	{
		return Arrays.asList(new Object[][] { { false }, { true } });
	}

	public PseudoInvariantTest(boolean block)
	{
		PythonBridge.setBlockPython(block);
	}

	@Test
	public void testPseudoInvariantCondition1()
	{
		// pseudo invariant at (1.5,1.5) in direction <1,0> should be 1.0 * x +
		// 0.0 * y >= 1.5
		HyperPoint point = new HyperPoint(1.5, 1.5);
		HyperPoint dir = new HyperPoint(-1.0, 0);
		String expectedResult = "-1.0 * x <= -1.5";

		ArrayList<String> vars = new ArrayList<String>(2);
		vars.add("x");
		vars.add("y");

		Expression inv = PseudoInvariantPass.createInvariantExpression(vars, point, dir);

		Assert.assertEquals(expectedResult, DefaultExpressionPrinter.instance.print(inv));
	}

	@Test
	public void testPseudoInvariantCondition2()
	{
		// pseudo invariant at (0, 0) in direction <0,1> should be 0.0 * x + 1.0
		// * y >= 0.0
		HyperPoint point = new HyperPoint(0, 0);
		HyperPoint dir = new HyperPoint(0, -1);
		String expectedResult = "-1.0 * y <= 0.0";

		ArrayList<String> vars = new ArrayList<String>(2);
		vars.add("x");
		vars.add("y");

		Expression inv = PseudoInvariantPass.createInvariantExpression(vars, point, dir);

		Assert.assertEquals(expectedResult, DefaultExpressionPrinter.instance.print(inv));
	}

	/**
	 * Tests for PseudoInvariantSimulatePass.pythonSimulate
	 */
	@Test
	public void testPythonSimulate()
	{
		if (!PythonBridge.hasPython())
			return;

		// This tests the python interface for the pseudo-invariant pass
		String[][] dynamics1 = { { "x", "2" } }; // x' == 2, x(0) = 0
		String[][] dynamics2 = { { "x", "1" } }; // x' == 1
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics1, "x <= 2", "x >= 2",
				dynamics2);

		SymbolicStatePoint start = new SymbolicStatePoint("mode1", new HyperPoint(0.0));
		List<Double> times = Arrays.asList(0.5, 2.0);

		List<SymbolicStatePoint> result = PseudoInvariantSimulatePass.pythonSimulate(c, start,
				times);
		double TOL = 1e-6;

		Assert.assertEquals("mode1", result.get(0).modeName);
		Assert.assertEquals(1.0, result.get(0).hp.dims[0], TOL);

		Assert.assertEquals("mode2", result.get(1).modeName);
		Assert.assertEquals(3.0, result.get(1).hp.dims[0], TOL);
	}

	/**
	 * Test pseudo-invariant simulate pass (which in turn uses pseudo-invariant pass)
	 */
	@Test
	public void testPseudoInvariantSimulatePass()
	{
		if (!PythonBridge.hasPython())
			return;

		// make a trivial automation with x' == 1
		String[][] dynamics = { { "x", "1", "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent) c.root;

		// run the pseudo-invariant pass on it
		// simulation time = 2.0 and then 5.0
		String params = PseudoInvariantSimulatePass.makeParamString(2.0, 5.0);
		new PseudoInvariantSimulatePass().runTransformationPass(c, params);

		// there should be four modes: on, on_2, on_3
		// and running_final
		Assert.assertEquals("three modes after pass", 3, ha.modes.size());

		AutomatonMode pi0 = ha.modes.get("on_2");
		AutomatonMode pi1 = ha.modes.get("on_3");
		AutomatonMode piFinal = ha.modes.get("on");

		Assert.assertTrue("first mode is not null", pi0 != null);
		Assert.assertTrue("first mode has an invariant", pi0.invariant != null);

		Assert.assertEquals("first mode's invariant is x <= 2", pi0.invariant.toDefaultString(),
				"1.0 * x <= 2.0");
		Assert.assertEquals("second mode's invariant is x <= 5", pi1.invariant.toDefaultString(),
				"1.0 * x <= 5.0");
		Assert.assertTrue("final mode's invariant is true", piFinal.invariant == Constant.TRUE);

		// the initial state for final should contain both pi guards
		Expression e = c.init.get("on");
		Assert.assertNotNull("on is one of the initial states", e);

		Assert.assertTrue("init(on) contains x >= 2",
				e.toDefaultString().contains("1.0 * x >= 2.0"));
		Assert.assertTrue("init(on) contains x >= 5",
				e.toDefaultString().contains("1.0 * x >= 5.0"));
	}

	/**
	 * Test pseudo-invariant simulate pass (which in turn uses pseudo-invariant pass), with a single
	 * time
	 */
	@Test
	public void testPseudoInvariantSimulateOnePass()
	{
		if (!PythonBridge.hasPython())
			return;

		// make a trivial automation with x' == 1
		String[][] dynamics = { { "x", "1", "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent) c.root;

		// run the pseudo-invariant pass on it
		// simulation time = 2.0
		String params = PseudoInvariantSimulatePass.makeParamString(2.0);
		new PseudoInvariantSimulatePass().runTransformationPass(c, params);

		// there should be two modes: on, on_2
		// and running_final
		Assert.assertEquals("three modes after pass", 2, ha.modes.size());

		AutomatonMode pi0 = ha.modes.get("on_2");
		AutomatonMode piFinal = ha.modes.get("on");

		Assert.assertTrue("first mode is not null", pi0 != null);
		Assert.assertTrue("first mode has an invariant", pi0.invariant != null);

		Assert.assertTrue("first mode's invariant is x <= 2",
				pi0.invariant.toDefaultString().equals("1.0 * x <= 2.0"));
		Assert.assertTrue("final mode's invariant is true", piFinal.invariant == Constant.TRUE);

		// the initial state for final should contain both pi guards
		Expression e = c.init.get("on");
		Assert.assertNotNull("on is one of the initial states", e);

		Assert.assertTrue("init(on) contains x >= 2",
				e.toDefaultString().contains("1.0 * x >= 2.0"));
	}

	/**
	 * Test pseudo-invariant simulate pass (which in turn uses pseudo-invariant pass), with a single
	 * time, printing to Flow*
	 */
	@Test
	public void testPIVanderpolFlowstar()
	{
		if (!PythonBridge.hasPython())
			return;

		// make a trivial automation with x' == 1
		String[][] dynamics = { { "x", "y", "1" }, { "y", "(1-x*x)*y-x", "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		// manually set initial state
		c.init.put("on", FormulaParser.parseInitialForbidden("x == 1 && -0.5 <= y <= 0.5"));

		ArrayList<HyperPoint> points = new ArrayList<HyperPoint>();
		ArrayList<HyperPoint> dirs = new ArrayList<HyperPoint>();

		points.add(new HyperPoint(0.75, 0));
		dirs.add(new HyperPoint(-1, 0));

		String params = PseudoInvariantPass.makeParamString(null, points, dirs, false);

		BaseComponent ha = (BaseComponent) c.root;

		// run the pseudo-invariant pass on it
		new PseudoInvariantPass().runTransformationPass(c, params);

		// there should be two modes: on, on_2, on_3
		// and running_final
		Assert.assertEquals("two modes after pass", 2, ha.modes.size());

		AutomatonMode on2 = ha.modes.get("on_2");
		Assert.assertNotNull(on2);
		Assert.assertNotNull(ha.modes.get("on"));

		Assert.assertEquals("-1.0 * x <= -0.75", on2.invariant.toDefaultString());

		// try to print to Flow*
		FlowstarPrinter fp = new FlowstarPrinter();

		fp.setOutputNone();
		fp.print(c, "", "filename.xml");
	}

	/**
	 * Test pseudo-invariant simulate pass (which in turn uses pseudo-invariant pass), with a single
	 * time, printing to Flow*
	 */
	@Test
	public void testInitPIVanderpol()
	{
		if (!PythonBridge.hasPython())
			return;

		String[][] dynamics = { { "barrier_clock", "1", "0" }, { "x", "-y", "0" },
				{ "y", "-((1-x*x)*y-x)", "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		BaseComponent ha = (BaseComponent) c.root;

		AutomatonMode on = ha.modes.get("on");
		on.invariant = FormulaParser.parseInvariant("x ^ 2 + y ^ 2 - 9 <= 0.0001");

		// manually set initial state
		String initialInitCond = "barrier_clock == 0 & -3.0001 <= y <= -2.82831491699 & -1.0001 <= x <= 1.0001";
		c.init.put("on", FormulaParser.parseInitialForbidden(initialInitCond));

		c.validate();

		// run the pseudo-invariant pass on it
		new PseudoInvariantInitPass().runTransformationPass(c, "");

		// there should be 3 modes
		// and running_final

		Assert.assertEquals("2 modes after pass", 2, ha.modes.size());

		Assert.assertEquals("1 transitions after pass", 1, ha.transitions.size());

		AutomatonMode on2 = ha.modes.get("on_2");
		Assert.assertNotNull(on2);
		Assert.assertNotNull(ha.modes.get("on"));

		Assert.assertEquals("one initial state", 1, c.init.size());
		Assert.assertEquals("initial state is 'on_2'", "on_2", c.init.keySet().iterator().next());

		// try to print to Flow*
		FlowstarPrinter fp = new FlowstarPrinter();

		// fp.setOutputNone();
		fp.setOutputString();
		fp.print(c, "", "filename.xml");

		// System.out.println(fp.outputString);
	}
}
