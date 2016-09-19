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
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.geometry.SymbolicStatePoint;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.complex.ContinuizationPass;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;

@RunWith(Parameterized.class)
public class ContinuizationPassTests
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

	public ContinuizationPassTests(boolean block)
	{
		PythonBridge.setBlockPython(block);
	}

	/**
	 * Test for the python-base range detection
	 */
	@Test
	public void testPythonRangeTestSim()
	{
		if (!PythonBridge.hasPython())
			return;

		String[][] dynamics = { { "t", "1" }, { "y", "sin(t)" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		ArrayList<Interval> timeIntervals = new ArrayList<Interval>();
		timeIntervals.add(new Interval(0, Math.PI / 4.0));
		timeIntervals.add(new Interval(0, 2 * Math.PI));

		SymbolicStatePoint start = new SymbolicStatePoint("on", new HyperPoint(0, 0));

		List<Interval> result = ContinuizationPass.pythonSimulateDerivativeRange(c, "y", start,
				timeIntervals);

		Assert.assertEquals(2, result.size());

		Interval i1 = result.get(0);
		Interval i2 = result.get(1);

		Interval.COMPARE_TOL = 1e-3;

		// integral of sin(t) is cos(t)
		// 0 to pi/4
		Assert.assertEquals(new Interval(0, Math.sqrt(2) / 2.0), i1);
		Assert.assertEquals(new Interval(-1, 1), i2);
	}

	@Test
	public void testContinuizationPassSineWave()
	{
		if (!PythonBridge.hasPython())
			return;

		String[][] dynamics = { { "y", "cos(t)" }, { "t", "1" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		String continuizationParam = ContinuizationPass.makeParamString("y", null, 0.1, false,
				Arrays.asList(new Double[] { 1.57, 3.14 }),
				Arrays.asList(new Double[] { 0.2, 0.2 }));

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
		if (!PythonBridge.hasPython())
			return;

		String[][] dynamics = { { "x", "v", "0.05" }, { "v", "a", "0" },
				{ "a", "-10 * v - 3 * a", "9.5" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		String continuizationParam = ContinuizationPass.makeParamString("a", "t", 0.005, false,
				Arrays.asList(new Double[] { 1.5, 5.0 }), Arrays.asList(new Double[] { 4.0, 4.0 }));

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

		Assert.assertTrue("on invariant is correct",
				running1.invariant.toDefaultString().contains("t <= 1.5"));

		Assert.assertTrue("time-triggered invariant is correct",
				running1.invariant.toDefaultString().contains("t <= 1.505"));

		// bloated range of a in times 0-1.5 is K=[-6.98, 13.5]
		// [-0.05, 0] * K = [-0.675, 0.349]
		Assert.assertEquals("mode1 v_der.max is 0.163", 0.163,
				running1.flowDynamics.get("v").getInterval().max, 1e-3);
		Assert.assertEquals("mode1 v_der.min is -0.046", -0.046,
				running1.flowDynamics.get("v").getInterval().min, 1e-3);

		Assert.assertEquals("mode2 a_der.max is 0.109", 0.109,
				running2.flowDynamics.get("a").getInterval().max, 1e-3);
		Assert.assertEquals("mode2 a_der.min is -0.075", -0.075,
				running2.flowDynamics.get("a").getInterval().min, 1e-3);
	}

	@Test
	public void testUrgentDoubleIntegrator()
	{
		if (!PythonBridge.hasPython())
			return;

		String[][] dynamics = { { "x", "v", "0" }, { "v", "a", "0" },
				{ "a", "-10 * v - 3 * a", "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		BaseComponent ha = (BaseComponent) c.root;
		AutomatonMode on = ha.modes.get("on");

		AutomatonMode init = ha.createMode("init");
		init.flowDynamics = null;
		init.invariant = Constant.TRUE;
		init.urgent = true;

		AutomatonTransition at = ha.createTransition(init, on);
		at.guard = Constant.TRUE;
		at.reset.put("a", new ExpressionInterval("10 * (1 - x) + 3 * (-v)"));

		c.init.clear();
		c.init.put("init",
				FormulaParser.parseInitialForbidden("0 <= x <= 0.1 && v == 0 && a == 0"));

		c.validate();

		String continuizationParam = ContinuizationPass.makeParamString("a", "t", 0.005, false,
				Arrays.asList(new Double[] { 1.5, 5.0 }), Arrays.asList(new Double[] { 4.0, 4.0 }));

		// this relies on hypy and scipy
		new ContinuizationPass().runTransformationPass(c, continuizationParam);

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

		Assert.assertTrue("on invariant is correct",
				running1.invariant.toDefaultString().contains("t <= 1.5"));

		Assert.assertTrue("time-triggered invariant is correct",
				running1.invariant.toDefaultString().contains("t <= 1.505"));

		// bloated range of a in times 0-1.5 is K=[-6.98, 13.5]
		// [-0.05, 0] * K = [-0.675, 0.349]

		Assert.assertEquals("mode1 v_der.max is 0.163", 0.163,
				running1.flowDynamics.get("v").getInterval().max, 1e-3);
		Assert.assertEquals("mode1 v_der.min is -0.046", -0.046,
				running1.flowDynamics.get("v").getInterval().min, 1e-3);

		Assert.assertEquals("mode2 a_der.max is 0.109", 0.109,
				running2.flowDynamics.get("a").getInterval().max, 1e-3);
		Assert.assertEquals("mode2 a_der.min is -0.075", -0.075,
				running2.flowDynamics.get("a").getInterval().min, 1e-3);
	}
}
