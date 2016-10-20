package com.verivital.hyst.junit;

import java.util.Arrays;
import java.util.Collection;
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
				"0.5 <= x & x <= 0.5 & 1.5 <= y & y <= 1.5 & -1 <= xvel & xvel <= 1 & -1 <= yvel & yvel <= 1",
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
				"2 <= x & x <= 3 & 1 <= y & y <= 2 & -0.3 <= xvel & xvel <= 0.3 & -0.3 <= yvel & yvel <= 0.3",
				entry.getValue().toDefaultString());

		BaseComponent ha = (BaseComponent) c.root;

		AutomatonMode am = ha.modes.get("mode_1_0");
		Expression e = am.flowDynamics.get("xvel").asExpression();
		Assert.assertEquals("-1.2 * (xvel - 1) + 0.1 * (yvel - 0)", e.toDefaultString());

		// check dynamics in mode 'A'
		am = ha.modes.get("mode_2_0");
		e = am.flowDynamics.get("xvel").asExpression();
		Assert.assertEquals("0", e.toDefaultString());

		// check dynamics in mode 'B'
		am = ha.modes.get("mode_0_2");
		e = am.flowDynamics.get("xvel").asExpression();
		Assert.assertEquals("0", e.toDefaultString());

		// check condition from mode_2_1 to mode_2_0 is y <= 1
		for (AutomatonTransition at : ha.transitions)
		{
			if (at.from.name.equals("mode_2_1") && at.to.name.equals("mode_2_0"))
				Assert.assertEquals(at.guard.toDefaultString(), "y <= 1");
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
	public void testMatthiasDrivetrainThetaZero()
	{
		DrivetrainGenerator gen = new DrivetrainGenerator();

		String param = "-theta 0 -init_scale 0 -error_guard x3>=85";
		Configuration c = gen.generate(param);

		Assert.assertEquals("8 variables", 8, c.root.variables.size());
		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
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

		// shouldn't be doing integet division
		Assert.assertFalse("Pysim printer shouldn't be using integer division",
				out.contains("1 / 12"));
	}

	@Test
	public void testDrivetrainHighInputHylaa()
	{
		if (!PythonBridge.hasPython())
			return;

		DrivetrainGenerator gen = new DrivetrainGenerator();

		String param = "-theta 1 -high_input";
		Configuration c = gen.generate(param);

		Assert.assertEquals("9 variables", 9, c.root.variables.size());
		ToolPrinter printer = new HylaaPrinter();
		printer.setOutputString();
		printer.print(c, "-s", "in.xml");

		String out = printer.outputString.toString();

		// System.out.println(out);

		Assert.assertTrue("some output exists", out.length() > 10);

		// shouldn't be doing integet division
		Assert.assertFalse("Pysim printer shouldn't be using integer division",
				out.contains("1 / 12"));
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
}
