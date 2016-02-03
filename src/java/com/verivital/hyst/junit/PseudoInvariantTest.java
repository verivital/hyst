package com.verivital.hyst.junit;

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
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantSimulatePass;
import com.verivital.hyst.passes.complex.pi.SymbolicStatePoint;
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
		String params = "-times 2.0 5.0"; // simulation time = 2.0 and then 5.0
		new PseudoInvariantSimulatePass().runTransformationPass(c, params);

		// there should be four modes: running_init, running_pi_0, running_pi_1,
		// and running_final
		Assert.assertEquals("four modes after pass", 4, ha.modes.size());

		AutomatonMode piInit = ha.modes.get("running_init");
		AutomatonMode pi0 = ha.modes.get("running_pi_0");
		AutomatonMode pi1 = ha.modes.get("running_pi_1");
		AutomatonMode piFinal = ha.modes.get("running_final");

		Assert.assertTrue("init mode is urgent", piInit.urgent == true);
		Assert.assertTrue("first mode is not null", pi0 != null);
		Assert.assertTrue("first mode has an invariant", pi0.invariant != null);
		Assert.assertTrue("first mode's invariant is x <= 2",
				pi0.invariant.toDefaultString().contains("-1 * x >= -2.0000"));
		Assert.assertTrue("second mode's invariant is x <= 5",
				pi1.invariant.toDefaultString().contains("-1 * x >= -4.9999"));
		Assert.assertTrue("final mode's invariant is true", piFinal.invariant == Constant.TRUE);

		// the transition from init to final should contain both pi guards
		for (AutomatonTransition at : ha.transitions) {
			if (at.from == piInit && at.to == piFinal) {
				Assert.assertTrue("guard from init to final contains x >= 2",
						at.guard.toDefaultString().contains("-1 * x <= -2.0000"));
				Assert.assertTrue("guard from init to final contains x >= 5",
						at.guard.toDefaultString().contains("-1 * x <= -4.9999"));
			}
		}
	}
}
