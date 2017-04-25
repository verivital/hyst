package com.verivital.hyst.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.generators.DrivetrainGenerator;
import com.verivital.hyst.generators.IntegralChainGenerator;
import com.verivital.hyst.generators.NamedNavigationGenerator;
import com.verivital.hyst.generators.NavigationGenerator;
import com.verivital.hyst.generators.SwitchedOscillatorGenerator;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.printers.FlowstarPrinter;
import com.verivital.hyst.printers.HylaaPrinter;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.python.PythonBridge;

/**
 * JUnit tests for model generators
 * 
 * @author sbak
 *
 */
@RunWith(Parameterized.class)
public class GeneratorTests
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

	public GeneratorTests(boolean block)
	{
		PythonBridge.setBlockPython(block);
	}

	@Test
	public void testIntegralChain()
	{
		IntegralChainGenerator gen = new IntegralChainGenerator();

		Configuration c = gen.generate("-M 6 -N 1 -U 1");

		Assert.assertEquals("six variables", 6, c.root.variables.size());

		ToolPrinter printer = new FlowstarPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	@Test
	public void testNav()
	{
		NavigationGenerator gen = new NavigationGenerator();

		String param = "-matrix -1.2 0.1 0.1 -1.2 -i_list 2 2 A 4 3 4 B 2 4 -width 3 "
				+ "-startx 0.5 -starty 1.5 -noise 0.25";
		Configuration c = gen.generate(param);

		Assert.assertEquals("four variables", 4, c.root.variables.size());

		Entry<String, Expression> entry = c.init.entrySet().iterator().next();

		Assert.assertEquals("mode_0_1", entry.getKey());
		Assert.assertEquals(
				"0.5 <= x & x <= 0.5 & 1.5 <= y & y <= 1.5 & -1.0 <= xvel & xvel <= 1.0 & -1.0 <= yvel & yvel <= 1.0",
				entry.getValue().toDefaultString());

		ToolPrinter printer = new FlowstarPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	@Test
	public void testNamedNav()
	{
		NamedNavigationGenerator gen = new NamedNavigationGenerator();

		String param = "-name nav02";
		Configuration c = gen.generate(param);

		Assert.assertEquals("four variables", 4, c.root.variables.size());

		Assert.assertEquals("one inital state", 1, c.init.entrySet().size());

		Entry<String, Expression> entry = c.init.entrySet().iterator().next();

		// x0 = new Interval[] { new Interval(2, 3), new Interval(1, 2) };
		// v0 = new Interval[] { new Interval(-0.3, 0.3), new Interval(-0.3, 0.3) };

		Assert.assertEquals("mode_2_1", entry.getKey());
		Assert.assertEquals(
				"2.0 <= x & x <= 3.0 & 1.0 <= y & y <= 2.0 & -0.3 <= xvel & xvel <= 0.3 & -0.3 <= yvel & yvel <= 0.3",
				entry.getValue().toDefaultString());

		BaseComponent ha = (BaseComponent) c.root;

		AutomatonMode am = ha.modes.get("mode_1_0");
		Expression e = am.flowDynamics.get("xvel").asExpression();
		Assert.assertEquals("-1.2 * (xvel - 1.0) + 0.1 * (yvel - 0.0)", e.toDefaultString());

		// check dynamics in mode 'A'
		am = ha.modes.get("mode_2_0");
		e = am.flowDynamics.get("xvel").asExpression();
		Assert.assertEquals("0.0", e.toDefaultString());

		// check dynamics in mode 'B'
		am = ha.modes.get("mode_0_2");
		e = am.flowDynamics.get("xvel").asExpression();
		Assert.assertEquals("0.0", e.toDefaultString());

		// check condition from mode_2_1 to mode_2_0 is y <= 1
		for (AutomatonTransition at : ha.transitions)
		{
			if (at.from.name.equals("mode_2_1") && at.to.name.equals("mode_2_0"))
				Assert.assertEquals(at.guard.toDefaultString(), "y <= 1.0");
		}

		// make sure it simplifies to an easy linear expression
		if (PythonBridge.hasPython())
		{
			// test printing to Hylaa
			HylaaPrinter printer = new HylaaPrinter();
			printer.pythonSimplify = true;
			printer.setOutputString();
			printer.print(c, "", "model.xml");

			String out = printer.outputString.toString();

			// System.out.println(out);
			Assert.assertTrue("some output exists", out.length() > 10);
		}
	}

	@Test
	public void testNav17()
	{
		// was giving a '-2 unknown param' error
		NamedNavigationGenerator gen = new NamedNavigationGenerator();

		String param = "-name nav17";
		gen.generate(param);
	}

	@Test
	public void testSwitchedOscillator()
	{
		SwitchedOscillatorGenerator gen = new SwitchedOscillatorGenerator();

		String param = "-dims 2";
		Configuration c = gen.generate(param);

		Assert.assertEquals("four variables", 4, c.root.variables.size());
		ToolPrinter printer = new FlowstarPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	@Test
	public void testMatthiasDrivetrain()
	{
		DrivetrainGenerator gen = new DrivetrainGenerator();

		String param = "-theta 1 -init_scale 0 -error_guard x3>=85";
		Configuration c = gen.generate(param);

		Assert.assertEquals("10 variables", 10, c.root.variables.size());
		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	@Test
	public void testMatthiasDrivetrainDynamicsThetaZero()
	{
		HashMap<String, ExpressionInterval> flowDynamics = new HashMap<String, ExpressionInterval>();
		DrivetrainGenerator.makeDynamics(flowDynamics, 0, false, "alpha", "k_s", "u");

		/*
		 * v = p.k_p*(gamma*x(4) - x(7)) ... + p.k_KD*(p.i*u(1) - 1/p.J_m*(x(2) - 1/p.i*p.k*(x(1) -
		 * p.alpha) - p.b_m*x(7))) ... + p.k_KI*(p.i*x(3) - p.i*(x(1) + x(5))) ... +
		 * 0*1/p.i*p.J_l*u(1);
		 */
		// plant model
		// f(1,1) = 1/p.i*x(7) - x(6); %Theta_d
		// f(2,1) = (v - x(2))/p.tau_eng; %T_m
		// f(3,1) = x(4); %Theta_ref
		// f(4,1) = u(1); %\dot{Theta}_ref
		// f(5,1) = x(6); %Theta_l
		// f(6,1) = 1/p.J_l*(p.k*(x(1) - p.alpha) - u(2) - p.b_l*x(6)); %\dot{Theta}_l
		// f(7,1) = 1/p.J_m*(x(2) - 1/p.i*p.k*(x(1) - p.alpha) - p.b_m*x(7)); %\dot{Theta}_m

		String v = "k_P * (gamma * x4 - x7) + k_I * (gamma * x3 - gamma * (x1 + x5)) + "
				+ "k_D * (gamma * u - 1.0 / J_m * (x2 - 1.0 / gamma * k_s * (x1 - alpha) - b_m * x7)";
		HashMap<String, String> expected = new HashMap<String, String>();
		expected.put("x1", "1.0 / gamma * x7 - x6");
		expected.put("x2", "(" + v + ") - x2) / tau_eng");
		expected.put("x3", "x4");
		expected.put("x4", "u");
		expected.put("x5", "x6");
		expected.put("x6", "1.0 / J_l * (k_i * (x1 - alpha) - b_l * x6)");
		expected.put("x7", "1.0 / J_m * (x2 - 1.0 / gamma * k_s * (x1 - alpha) - b_m * x7)");

		for (Entry<String, ExpressionInterval> e : flowDynamics.entrySet())
		{
			String expectedString = expected.get(e.getKey());

			Assert.assertNotNull("flow for '" + e.getKey() + "' was not expected.", expectedString);

			Assert.assertEquals("Mismatch in flow for " + e.getKey(), expectedString,
					e.getValue().toDefaultString());
		}
	}

	@Test
	public void testMatthiasDrivetrainDynamicsThetaOne()
	{
		HashMap<String, ExpressionInterval> flowDynamics = new HashMap<String, ExpressionInterval>();
		DrivetrainGenerator.makeDynamics(flowDynamics, 1, false, "alpha", "k_s", "u");

		// v = p.k_K*(p.i*x(4) - x(7)) ...
		// + p.k_KD*(p.i*u(1) - 1/p.J_m*(x(2) - 1/p.i*p.k*(x(1) - p.alpha) - p.b_m*x(7))) ...
		// + p.k_KI*(p.i*x(3) - p.i*(x(1) + x(8))) ...
		// + 0*1/p.i*p.J_l*u(1);

		// %plant model
		// f(1,1) = 1/p.i*x(7) - x(9); %Theta_d
		// f(2,1) = (v - x(2))/p.tau_eng; %T_m
		// f(3,1) = x(4); %Theta_ref
		// f(4,1) = u(1); %\dot{Theta}_ref
		// f(5,1) = x(6); %Theta_l
		// f(6,1) = 1/p.J_l*(p.k_i*(x(8) - x(5)) - u(2) - p.b_l*x(6)); %\dot{Theta}_l
		// f(7,1) = 1/p.J_m*(x(2) - 1/p.i*p.k*(x(1) - p.alpha) - p.b_m*x(7)); %\dot{Theta}_m
		// f(8,1) = x(9); %Theta_1
		// f(9,1) = p.J_i*(p.k*(x(1) - p.alpha) - p.k_i*(x(8) - x(5)) - p.b_i*x(9)); %\dot{Theta}_1

		String v = "k_P * (gamma * x4 - x7) + k_I * (gamma * x3 - gamma * (x1 + x8)) + "
				+ "k_D * (gamma * u - 1.0 / J_m * (x2 - 1.0 / gamma * k_s * (x1 - alpha) - b_m * x7)";

		HashMap<String, String> expected = new HashMap<String, String>();
		expected.put("x1", "1.0 / gamma * x7 - x9");
		expected.put("x2", "(" + v + ") - x2) / tau_eng");
		expected.put("x3", "x4");
		expected.put("x4", "u");
		expected.put("x5", "x6");
		expected.put("x6", "1.0 / J_l * (k_i * (x8 - x5) - b_l * x6)");
		expected.put("x7", "1.0 / J_m * (x2 - 1.0 / gamma * k_s * (x1 - alpha) - b_m * x7)");
		expected.put("x8", "x9");
		expected.put("x9", "J_i * (k_s * (x1 - alpha) - k_i * (x8 - x5) - b_i * x9)");

		for (Entry<String, ExpressionInterval> e : flowDynamics.entrySet())
		{
			String expectedString = expected.get(e.getKey());

			Assert.assertNotNull("flow for '" + e.getKey() + "' was not expected.", expectedString);

			Assert.assertEquals("Mismatch in flow for " + e.getKey(), expectedString,
					e.getValue().toDefaultString());
		}
	}

	@Test
	public void testMatthiasDrivetrainDynamicsThetaTwo()
	{
		HashMap<String, ExpressionInterval> flowDynamics = new HashMap<String, ExpressionInterval>();
		DrivetrainGenerator.makeDynamics(flowDynamics, 2, false, "alpha", "k_s", "u");

		// %control
		// v = p.k_K*(p.i*x(4) - x(7)) ...
		// + p.k_KD*(p.i*u(1) - 1/p.J_m*(x(2) - 1/p.i*p.k*(x(1) - p.alpha) - p.b_m*x(7))) ...
		// + p.k_KI*(p.i*x(3) - p.i*(x(1) + x(8)));

		// %plant model
		// f(1,1) = 1/p.i*x(7) - x(9); %Theta_d
		// f(2,1) = (v - x(2))/p.tau_eng; %T_m
		// f(3,1) = x(4); %Theta_ref
		// f(4,1) = u(1); %\dot{Theta}_ref
		// f(5,1) = x(6); %Theta_l
		// f(6,1) = 1/p.J_l*(p.k_i*(x(10) - x(5)) - u(2) - p.b_l*x(6)); %\dot{Theta}_l
		// f(7,1) = 1/p.J_m*(x(2) - 1/p.i*p.k*(x(1) - p.alpha) - p.b_m*x(7)); %\dot{Theta}_m
		// f(8,1) = x(9); %Theta_1
		// f(9,1) = p.J_i*(p.k*(x(1) - p.alpha) - p.k_i*(x(8) - x(10)) - p.b_i*x(9)); %\dot{Theta}_1
		// f(10,1) = x(11); %Theta_2
		// f(11,1) = p.J_i*(p.k_i*(x(8) - x(10)) - p.k_i*(x(10) - x(5)) - p.b_i*x(11));
		// %\dot{Theta}_2

		String v = "k_P * (gamma * x4 - x7) + k_I * (gamma * x3 - gamma * (x1 + x8)) + "
				+ "k_D * (gamma * u - 1.0 / J_m * (x2 - 1.0 / gamma * k_s * (x1 - alpha) - b_m * x7)";

		HashMap<String, String> expected = new HashMap<String, String>();
		expected.put("x1", "1.0 / gamma * x7 - x9");
		expected.put("x2", "(" + v + ") - x2) / tau_eng");
		expected.put("x3", "x4");
		expected.put("x4", "u");
		expected.put("x5", "x6");
		expected.put("x6", "1.0 / J_l * (k_i * (x10 - x5) - b_l * x6)");
		expected.put("x7", "1.0 / J_m * (x2 - 1.0 / gamma * k_s * (x1 - alpha) - b_m * x7)");
		expected.put("x8", "x9");
		expected.put("x9", "J_i * (k_s * (x1 - alpha) - k_i * (x8 - x10) - b_i * x9)");
		expected.put("x10", "x11");
		expected.put("x11", "J_i * (k_i * (x8 - x10) - k_i * (x10 - x5) - b_i * x11)");

		for (Entry<String, ExpressionInterval> e : flowDynamics.entrySet())
		{
			String expectedString = expected.get(e.getKey());

			Assert.assertNotNull("flow for '" + e.getKey() + "' was not expected.", expectedString);

			Assert.assertEquals("Mismatch in flow for " + e.getKey(), expectedString,
					e.getValue().toDefaultString());
		}
	}

	@Test
	public void testDrivetrainHighInputPysim()
	{
		if (!PythonBridge.hasPython())
			return;

		DrivetrainGenerator gen = new DrivetrainGenerator();

		String param = "-theta 1 -high_input -init_scale 0";
		Configuration c = gen.generate(param);

		Assert.assertEquals("9 variables", 9, c.root.variables.size());
		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(c, "", "in.xml");

		String out = printer.outputString.toString();

		// System.out.println(out);

		Assert.assertTrue("some output exists", out.length() > 10);

		// shouldn't be doing integer division
		Assert.assertFalse("Pysim printer shouldn't be using integer division",
				out.contains("1 / 12"));
	}

	@Test
	public void testDrivetrainHighInputErrorConditionHylaa()
	{
		if (!PythonBridge.hasPython())
			return;

		DrivetrainGenerator gen = new DrivetrainGenerator();

		String param = "-theta 1 -high_input -error_guard x3>=85&x1<=0.11";
		Configuration c = gen.generate(param);

		// should have an error mode
		Assert.assertEquals(c.forbidden.size(), 1);

		Assert.assertEquals("9 variables", 9, c.root.variables.size());
		ToolPrinter printer = new HylaaPrinter();
		printer.setOutputString();
		printer.print(c, "-s", "in.xml");

		String out = printer.outputString.toString();

		// System.out.println(out);

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	@Test
	public void testDrivetrainPysimPointInit()
	{
		if (!PythonBridge.hasPython())
			return;

		DrivetrainGenerator gen = new DrivetrainGenerator();

		String param = "-theta 1 -high_input -init_points 20";
		Configuration c = gen.generate(param);

		Assert.assertEquals("9 variables", 9, c.root.variables.size());
		Assert.assertEquals("1 init state", 1, c.init.size());

		String init = c.init.values().iterator().next().toDefaultString();

		Assert.assertTrue("init contains disjunction", init.contains("|"));

		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(c, "", "in.xml");

		String out = printer.outputString.toString();

		// System.out.println(out);

		Assert.assertTrue("some output exists", out.length() > 10);

		// shouldn't be doing integet division
		Assert.assertFalse("Pysim printer shouldn't be using integer division",
				out.contains("1 / 12"));
	}

	@Test
	public void testDrivetrainReverseErrors()
	{
		DrivetrainGenerator gen = new DrivetrainGenerator();

		String param = "-theta 1 -init_scale 0 -reverse_errors";
		Configuration c = gen.generate(param);

		Assert.assertEquals("10 variables", 10, c.root.variables.size());
		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}
}
