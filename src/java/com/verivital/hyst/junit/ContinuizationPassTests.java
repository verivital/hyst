package com.verivital.hyst.junit;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
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
	
	@Test
	public void testContinuizationPassSineWave() 
	{
		String[][] dynamics = { { "y", "cos(t)" }, { "t", "1" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		String continuizationParam = ContinuizationPass.makeParamString("y", null, 0.1, false, 
				Arrays.asList(new Double[]{1.57, 3.14}),
				Arrays.asList(new Double[]{0.2, 0.2}));
				
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
	}
}
