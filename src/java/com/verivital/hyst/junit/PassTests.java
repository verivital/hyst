package com.verivital.hyst.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.basic.CopyInstancePass;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.printers.FlowstarPrinter;
import com.verivital.hyst.printers.HylaaPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.python.PythonBridge;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * JUnit tests for transformation passes
 * 
 * @author sbak, ttj
 *
 */
@RunWith(Parameterized.class)
public class PassTests
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

	public PassTests(boolean block)
	{
		PythonBridge.setBlockPython(block);
	}

	public static String UNIT_BASEDIR = "tests/unit/models/";

	/**
	 * make a sample network configuration, which is used in multiple tests
	 * 
	 * @return the constructed Configuration
	 */
	private static Configuration makeSampleNetworkConfiguration()
	{
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

	@Test
	public void testSimplifyExpressions()
	{
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
	public void testSubConstantsPll()
	{
		String path = UNIT_BASEDIR + "pll/";
		String spaceExFile = path + "pll_orig.xml";
		String configFile = path + "pll_orig.cfg";

		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(configFile, spaceExFile);

		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(spaceExDoc);
		Configuration config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);

		new SubstituteConstantsPass().runTransformationPass(config, null);
		new SimplifyExpressionsPass().runTransformationPass(config, null);
	}

	/**
	 * Replicate a component instance
	 */
	@Test
	public void testCopyInstancePass()
	{
		String path = UNIT_BASEDIR + "heli_large/";
		String spaceExFile = path + "heli_large.xml";
		String configFile = path + "heli_large.cfg";

		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(configFile, spaceExFile);
		spaceExDoc.setForbiddenStateConditions(FormulaParser.parseInitialForbidden("x8 >= 0.5"));

		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(spaceExDoc);
		Configuration config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);

		new CopyInstancePass().runTransformationPass(config, "-name Controlled_Heli_1 -num 3");
		NetworkComponent root = (NetworkComponent) config.root;

		Assert.assertEquals("four instances after copy pass", 4, root.children.size());

		for (String instance : new String[] { "Controlled_Heli_1", "clock_1",
				"copy2_Controlled_Heli_1", "copy3_Controlled_Heli_1" })
			Assert.assertTrue("child instance named '" + instance + "' exists",
					root.children.containsKey(instance));

		ComponentInstance copy3 = root.children.get("copy3_Controlled_Heli_1");

		for (ComponentMapping mapping : copy3.varMapping)
		{
			Assert.assertEquals("variable was correctly renamed in copy3",
					"copy3_" + mapping.childParam, mapping.parentParam);
		}

		// check initial states
		Expression initExp = config.init.values().iterator().next();

		HashMap<String, Interval> ranges = FlowstarPrinter.getExpressionVariableRanges(initExp);

		for (ComponentMapping mapping : copy3.varMapping)
		{
			Interval range1 = ranges.get(mapping.childParam);
			Interval range2 = ranges.get(mapping.parentParam);

			Assert.assertTrue("initial range for " + mapping.childParam + " and "
					+ mapping.childParam + " were not equal", range1.equals(range2, 1e-9));
		}

		ToolPrinter tp = new HylaaPrinter();
		tp.setOutputNone();

		tp.print(config, "", "out.xml");
	}
}
