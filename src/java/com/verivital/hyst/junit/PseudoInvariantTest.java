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
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantPass;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantSimulatePass;
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
		// pseudo invariant at (1.5,1.5) in direction <1,0> should be 1.0 * x + 0.0 * y >= 1.5
		HyperPoint point = new HyperPoint(1.5, 1.5);
		HyperPoint dir = new HyperPoint(-1.0, 0);
		String expectedResult = "-1 * x <= -1.5";
		
		ArrayList <String> vars = new ArrayList<String>(2);
		vars.add("x");
		vars.add("y");
		
		Expression inv = PseudoInvariantPass.createInvariantExpression(vars, point, dir); 
		
		Assert.assertEquals(expectedResult, DefaultExpressionPrinter.instance.print(inv));
	}
	
	@Test
	public void testPseudoInvariantCondition2()
	{
		// pseudo invariant at (0, 0) in direction <0,1> should be 0.0 * x + 1.0 * y >= 0.0
		HyperPoint point = new HyperPoint(0, 0);
		HyperPoint dir = new HyperPoint(0, -1);
		String expectedResult = "-1 * y <= 0";
		
		ArrayList <String> vars = new ArrayList<String>(2);
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
		String[][] dynamics1 = {{"x", "2"}}; // x' == 2, x(0) = 0
		String[][] dynamics2 = {{"x", "1"}}; // x' == 1
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics1, "x <= 2", "x >= 2", dynamics2);
		
		SymbolicStatePoint start = new SymbolicStatePoint("mode1", new HyperPoint(0.0));
		List <Double> times = Arrays.asList(0.5, 2.0);
		
		List<SymbolicStatePoint> result = PseudoInvariantSimulatePass.pythonSimulate(c, start, times);
		double TOL = 1e-6;
		
		Assert.assertEquals("mode1", result.get(0).modeName);
		Assert.assertEquals(1.0, result.get(0).hp.dims[0], TOL);
		
		Assert.assertEquals("mode2", result.get(1).modeName);
		Assert.assertEquals(3.0, result.get(1).hp.dims[0], TOL);
	}
	
	/**
	 * Test pseudo-invariant simulate pass (which in turn uses pseudo-invariant
	 * pass)
	 */
	@Test
	public void testPseudoInvariantSimulatePass() 
	{
		if (!PythonBridge.hasPython())
			return;
		
		// make a trivial automation with x' == 1
		String[][] dynamics = {{"x", "1", "0"}};
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent)c.root;
		
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
		
		Assert.assertTrue("first mode's invariant is x <= 2",
				pi0.invariant.toDefaultString().equals("2 * x <= 4"));
		Assert.assertTrue("second mode's invariant is x <= 5",
				pi1.invariant.toDefaultString().equals("5 * x <= 25"));
		Assert.assertTrue("final mode's invariant is true", piFinal.invariant == Constant.TRUE);
		
		// the initial state for final should contain both pi guards
		Expression e = c.init.get("on");
		Assert.assertNotNull("on is one of the initial states", e);
		
		Assert.assertTrue("init(on) contains x >= 2",
				e.toDefaultString().contains("2 * x >= 4"));
		Assert.assertTrue("init(on) contains x >= 5",
				e.toDefaultString().contains("5 * x >= 25"));
	}
}
