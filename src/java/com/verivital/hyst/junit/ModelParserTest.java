package com.verivital.hyst.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.internalpasses.ConvertIntervalConstants;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.basic.ConvertHavocFlows;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.passes.complex.FlattenAutomatonPass;
import com.verivital.hyst.printers.FlowstarPrinter;
import com.verivital.hyst.printers.SimulinkStateflowPrinter;
import com.verivital.hyst.printers.SpaceExPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Classification;
import com.verivital.hyst.util.FlattenRenameUtils;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;

// Tests dealing with aspects of the modelimporter
@RunWith(Parameterized.class)
public class ModelParserTest
{
	private String UNIT_BASEDIR;

	@Parameters
	public static Collection<Object[]> data()
	{
		return Arrays.asList(new Object[][] { { false }, { true } });
	}

	public ModelParserTest(boolean block)
	{
		PythonBridge.setBlockPython(block);
		
UNIT_BASEDIR = "tests/unit/models/";
		
		File f;
		try {
			f = new File(UNIT_BASEDIR);
			
			if (!f.exists()) {
				UNIT_BASEDIR = "src" + File.separator + UNIT_BASEDIR;
			}
		}
		catch (Exception ex0) {
			try {
				UNIT_BASEDIR = "src" + File.separator + UNIT_BASEDIR;
				f = new File(UNIT_BASEDIR); 
			}
			catch (Exception ex1) {
				
				//if (!f.exists()) {
				//	throw new Exception("Bad unit test base directory: " +
				//			UNIT_BASEDIR + " not found; full path tried: " + new File(UNIT_BASEDIR).getAbsolutePath());
				//}
			}
		}
	}

	@Before
	public void setUpClass()
	{
		Expression.expressionPrinter = null;
	}

	public static Configuration flatten(SpaceExDocument spaceExDoc)
	{
		// 2. convert the SpaceEx data structures to template automata
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(spaceExDoc);

		// 3. run any component template passes here (future)
		// 4. instantiate the component templates into a networked configuration
		Configuration config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);

		FlattenAutomatonPass.flattenAndOptimize(config);

		return config;
	}

	/**
	 * Model has a 'const' value which is actually an interval in the initial conditions
	 */
	@Test
	public void testParseRangedConstant()
	{
		String path = UNIT_BASEDIR + "const_range/";

		Configuration c = flatten(
				SpaceExImporter.importModels(path + "const_range.cfg", path + "const_range.xml"));

		// const x should be converted to a variable
		BaseComponent bc = (BaseComponent) c.root;

		Assert.assertTrue("x is initially a constant", bc.constants.get("x") != null);
		Assert.assertTrue("x is initially an interval",
				bc.constants.get("x").isConstant() == false);

		// run conversion pass of interval constants -> variables
		ConvertIntervalConstants.run(c);

		Assert.assertTrue("x is no longer a constant", !bc.constants.containsKey("x"));
		Assert.assertTrue("x was converted to a variable", bc.variables.contains("x"));
		Assert.assertTrue("x is no longer a constant in the model", !bc.constants.containsKey("x"));
		Assert.assertTrue("x has dynamics x' == 0", bc.modes.values().iterator().next().flowDynamics
				.get("x").equalsInterval(new Interval(0)));
	}

	/**
	 * Model has a 'const' value which is defined in the model and not in the initial conditions
	 */
	@Test
	public void testParseModelConstants()
	{
		String path = UNIT_BASEDIR + "const_model/";

		try
		{
			flatten(SpaceExImporter.importModels(path + "const_model.cfg",
					path + "const_model.xml"));

			Assert.fail("Exception expected due to undefined constant param in network component");
		}
		catch (AutomatonExportException e)
		{
			// expected
		}
	}

	@Test
	public void testUrgent()
	{
		String path = UNIT_BASEDIR + "urgent/";

		Configuration c = flatten(
				SpaceExImporter.importModels(path + "urgent.cfg", path + "urgent.xml"));
		BaseComponent ha = (BaseComponent) c.root;

		if (!AutomatonUtil.hasUrgentMode(ha))
		{
			Assert.fail("urgent model not imported correctly");
		}
	}

	/**
	 * Composability Test. Test simple network component with two base component
	 */
	@Test
	public void testModelSimpleTwoNetworkComponent()
	{
		String path = UNIT_BASEDIR + "comp_simple_crossprod_network/";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "sys.cfg", path + "sys.xml");

		Configuration c = flatten(sd);
		BaseComponent ha = (BaseComponent) c.root;

		// make sure the cross product is done right, exists 1.1 -> 1.2 and 1.2
		// -> 2.2
		boolean foundFirst = false;
		boolean foundSecond = false;
		boolean foundThird = false;

		for (AutomatonTransition t : ha.transitions)
		{
			if (t.from.name.equals("one_one") && t.to.name.equals("one_two"))
			{
				foundFirst = true;
			}
			else if (t.from.name.equals("one_two") && t.to.name.equals("two_two"))
			{
				foundSecond = true;
			}
			else if (t.from.name.equals("one_one") && t.to.name.equals("two_one"))
			{
				foundThird = true;
			}
		}

		if (!foundFirst)
		{
			Assert.fail("Flattened automaton does not contain trannsition from one_one to one_two");
		}

		if (!foundSecond)
		{
			Assert.fail("Flattened automaton does not contain trannsition from one_two to two_two");
		}

		if (!foundThird)
		{
			Assert.fail("Flattened automaton does not contain trannsition from one_one to two_one");
		}

		if (ha.transitions.size() != 8)
		{
			Assert.fail("Expected 8 transitions in flattened automaton. found: "
					+ ha.transitions.size());
		}
	}

	@Test
	public void testHeaterInstantiation()
	{
		String path = UNIT_BASEDIR + "controller_heater/";
		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(path + "controller_heater.cfg",
				path + "controller_heater.xml");

		// 2. convert the SpaceEx data structures to template automata
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(spaceExDoc);

		BaseComponent controller = (BaseComponent) componentTemplates.get("ControllerTemplate");
		Assert.assertTrue("controller's first transition has a label",
				controller.transitions.get(0).label != null);
		Assert.assertTrue("controller's first transition's label is 'turn_on'",
				controller.transitions.get(0).label.equals("turn_on"));

		BaseComponent heater = (BaseComponent) componentTemplates.get("HeaterTemplate");
		Assert.assertTrue("heaters first transition has a label",
				heater.transitions.get(0).label != null);
		Assert.assertTrue("heaters first transition's label is 'turn_on'",
				heater.transitions.get(0).label.equals("turn_on"));

		ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);
	}

	/**
	 * Test support for local variables in components
	 */
	@Test
	public void testLocalVar()
	{
		String path = UNIT_BASEDIR + "local_vars/";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "sys.cfg", path + "sys.xml");

		Configuration c = flatten(sd);
		BaseComponent ha = (BaseComponent) c.root;

		for (String v : ha.variables)
		{
			if (v.contains("."))
			{
				Assert.fail("variable names can't contain dots");
			}
		}
	}

	@Test
	public void testLocalVarsInstantiation()
	{
		String path = UNIT_BASEDIR + "local_vars/";
		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(path + "sys.cfg",
				path + "sys.xml");

		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(spaceExDoc);

		Assert.assertTrue("two templates were imported", componentTemplates.size() == 2);
		Assert.assertTrue("'template' template got imported",
				componentTemplates.containsKey("template"));
		Assert.assertTrue("system template got imported", componentTemplates.containsKey("system"));

		ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);
	}

	@Test
	public void testToyTemplateImporting()
	{
		String path = UNIT_BASEDIR + "loc_init/";
		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(path + "one_init.cfg",
				path + "model.xml");

		// 2. convert the SpaceEx data structures to template automata
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(spaceExDoc);

		Assert.assertTrue("two templates were imported", componentTemplates.size() == 2);
		Assert.assertTrue("toy template got imported", componentTemplates.containsKey("toy"));
		Assert.assertTrue("system template got imported", componentTemplates.containsKey("system"));

		Assert.assertTrue("system template is not null", componentTemplates.get("system") != null);

		Component c = componentTemplates.get("system");
		Assert.assertTrue("system was a network component", c instanceof NetworkComponent);
		NetworkComponent nc = (NetworkComponent) c;

		// template's instance names are their types
		Assert.assertTrue("template instance name is null", nc.instanceName != null);
		Assert.assertTrue("network component template instance name is 'system'",
				nc.instanceName.equals("system"));
		Assert.assertTrue("base component template instance name is 'toy'",
				componentTemplates.get("toy").instanceName.equals("toy"));

		BaseComponent toy = (BaseComponent) componentTemplates.get("toy");

		Assert.assertTrue("'toy' component has two transitions", toy.transitions.size() == 2);

		Assert.assertTrue("'toy' component's template is itself", toy.template == toy);
		Assert.assertTrue("'system' component's template is itself", nc.template == nc);

		Assert.assertTrue("system template contains a constant named timeout",
				nc.constants.containsKey("timeout"));
		Assert.assertTrue("system template contains a variable named x",
				nc.variables.contains("x"));
		Assert.assertTrue("system template contains 3 variables", nc.variables.size() == 3);

		Assert.assertTrue("system template has one child", nc.children.size() == 1);

		Entry<String, ComponentInstance> e = nc.children.entrySet().iterator().next();
		Assert.assertTrue("system's child is named toy_1 in map", e.getKey().equals("toy_1"));

		ComponentInstance ci = e.getValue();
		Assert.assertTrue("system's child is the toy template automaton", ci.child == toy);

		Assert.assertTrue("system's child has two const mappings", ci.constMapping.size() == 2);
		Assert.assertTrue("system's child has three var mappings", ci.varMapping.size() == 3);

		ComponentMapping mapping = ci.constMapping.get(1);
		Assert.assertTrue("system's child second const mapping childParam is 'tmax'",
				mapping.childParam.equals("tmax"));
		Assert.assertTrue("system's child second const mapping parentParam is 'timeout'",
				mapping.parentParam.equals("timeout"));
	}

	@Test
	public void testToyTemplateInstantiation()
	{
		String path = UNIT_BASEDIR + "loc_init/";
		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(path + "one_init.cfg",
				path + "model.xml");

		// 2. convert the SpaceEx data structures to template automata
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(spaceExDoc);

		// 3. run any component template passes here (future)
		// 4. instantiate the component templates into a networked configuration
		Configuration c = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);

		Assert.assertTrue("first configuration plot variable is t",
				c.settings.plotVariableNames[0].equals("t"));

		Assert.assertTrue("root is a network component", c.root instanceof NetworkComponent);
		NetworkComponent system = (NetworkComponent) c.root;

		Assert.assertTrue("root has one child", system.children.size() == 1);
		BaseComponent toy = (BaseComponent) system.children.values().iterator().next().child;

		Assert.assertTrue("child has two transitions", toy.transitions.size() == 2);
	}

	@Test
	public void testToyFullyQualified()
	{
		String path = UNIT_BASEDIR + "loc_init/";
		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(path + "one_init.cfg",
				path + "model.xml");

		// 2. convert the SpaceEx data structures to template automata
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(spaceExDoc);

		// 3. run any component template passes here (future)
		// 4. instantiate the component templates into a networked configuration
		Configuration c = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);

		FlattenRenameUtils.convertToFullyQualifiedParams(c.root);

		Assert.assertTrue("root is a network component", c.root instanceof NetworkComponent);
		NetworkComponent system = (NetworkComponent) c.root;

		Assert.assertTrue("root has one child", system.children.size() == 1);
		BaseComponent toy = (BaseComponent) system.children.values().iterator().next().child;

		Assert.assertTrue("fully-qualified child (toy) has constant renamed to timeout",
				toy.constants.containsKey("timeout"));

		Assert.assertTrue("child has two transitions", toy.transitions.size() == 2);

		c.root.validate();
	}

	@Test
	public void testToyFlattening()
	{
		String path = UNIT_BASEDIR + "loc_init/";
		Configuration c = flatten(
				SpaceExImporter.importModels(path + "one_init.cfg", path + "model.xml"));

		Assert.assertTrue("Automaton is flattened", c.root instanceof BaseComponent);

		Assert.assertTrue("Single initial mode", c.init.size() == 1);

		BaseComponent bc = (BaseComponent) c.root;
		Assert.assertTrue("Two transitions in flattened automaton", bc.transitions.size() == 2);

		String initMode = "loc1";
		Assert.assertTrue("initial mode is called '" + initMode + "'",
				c.init.containsKey(initMode));

		// make sure the plot variables are set correctly
		Assert.assertTrue("first plot variable is t", c.settings.plotVariableNames[0].equals("t"));
		Assert.assertTrue("second plot bariable is x", c.settings.plotVariableNames[1].equals("x"));
	}

	@Test
	public void testNoLocInInitial()
	{
		// no loc in initial should be okay now (no exception)

		String path = UNIT_BASEDIR + "loc_init/";

		flatten(SpaceExImporter.importModels(path + "no_init.cfg", path + "model.xml"));
	}

	@Test
	public void testImportSpaceEx()
	{

		String path = UNIT_BASEDIR + "loc_init/";
		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(path + "one_init.cfg",
				path + "model.xml");

		SpaceExNetworkComponent net = (SpaceExNetworkComponent) spaceExDoc.getComponent("system");

		Assert.assertTrue("toy bind has five mappings", net.getBind(0).getMapCount() == 5);
	}

	@Test
	public void testAllLocInInitial()
	{
		try
		{
			String path = UNIT_BASEDIR + "loc_init/";
			Configuration c = flatten(
					SpaceExImporter.importModels(path + "all_init.cfg", path + "model.xml"));

			Expression.expressionPrinter = DefaultExpressionPrinter.instance;
			Assert.assertNotEquals(c.init, null);

		}
		catch (AutomatonExportException e)
		{
			throw e;
		}
	}

	/**
	 * For Flow*, models require at least one variable. Thus, printing such models with a tool like
	 * Flow* should raise a precondition error
	 */
	@Test
	public void testModelNoVars()
	{
		String path = UNIT_BASEDIR + "no_vars_check/";

		Configuration c = flatten(
				SpaceExImporter.importModels(path + "discrete.cfg", path + "discrete.xml"));

		// use flow* as an example printer that needs at least one variable,
		// which should raise an error
		ToolPrinter tp = new FlowstarPrinter();

		tp.setOutputNone();

		try
		{
			tp.print(c, "", "");

			Assert.fail("precondition exception not raised for model with no variables");
		}
		catch (PreconditionsFailedException e)
		{
			// expected
		}
	}

	/**
	 * Models with variables should not be rejected
	 */
	@Test
	public void testModelHasVarsAcceptance()
	{
		String path = UNIT_BASEDIR + "no_vars_check/";

		Configuration c = flatten(
				SpaceExImporter.importModels(path + "has_vars.cfg", path + "has_vars.xml"));

		ToolPrinter tp = new FlowstarPrinter();

		tp.setOutputNone();
		tp.print(c, "", "");
	}

	/**
	 * Models with blank forbidden states should be allowed (spaceex includes examples of these,
	 * like heli)
	 */
	@Test
	public void testModelBlankForbidden()
	{
		String path = UNIT_BASEDIR + "blank_forbidden/";

		Configuration c = flatten(SpaceExImporter.importModels(path + "blank_forbidden.cfg",
				path + "blank_forbidden.xml"));

		ToolPrinter tp = new FlowstarPrinter();

		tp.setOutputNone();
		tp.print(c, "", "");
	}

	/**
	 * Test base component with a missing component name in loc(component)= assignment for initial
	 * states This should be an error
	 */
	@Test
	public void testMissingBaseComponent()
	{
		String path = UNIT_BASEDIR + "comp_base/";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "sys.cfg", path + "sys.xml");

		try
		{
			flatten(sd);
			Assert.fail("Exception not thrown");
		}
		catch (AutomatonExportException e)
		{
		}
	}

	/**
	 * Test base component with blank component name in loc()= assignment for initial states
	 */
	@Test
	public void testModelBaseTwoComponent()
	{
		String path = UNIT_BASEDIR + "comp_base/";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "sys2.cfg", path + "sys2.xml");

		Configuration ha = flatten(sd);

		if (!ha.init.containsKey("off"))
		{
			Assert.fail("off mode should be init mode");
		}

		if (ha.init.size() != 1)
		{
			Assert.fail("ONLY off mode should be init mode");
		}
	}

	/**
	 * Composability Test. Test network component with single base component
	 */
	@Test
	public void testModelSingleNetworkComponent()
	{
		String path = UNIT_BASEDIR + "comp_single_network/";

		Configuration c = flatten(SpaceExImporter.importModels(path + "sys.cfg", path + "sys.xml"));

		new SubstituteConstantsPass().runTransformationPass(c, "");

		Assert.assertFalse(
				"init expression shouldn't have redundant '3 = 3': "
						+ c.init.get("on").toDefaultString(),
				c.init.get("on").toDefaultString().contains("3 = 3"));

		ToolPrinter tp = new FlowstarPrinter();

		tp.setOutputNone();
		tp.print(c, "", "");
	}

	/**
	 * Composability Test. Test network component with labels.
	 */
	@Test
	public void testModelLabelledTwoNetworkComponent()
	{
		String path = UNIT_BASEDIR + "controller_heater/";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "controller_heater.cfg",
				path + "controller_heater.xml");

		Configuration c = flatten(sd);
		BaseComponent ha = (BaseComponent) c.root;

		if (ha.transitions.size() != 2)
		{
			Assert.fail("Expected 2 transitions in flattened automaton");
		}

		// make sure the cross product is done right, exists 1.1 -> 1.2 and 1.2
		// -> 2.2
		boolean foundFirst = false;
		boolean foundSecond = false;

		for (AutomatonTransition t : ha.transitions)
		{
			if (t.from.name.equals("heater_off_controller_off")
					&& t.to.name.equals("heater_on_controller_on"))
			{
				foundFirst = true;
			}
			else if (t.from.name.equals("heater_on_controller_on")
					&& t.to.name.equals("heater_off_controller_off"))
			{
				foundSecond = true;
			}
		}

		if (!foundFirst)
		{
			Assert.fail("Flattened automaton does not contain trannsition from off to on");
		}

		if (!foundSecond)
		{
			Assert.fail("Flattened automaton does not contain trannsition from on to off");
		}
	}

	/**
	 * Composability Test. Test network component with labels.
	 */
	@Test
	public void testModelTimedNetworkComponent()
	{
		String path = UNIT_BASEDIR + "controller_heater/";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "timed_controller_heater.cfg",
				path + "controller_heater.xml");

		Configuration c = flatten(sd);
		BaseComponent ha = (BaseComponent) c.root;

		if (ha.transitions.size() != 2)
		{
			Assert.fail("Expected 2 transitions in flattened automaton");
		}

		// make sure the plot variables are right
		Assert.assertTrue(c.settings.plotVariableNames[0].equals("time"));
		Assert.assertTrue(c.settings.plotVariableNames[1].equals("temp"));

		// check that inner constant was extracted correctly
		Assert.assertTrue(ha.constants.containsKey("timer_t_max"));
	}

	@Test
	public void testParseHavocFlow()
	{
		String path = UNIT_BASEDIR + "havoc_flow/";

		try
		{
			flatten(SpaceExImporter.importModels(path + "havoc_flow.cfg", path + "havoc_flow.xml"));
		}
		catch (AutomatonExportException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testParseHavocFlowTransition()
	{
		String path = UNIT_BASEDIR + "havoc_flow_transition/";

		try
		{
			Configuration c = flatten(SpaceExImporter.importModels(
					path + "havoc_flow_transition.cfg", path + "havoc_flow_transition.xml"));

			// it isn't run automatically during flatten since some printers
			// (SpaceEx) can print havoc dynamics directly
			new ConvertHavocFlows().runVanillaPass(c, "");

			BaseComponent ha = (BaseComponent) c.root;

			AutomatonTransition at = ha.transitions.get(0);

			if (at == null)
			{
				Assert.fail("transition is null");
			}

			if (!at.reset.get("range").getInterval().equals(new Interval(2, 3)))
			{
				Assert.fail("range should be set nondeterministically");
			}

		}
		catch (AutomatonExportException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testNondetermAssignment()
	{
		String path = UNIT_BASEDIR + "nondeterm_reset/";

		try
		{
			Configuration c = flatten(SpaceExImporter.importModels(path + "nondeterm_reset.cfg",
					path + "nondeterm_reset.xml"));
			BaseComponent ha = (BaseComponent) c.root;

			AutomatonTransition at = ha.transitions.get(0);

			if (at == null || at.reset == null || at.reset.get("y") == null)
			{
				Assert.fail("reset doesn't exist in transition.");
			}

			if (!at.reset.get("y").getInterval().equals(new Interval(0, 1)))
			{
				Assert.fail("range should be set nondeterministically");
			}

		}
		catch (AutomatonExportException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testUrgentSimple()
	{
		String path = UNIT_BASEDIR + "urgent_simple/";

		SpaceExDocument doc = SpaceExImporter.importModels(path + "urgent_simple.cfg",
				path + "urgent_simple.xml");

		Configuration c = flatten(doc);
		BaseComponent ha = (BaseComponent) c.root;

		if (!AutomatonUtil.hasUrgentMode(ha))
		{
			Assert.fail("urgent model not imported correctly");
		}
	}

	@Test
	public void testUrgentComposition()
	{
		String path = UNIT_BASEDIR + "urgent_composition/";

		Configuration c = flatten(SpaceExImporter.importModels(path + "urgent_composition.cfg",
				path + "urgent_composition.xml"));
		BaseComponent ha = (BaseComponent) c.root;

		if (!AutomatonUtil.hasUrgentMode(ha))
		{
			Assert.fail("urgent_composition model not imported correctly");
		}
	}

	@Test
	public void testUrgentInit()
	{
		String path = UNIT_BASEDIR + "urgent_init/";

		flatten(SpaceExImporter.importModels(path + "urgent_init.cfg", path + "urgent_init.xml"));
	}

	@Test
	public void testMerge()
	{
		String path = UNIT_BASEDIR + "merge/";

		Configuration c1 = flatten(SpaceExImporter.importModels(path + "controller_heater.cfg",
				path + "controller.xml", path + "base_heater.xml"));
		BaseComponent ha1 = (BaseComponent) c1.root;

		if (ha1.transitions.size() != 2)
		{
			Assert.fail("Expected two transitions in merged automata");
		}

		// merge in the other order
		Configuration c2 = flatten(SpaceExImporter.importModels(path + "controller_heater.cfg",
				path + "base_heater.xml", path + "controller.xml"));
		BaseComponent ha2 = (BaseComponent) c2.root;

		if (ha1 == null || !ha1.toString().equals(ha2.toString()))
		{
			Assert.fail("Hybrid automata order on merge shouldn't matter.");
		}
	}

	@Test
	public void testParseHavocVariableFlow()
	{
		String path = UNIT_BASEDIR + "havoc_var_range/";

		try
		{
			flatten(SpaceExImporter.importModels(path + "havoc_var_range.cfg",
					path + "havoc_var_range.xml"));
		}
		catch (AutomatonExportException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testParseHavocVariableEqualFlow()
	{
		String path = UNIT_BASEDIR + "havoc_var_equal/";

		try
		{
			flatten(SpaceExImporter.importModels(path + "havoc_var_equal.cfg",
					path + "havoc_var_equal.xml"));
		}
		catch (AutomatonExportException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testParseHavocVariableEqualRemove()
	{
		String path = UNIT_BASEDIR + "havoc_var_equal_remove/";

		try
		{
			Configuration c = flatten(SpaceExImporter.importModels(
					path + "havoc_var_equal_remove.cfg", path + "havoc_var_equal_remove.xml"));

			BaseComponent ha = (BaseComponent) c.root;

			if (ha.modes.size() != 2)
			{
				Assert.fail("Should have 2 modes (not " + ha.modes.size() + ") after removing one "
						+ "with unrealizable invariant and subsequent states.");
			}
		}
		catch (AutomatonExportException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testParseHavocVariableEqualRemoveConstant()
	{
		String path = UNIT_BASEDIR + "havoc_var_equal_remove2/";

		try
		{
			Configuration c = flatten(SpaceExImporter.importModels(
					path + "havoc_var_equal_remove2.cfg", path + "havoc_var_equal_remove2.xml"));
			BaseComponent ha = (BaseComponent) c.root;

			if (ha.modes.size() != 2)
			{
				Assert.fail("Should have 2 modes (not " + ha.modes.size() + ") after removing one "
						+ "with unequal constants in invartiant.");
			}
		}
		catch (AutomatonExportException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Composability Test with different length transition names.
	 */
	@Test
	public void testModelTwoNetworkDifferentLengthNamesComponent()
	{
		String path = UNIT_BASEDIR + "two_network_diff_names/";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "sys.cfg", path + "sys.xml");

		Configuration c = flatten(sd);
		BaseComponent ha = (BaseComponent) c.root;

		if (ha.transitions.size() != 8)
		{
			Assert.fail("Expected 8 transitions in flattened automaton, found: "
					+ ha.transitions.size());
		}

		// make sure the cross product is done right, exists 1.1 -> 1.2 and 1.2
		// -> 2.2
		boolean foundFirst = false;
		boolean foundSecond = false;

		for (AutomatonTransition t : ha.transitions)
		{
			if (t.from.name.equals("first_second") && t.to.name.equals("second_second"))
			{
				foundFirst = true;
			}
			else if (t.from.name.equals("second_first") && t.to.name.equals("second_second"))
			{
				foundSecond = true;
			}
		}

		if (!foundFirst)
		{
			Assert.fail(
					"Flattened automaton does not contain trannsition from first_second to second_second");
		}

		if (!foundSecond)
		{
			Assert.fail(
					"Flattened automaton does not contain trannsition from second_first to second_second");
		}
	}

	@Test
	public void testOpenIntervalPrintFail()
	{
		// printing models with nondeterministic assignments to open intervals
		// should fail

		String path = UNIT_BASEDIR + "nondeterm_reset_open/";

		try
		{
			Configuration c = flatten(
					SpaceExImporter.importModels(path + "sys.cfg", path + "sys.xml"));

			ToolPrinter tp = new SpaceExPrinter();

			tp.setOutputNone();
			tp.print(c, "", "");

			Assert.fail("AutomatonExportException not thrown");
		}
		catch (AutomatonExportException e)
		{
			// expected
		}
	}

	@Test
	public void testComplexInit()
	{
		// init expression has a non-trivial expression: "timeout = 2 * 10"
		String path = UNIT_BASEDIR + "loc_init/";
		Configuration c = flatten(
				SpaceExImporter.importModels(path + "complex_init.cfg", path + "model.xml"));

		Assert.assertTrue("timeout constant is 20",
				c.root.constants.get("timeout").equals(new Interval(20)));
	}

	@Test
	public void testPrintInitStates()
	{
		// Luan's import test. Init was printing as null when it wasn't null
		String cfgPath = UNIT_BASEDIR + "loc_init/one_init.cfg";
		String xmlPath = UNIT_BASEDIR + "loc_init/model.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration config = com.verivital.hyst.importer.ConfigurationMaker.fromSpaceEx(doc,
				componentTemplates);

		String initString = AutomatonUtil.getMapExpressionString(config.init);

		Assert.assertTrue("init string is not null", initString != null);
	}

	@Test
	public void testInputOutput()
	{
		// test model with input and output variables
		String cfgPath = UNIT_BASEDIR + "comp_in_out/sys.cfg";
		String xmlPath = UNIT_BASEDIR + "comp_in_out/sys.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

		NetworkComponent nc = (NetworkComponent) config.root;

		BaseComponent bcX = (BaseComponent) nc.children.get("out_x_1").child;
		BaseComponent bcY = (BaseComponent) nc.children.get("out_y_1").child;

		Assert.assertEquals("two variables in out_x component", 2, bcX.variables.size());
		Assert.assertEquals("one defined flow in out_x component", 1,
				bcX.modes.values().iterator().next().flowDynamics.size());

		// also test AutomatonUtil.isOutputVariable()
		Assert.assertTrue("x is an output variable of base component 'out_x_1'",
				AutomatonUtil.isOutputVariable(bcX, "x"));
		Assert.assertTrue("y is NOT an output variable of base component 'out_x_1'",
				!AutomatonUtil.isOutputVariable(bcX, "y"));

		Assert.assertTrue("x is NOT an output variable of base component 'out_y_1'",
				!AutomatonUtil.isOutputVariable(bcY, "x"));
		Assert.assertTrue("y is an output variable of base component 'out_y_1'",
				AutomatonUtil.isOutputVariable(bcY, "y"));
	}

	@Test
	public void testInputOutputError()
	{
		// test illegal model with input and output (one of the automata
		// contains a variable that is not defined in all modes)
		String cfgPath = UNIT_BASEDIR + "comp_in_out_mismatch/sys.cfg";
		String xmlPath = UNIT_BASEDIR + "comp_in_out_mismatch/sys.xml";

		try
		{
			SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
			Map<String, Component> componentTemplates = TemplateImporter
					.createComponentTemplates(doc);
			ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

			Assert.fail("Validation exception not raised on invalid input / output automaton");
		}
		catch (AutomatonValidationException e)
		{
			// expected
		}
	}

	@Test
	public void testTripleNestedModelWithRenaming()
	{
		// network component inside another network component, all with renaming
		// & locals at each level
		// test that the names are correctly flattened

		// test illegal model with input and output (one of the automata
		// contains a variable that is not defined in all modes)
		String cfgPath = UNIT_BASEDIR + "three_hier/three_hier.cfg";
		String xmlPath = UNIT_BASEDIR + "three_hier/three_hier.xml";

		flatten(SpaceExImporter.importModels(cfgPath, xmlPath));
	}

	@Test
	public void testSixTank()
	{
		// network component inside another network component, all with renaming
		// & locals at each level
		// this is the 6 tank model

		// test illegal model with input and output (one of the automata
		// contains a variable that is not defined in all modes)
		String cfgPath = UNIT_BASEDIR + "three_hier/tank6.cfg";
		String xmlPath = UNIT_BASEDIR + "three_hier/tank6.xml";

		// 1. import spaceex doc
		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);

		// 2. convert the SpaceEx data structures to template automata
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);

		// 3. run any component template passes here (future)
		// 4. instantiate the component templates into a networked configuration
		Configuration c = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);
		ArrayList<String> originalOrder = new ArrayList<String>();
		originalOrder.addAll(c.root.variables);

		FlattenAutomatonPass.flattenAndOptimize(c);

		Assert.assertEquals("Single mode after flattening", 1,
				((BaseComponent) c.root).modes.size());

		// variable names should remain ordered after flattening
		for (int i = 0; i < 6; ++i)
		{
			String expectedVarName = originalOrder.get(i);
			String varName = c.root.variables.get(i);

			Assert.assertEquals("variable name at index " + i + " was incorrect", expectedVarName,
					varName);
		}
	}

	@Test
	public void testConvertLinearDynamicTwoVars()
	{
		String path = UNIT_BASEDIR + "linear_dynamic/";
		SpaceExDocument test1 = SpaceExImporter.importModels(path + "two_var.cfg",
				path + "two_var.xml");

		Configuration c = flatten(test1);
		BaseComponent ha = (BaseComponent) c.root;

		Classification cls = new Classification();
		Classification.ha = ha;
		cls.setVarID(ha);
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.ha = ha;
		// sp.setVarID(ha);
		AutomatonMode mode = ha.modes.get("running");
		cls.setLinearMatrix(mode);
		String s = sp.convertFlowToAMatrix(mode);
		String result = "[2.0 4.0 ;0.0 -3.0 ;]";
		Assert.assertEquals(s, result);
	}

	@Test
	public void testConvertLinearDynamicThreeVars()
	{
		String path = UNIT_BASEDIR + "linear_dynamic/";
		SpaceExDocument test1 = SpaceExImporter.importModels(path + "three_var.cfg",
				path + "three_var.xml");

		Configuration c = flatten(test1);
		BaseComponent ha = (BaseComponent) c.root;

		Classification cls = new Classification();
		Classification.ha = ha;
		cls.setVarID(ha);
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.ha = ha;
		// sp.setVarID(ha);
		AutomatonMode mode = ha.modes.get("running");
		cls.setLinearMatrix(mode);
		String s = sp.convertFlowToAMatrix(mode);
		String result = "[-1.0 4.0 2.0 ;2.0 -3.0 3.0 ;0.0 2.0 4.0 ;]";
		Assert.assertEquals(s, result);
	}

	@Test
	public void testConvertLinearDynamicOneVar()
	{
		String path = UNIT_BASEDIR + "linear_dynamic/";
		SpaceExDocument test1 = SpaceExImporter.importModels(path + "one_var.cfg",
				path + "one_var.xml");

		Configuration c = flatten(test1);
		BaseComponent ha = (BaseComponent) c.root;

		Classification cls = new Classification();
		Classification.ha = ha;
		cls.setVarID(ha);
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.ha = ha;
		// sp.setVarID(ha);
		AutomatonMode mode = ha.modes.get("running");
		cls.setLinearMatrix(mode);
		String s = sp.convertFlowToAMatrix(mode);
		String result = "[1.0 ;]";
		Assert.assertEquals(s, result);
	}

	@Test
	public void testConvertLinearDynamicTimeHAOneVar()
	{
		String path = UNIT_BASEDIR + "linear_dynamic/";
		SpaceExDocument test1 = SpaceExImporter.importModels(path + "time_flow_one_var.cfg",
				path + "time_flow_one_var.xml");

		Configuration c = flatten(test1);
		BaseComponent ha = (BaseComponent) c.root;

		Classification cls = new Classification();
		Classification.ha = ha;
		cls.setVarID(ha);
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.ha = ha;
		// sp.setVarID(ha);
		AutomatonMode mode = ha.modes.get("running");
		cls.setLinearMatrix(mode);
		String s = sp.convertFlowToAMatrix(mode);
		String result = "[0.0 ;]";
		Assert.assertEquals(s, result);
	}

	@Test
	public void testConvertLinearDynamicTwoVarsOneInput()
	{
		String path = UNIT_BASEDIR + "linear_dynamic/";
		SpaceExDocument test1 = SpaceExImporter.importModels(path + "two_var_one_input.cfg",
				path + "two_var_one_input.xml");

		Configuration c = flatten(test1);
		BaseComponent ha = (BaseComponent) c.root;

		Classification cls = new Classification();
		Classification.ha = ha;
		cls.setVarID(ha);
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.ha = ha;
		// sp.setVarID(ha);
		AutomatonMode mode = ha.modes.get("running");
		cls.setLinearMatrix(mode);

		String A = sp.convertFlowToAMatrix(mode);
		String resultA = "[-1.0 4.0 ;-2.0 -3.0 ;]";
		Assert.assertEquals(resultA, A);
		String B = sp.convertInputToBMatrix(mode);
		String resultB = "[-0.2 2.0 ;]";
		Assert.assertEquals(resultB, B);
	}

	@Test
	public void testClassifySubtract()
	{
		String[][] dy = { { "x", "-x - 2 * y -0.2 * u" }, { "y", "4 * x - 3 * y + 2 * u" },
				{ "u", "1" } };
		// Automaton with three variables ['x', 'y', 'u'] and flows:
		// x' == -x - 2 * y -0.2 * u
		// y' == 4 * x - 3 * y + 2 * u
		// u' == 1
		Configuration c = AutomatonUtil.makeDebugConfiguration(dy);
		BaseComponent ha = ((BaseComponent) c.root);
		AutomatonMode mode = ha.modes.values().iterator().next();

		Classification cls = new Classification();
		Classification.ha = ha;
		cls.setVarID(ha);
		cls.setLinearMatrix(mode);
		double TOL = 1e-9;
		Assert.assertEquals(-1, Classification.linearMatrix[0][0], TOL);
	}

	@Test
	public void testConvertLinearDynamicTwoVarTwoHavocTwoInput()
	{
		String path = UNIT_BASEDIR + "linear_dynamic/";
		SpaceExDocument test1 = SpaceExImporter.importModels(path + "four_var_two_input.cfg",
				path + "four_var_two_input.xml");
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(test1);

		Configuration c = ConfigurationMaker.fromSpaceEx(test1, componentTemplates);

		new FlattenAutomatonPass().runVanillaPass(c, null);
		BaseComponent ha = (BaseComponent) c.root;

		// Configuration c = flatten(test1);
		// BaseComponent ha = (BaseComponent)c.root;
		Classification cls = new Classification();
		Classification.ha = ha;
		cls.setVarID(ha);
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.ha = ha;
		// sp.setVarID(ha);
		AutomatonMode mode = ha.modes.get("running");
		cls.setLinearMatrix(mode);
		// test A matrix
		String A = sp.convertFlowToAMatrix(mode);
		String resultA = "[1.0 4.0 ;0.5 -3.0 ;]";
		Assert.assertEquals(A, resultA);
		// test B matrix
		String B = sp.convertInputToBMatrix(mode);
		String resultB = "[0.5 -2.0 ;-0.2 3.0 ;]";
		Assert.assertEquals(B, resultB);
		// test A matrix
		String C = sp.convertInvToMatrix(mode);
		String resultC = "[1 2.0 ;0 1 ;]";
		Assert.assertEquals(C, resultC);

	}

	@Test
	public void testDisjunctionForbidden()
	{
		// test model with input and output variables
		String cfgPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.cfg";
		String xmlPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

		// [[loc1]] with equation x >= 5
		// [[loc1]] with equation t >= 5
		// [[loc3]] with equation t <= 5
		Assert.assertEquals("two forbidden states", 2, config.forbidden.size());
		String loc1 = config.forbidden.get("loc1").toDefaultString();
		String loc3 = config.forbidden.get("loc3").toDefaultString();
		Assert.assertTrue(loc1.contains("x >= 5"));
		Assert.assertTrue(loc1.contains("t >= 5"));
		Assert.assertEquals(loc3, "t <= 5.0");
	}

	@Test
	public void testHierarchyWithRenamingErrorDetection()
	{
		String cfgPath = UNIT_BASEDIR + "bugfix_local_var_missing/local_var.cfg";
		String xmlPath = UNIT_BASEDIR + "bugfix_local_var_missing/local_var.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);

		try
		{
			flatten(doc);
			Assert.fail("No exception raised but initial states contain unknown variable");
		}
		catch (AutomatonValidationException e)
		{
			// make sure there's a friendly error message
			Assert.assertTrue(e.getLocalizedMessage().contains("Did you mean 'inst.x'?"));
		}
	}

	@Test
	public void testBadInitComponent()
	{
		// model contains an init(X)=Y expression where component X is not an
		// instance in the model

		String cfgPath = UNIT_BASEDIR + "bad_init_comp/bad_init_comp.cfg";
		String xmlPath = UNIT_BASEDIR + "bad_init_comp/bad_init_comp.xml";

		try
		{
			SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
			Map<String, Component> componentTemplates = TemplateImporter
					.createComponentTemplates(doc);
			com.verivital.hyst.importer.ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

			Assert.fail("no exception raised");
		}
		catch (AutomatonExportException e)
		{
			// expected
		}
	}

	@Test
	public void testTte()
	{
		// Test tte benchmark from ARCH16

		String cfgPath = UNIT_BASEDIR + "tte/tte5.cfg";
		String xmlPath = UNIT_BASEDIR + "tte/tte5.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);

		Configuration config = com.verivital.hyst.importer.ConfigurationMaker.fromSpaceEx(doc,
				componentTemplates);

		Assert.assertNotNull(config);

		/*
		 * We need a network printer for this:
		 * 
		 * ToolPrinter printer = new SpaceExPrinter(); printer.setOutputString();
		 * printer.print(config, "", "model.xml");
		 * 
		 * System.out.println(printer.outputString.toString());
		 */
	}

	@Test
	public void testModelWithUncontrolledVars()
	{
		// split harmonic oscialltor, x' = y, y' = -x
		// Model with two base components and two variables, they each control one of them, but use
		// the other

		String cfgPath = UNIT_BASEDIR + "split_ha/split_ha.cfg";
		String xmlPath = UNIT_BASEDIR + "split_ha/split_ha.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);

		Configuration config = com.verivital.hyst.importer.ConfigurationMaker.fromSpaceEx(doc,
				componentTemplates);

		ToolPrinter printer = new FlowstarPrinter();
		printer.setOutputString();
		printer.print(config, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}
}
