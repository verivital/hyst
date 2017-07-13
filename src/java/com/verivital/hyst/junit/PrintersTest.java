package com.verivital.hyst.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.generators.DrivetrainGenerator;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.internalpasses.ConvertToStandardForm;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMixedTriggeredPass;
import com.verivital.hyst.printers.DReachPrinter;
import com.verivital.hyst.printers.FlowstarPrinter;
import com.verivital.hyst.printers.HylaaPrinter;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.printers.SpaceExPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.printers.hycreate2.HyCreate2Printer;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * A unit test suite for testing various types of printers. While ModelParserTest focuses on
 * validating that the models are input correctly, this suite instead focuses on exporting models.
 * 
 * @author Stanley Bak
 *
 */
@RunWith(Parameterized.class)
public class PrintersTest
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
	
	private String UNIT_BASEDIR;

	public PrintersTest(boolean block) throws Exception
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

	

	// tools to test here. Each test will run all of these
	private static final ArrayList<ToolPrinter> printers;

	static
	{
		printers = new ArrayList<ToolPrinter>();

		// System.out.println(". PrintersTest.java todo: uncomment all
		// printers");
		addPrinter(new FlowstarPrinter());
		addPrinter(new HyCreate2Printer());
		addPrinter(new DReachPrinter());
		addPrinter(new SpaceExPrinter());
		addPrinter(new PySimPrinter());
		addPrinter(new HylaaPrinter());
	};

	private static void addPrinter(ToolPrinter p)
	{
		printers.add(p);
	}

	/**
	 * Test all the printers defined in the printers array on the passed-in model within the
	 * tests/unit/models/ directory
	 * 
	 * @param baseName
	 *            the name used to construct the directory, and names of the .xml and .cfg files
	 */
	private void runAllPrintersOnModel(String baseName)
	{
		String path = UNIT_BASEDIR + baseName + "/";
		String xml = baseName + ".xml";
		String cfg = baseName + ".cfg";

		runAllPrintersOnModel(path, xml, cfg);
	}

	/**
	 * Test all the printers defined in the printers array on the passed-in model
	 * 
	 * @param path
	 *            the directory path, ends in '/'
	 * @param xmlName
	 *            the name of the xml file in the directory
	 * @param cfgName
	 *            the name of the cfg file in the directory
	 */
	private void runAllPrintersOnModel(String path, String xmlName, String cfgName)
	{
		boolean printedOk = false;

		for (ToolPrinter tp : printers)
		{
			// clear expression printer since no assumptions can be made about
			// it. If null pointer exceptinons are thrown, this means
			// it should have been assigned on printAutomaton()
			Expression.expressionPrinter = null;

			SpaceExDocument sd = SpaceExImporter.importModels(path + cfgName, path + xmlName);
			Configuration c = ModelParserTest.flatten(sd);

			try
			{
				String loadedFilename = "mymodel.xml";

				tp.setOutputNone();

				tp.print(c, "", loadedFilename);

				printedOk = true;
			}
			catch (PreconditionsFailedException e)
			{
				// preconditions error, ignore this model for this printer
			}
		}

		if (!printedOk)
			throw new RuntimeException(
					"No printer successfully printed the model (all precondition checks rejected it): "
							+ xmlName);
	}

	/**
	 * Test all the printers defined in the printers array on the passed-in hybrid automaton
	 * 
	 * @param ha
	 *            the automaton to print
	 */
	private void runAllPrintersOnConfiguration(Configuration config)
	{
		config.validate();
		boolean printedOk = false;

		for (ToolPrinter tp : printers)
		{
			Configuration c = config.copy();

			try
			{
				String loadedFilename = "mymodel.xml";

				tp.setOutputNone();
				tp.print(c, "", loadedFilename);
				printedOk = true;
			}
			catch (PreconditionsFailedException e)
			{
				// preconditions error, ignore this model for this printer
			}
		}

		if (!printedOk)
		{
			System.out.println("Rejected model:\n" + config);
			throw new RuntimeException(
					"No printer successfully printed the model (all precondition checks rejected it)");
		}
	}

	/**
	 * Printers should be able to print a simple model with no errors.
	 */
	@Test
	public void testPrintSimpleModel()
	{
		runAllPrintersOnModel(UNIT_BASEDIR + "no_vars_check/", "has_vars.xml", "has_vars.cfg");
	}

	/**
	 * Printers should be able to print a slightly more complex model
	 */
	@Test
	public void testPrintMoreComplexModel()
	{
		runAllPrintersOnModel("controller_heater");
	}

	/**
	 * Printers should be able to print a model with havoc flows in the init state
	 */
	@Test
	public void testPrintHavocInitFlows()
	{
		runAllPrintersOnModel("havoc_flow");
	}

	@Test
	public void testPrintUrgentSimple()
	{
		runAllPrintersOnModel("urgent_simple");
	}

	@Test
	public void testPrintUrgent()
	{
		runAllPrintersOnModel("urgent_composition");
	}

	/**
	 * Printers should be able to print a model with havoc flows in a state that occurs after a
	 * transition
	 */
	@Test
	public void testPrintHavocTransitionFlows()
	{
		runAllPrintersOnModel("havoc_flow_transition");
	}

	/**
	 * Printers should be able to print a model with nondeterministic assignments and deterministic
	 * flows
	 */
	@Test
	public void testPrintNondeterministicAssignments()
	{
		runAllPrintersOnModel("nondeterm_reset");
	}

	/**
	 * make a sample configuration, which is used in multiple tests
	 * 
	 * @return the constructed Configuration
	 */
	private static Configuration makeSampleConfiguration()
	{
		BaseComponent ha = new BaseComponent();
		Configuration c = new Configuration(ha);

		ha.variables.add("x");
		ha.variables.add("t");
		c.settings.plotVariableNames[0] = "t";
		c.settings.plotVariableNames[1] = "x";
		c.init.put("running", FormulaParser.parseInitialForbidden("x = 0 & t = 0"));

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
	 * Test the conversion of multiple initial modes for use in the Flow* printer
	 */
	@Test
	public void testFlowConvertMultipleInitialModes()
	{
		Configuration c = makeSampleConfiguration();

		// add a second initial mode
		c.init.put("stopped", FormulaParser.parseInitialForbidden("x = 5 & t = 6"));

		FlowstarPrinter.convertInitialStatesToUrgent(c);

		BaseComponent ha = (BaseComponent) c.root;
		AutomatonMode init = ConvertToStandardForm.getInitMode(ha);

		Assert.assertNotNull(init);

		boolean found = false;

		for (AutomatonTransition at : ha.transitions)
		{
			if (at.from == init && at.to.name.equals("stopped"))
			{
				found = true;

				Assert.assertTrue("reset sets x to 5",
						at.guard.toDefaultString().contains("x = 5"));
				Assert.assertTrue("reset sets t to 6",
						at.guard.toDefaultString().contains("t = 6"));
			}
		}

		if (!found)
			Assert.fail("Transition from init to stopped not found");
	}

	/**
	 * The printers should be able to print a hybrid automaton which has interval expressions in the
	 * flow dynamics
	 */
	@Test
	public void testPrintIntervalExpression()
	{
		Configuration c = makeSampleConfiguration();

		BaseComponent ha = (BaseComponent) c.root;
		AutomatonMode am = ha.modes.get("running");
		ExpressionInterval ei = new ExpressionInterval(new Constant(0), new Interval(-1, 1));
		am.flowDynamics.put("x", ei);

		runAllPrintersOnConfiguration(c);
	}

	/**
	 * The preconditions should split a disjunctive condition directly
	 */
	@Test
	public void testDisjunctiveGuardSpaceEx()
	{
		Configuration c = makeSampleConfiguration();

		BaseComponent ha = (BaseComponent) c.root;
		ha.transitions.get(0).guard = FormulaParser.parseGuard("t >= 5 | x >= 7");

		ToolPrinter tp = new SpaceExPrinter();
		tp.setOutputNone();
		tp.print(c, "", "model.xml");
	}

	/**
	 * The preconditions should split a disjunctive condition directly
	 */
	@Test
	public void testDisjunctiveGuard()
	{
		Configuration c = makeSampleConfiguration();

		BaseComponent ha = (BaseComponent) c.root;
		ha.transitions.get(0).guard = FormulaParser.parseGuard("t >= 5 | x >= 7");

		runAllPrintersOnConfiguration(c);
	}

	@Test
	public void testSpaceExHybrizized()
	{
		SpaceExPrinter tp = new SpaceExPrinter();

		String path = UNIT_BASEDIR + "hybridized/hybridized.";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "cfg", path + "xml");
		Configuration c = ModelParserTest.flatten(sd);

		for (String scenario : new String[] { "supp", "stc", "phaver" })
		{
			String loadedFilename = "hybridized.xml";

			tp.setOutputNone();
			tp.scenario = scenario;
			tp.print(c, "", loadedFilename);
		}
	}

	@Test
	public void testHyCreatePowExpression()
	{
		// should use Math.pow, not ^
		String[][] dynamics = { { "y", "t^2" }, { "t", "1" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		ToolPrinter printer = new HyCreate2Printer();
		printer.setOutputString();
		printer.print(c, "", "fakeinput.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
		Assert.assertFalse("found '^' in HyCreate output", out.contains("^"));
		Assert.assertTrue("didn't find 'Math.pow($t, 2.0)' in HyCreate output",
				out.contains("Math.pow($t, 2.0)"));
	}

	@Test
	public void testDisjunctionSpaceExPrint()
	{
		// test model with input and output variables
		String cfgPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.cfg";
		String xmlPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

		ToolPrinter printer = new SpaceExPrinter();
		printer.setOutputString();
		printer.print(config, "", "fakeinput.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);

		String expected = "forbidden = \"loc(fakeinput) == loc1 & (x >= 5.0 | t >= 5.0) "
				+ "| loc(fakeinput) == loc3 & t <= 5.0\"";
		Assert.assertTrue("forbidden is correct (disjunction)", out.contains(expected));
	}

	@Test
	public void testDisjunctionFlowstarPrint()
	{
		// test model with input and output variables
		String cfgPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.cfg";
		String xmlPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

		ToolPrinter printer = new FlowstarPrinter();
		printer.setOutputString();
		printer.print(config, "", "fakeinput.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);

		Assert.assertTrue("standard form _error mode exists", out.contains("_error"));
		Assert.assertFalse("standard form _init mode doesn't exist", out.contains("_init"));
	}

	@Test
	public void testPrintDisjunction()
	{
		// may need to add precondition to convert to standard form
		runAllPrintersOnModel("disjunction_forbidden");
	}

	@Test
	public void testPysimPrint()
	{
		// test model with input and output variables
		String cfgPath = UNIT_BASEDIR + "controller_heater/controller_heater.cfg";
		String xmlPath = UNIT_BASEDIR + "controller_heater/controller_heater.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(config, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	@Test
	public void testPrintHybridized()
	{
		if (!PythonBridge.hasPython())
			return;

		String path = UNIT_BASEDIR + "hybridize_skiperror/";
		SpaceExDocument doc = SpaceExImporter.importModels(path + "vanderpol_althoff.cfg",
				path + "vanderpol_althoff.xml");
		Configuration c = ModelParserTest.flatten(doc);

		// -T 5.5 -S starcorners -delta_tt 0.05 -n_pi 31 -delta_pi 1 -epsilon
		// 0.05 -noerror
		String params = HybridizeMixedTriggeredPass.makeParamString(0.15, "starcorners", 0.05, 31,
				1, 0.05, "basinhopping", true);

		new HybridizeMixedTriggeredPass().runTransformationPass(c, params);

		// try printing to spaceex
		ToolPrinter printer = new SpaceExPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	@Test
	public void testPrintLutModelWithoutPython()
	{
		String cfgPath = UNIT_BASEDIR + "lut_table/lut_table.cfg";
		String xmlPath = UNIT_BASEDIR + "lut_table/lut_table.xml";

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

	public void testConstantInResetRange()
	{
		// model with constant in reset range. There's no good internal
		// representation for this currently
		// suggested fix should be to run substitute constants pass explicitly

		String cfgPath = UNIT_BASEDIR + "reset_with_const/reset_with_const.cfg";
		String xmlPath = UNIT_BASEDIR + "reset_with_const/reset_with_const.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration c = com.verivital.hyst.importer.ConfigurationMaker.fromSpaceEx(doc,
				componentTemplates);

		// print to flow*
		ToolPrinter printer = new FlowstarPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	@Test
	public void testConstantWithLuts()
	{
		// model with constant in reset range. There's no good internal
		// representation for this currently
		// suggested fix should be to run substitute constants pass explicitly

		String cfgPath = UNIT_BASEDIR + "pd_lut_linear/pd_lut_linear.cfg";
		String xmlPath = UNIT_BASEDIR + "pd_lut_linear/pd_lut_linear.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration c = com.verivital.hyst.importer.ConfigurationMaker.fromSpaceEx(doc,
				componentTemplates);

		// print to flow*
		ToolPrinter printer = new FlowstarPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
		Assert.assertTrue("output should not contain constant 'input'", !out.contains("input"));
	}

	@Test
	public void testOuterConditionsFlowstarInvalid()
	{
		if (!PythonBridge.hasPython())
			return;

		// test printing a model with two modes, and initial states in one where
		// x <= -1 and the other x >= 1

		String[][] dynamics = { { "x", "1", "0" }, { "y", "1", "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		BaseComponent ha = ((BaseComponent) c.root);

		AutomatonMode off = ha.createMode("off");
		off.flowDynamics.put("x", new ExpressionInterval(1));
		off.flowDynamics.put("y", new ExpressionInterval(2));
		off.invariant = FormulaParser.parseInvariant("1 <= x <= 2");

		AutomatonMode on = ha.modes.get("on");
		on.invariant = FormulaParser.parseInvariant("-2 <= x <= -1");

		// manually set initial state
		c.init.put("on", FormulaParser.parseInitialForbidden("x ==  -1 * y*y & 1.0 <= y <= 1.1"));
		c.init.put("off", FormulaParser.parseInitialForbidden("x == 1.5 & y == 1"));

		c.validate();

		Assert.assertEquals("two modes", 2, ha.modes.size());
		Assert.assertEquals("two initial states", 2, c.init.size());

		// try to print to Flow*
		FlowstarPrinter fp = new FlowstarPrinter();

		fp.setOutputNone();

		try
		{
			fp.print(c, "", "filename.xml");

			Assert.fail("expected exception from range extraction");
		}
		catch (AutomatonExportException e)
		{
			Assert.assertTrue(
					e.toString().contains("Could not determine constant upper/lower bounds"));
		}
	}

	@Test
	public void testOuterConditionsFlowstarValid()
	{
		if (!PythonBridge.hasPython())
			return;

		// test printing a model with two modes, and initial states in one where
		// x <= -1 and the other x >= 1

		String[][] dynamics = { { "x", "1", "0" }, { "y", "1", "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		BaseComponent ha = ((BaseComponent) c.root);

		AutomatonMode off = ha.createMode("off");
		off.flowDynamics.put("x", new ExpressionInterval(1));
		off.flowDynamics.put("y", new ExpressionInterval(2));
		off.invariant = FormulaParser.parseInvariant("1 <= x <= 2");

		AutomatonMode on = ha.modes.get("on");
		on.invariant = FormulaParser.parseInvariant("-2 <= x <= -1");

		// manually set initial state
		c.init.put("on", FormulaParser
				.parseInitialForbidden("-2 <= x <= -1.5 & x == -1 * y*y & 1.0 <= y <= 1.1"));
		c.init.put("off", FormulaParser.parseInitialForbidden("x == 1.5 & y == 1"));

		c.validate();

		Assert.assertEquals("two modes", 2, ha.modes.size());
		Assert.assertEquals("two initial states", 2, c.init.size());

		// try to print to Flow*
		FlowstarPrinter fp = new FlowstarPrinter();

		fp.setOutputString();

		fp.print(c, "", "filename.xml");

		Assert.assertTrue("init has both mode's x range limits: x in [-2, 1.5]",
				fp.outputString.toString().contains("x in [-2, 1.5]"));

	}

	@Test
	public void testFlowstarStrictIneq()
	{
		if (!PythonBridge.hasPython())
			return;

		// printing a strict inequality '<' in flow* should convert it to a
		// non-strict one '<='

		String[][] dynamics = { { "x", "1", "0" }, { "y", "1", "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		BaseComponent ha = ((BaseComponent) c.root);

		AutomatonMode off = ha.createMode("off");
		off.flowDynamics.put("x", new ExpressionInterval(1));
		off.flowDynamics.put("y", new ExpressionInterval(2));
		off.invariant = Constant.TRUE;

		AutomatonMode on = ha.modes.get("on");

		AutomatonTransition at = ha.createTransition(on, off);
		at.guard = FormulaParser.parseGuard("x < 1");

		c.validate();

		Assert.assertEquals("two modes", 2, ha.modes.size());

		// try to print to Flow*
		FlowstarPrinter fp = new FlowstarPrinter();

		fp.setOutputNone();

		try
		{
			fp.print(c, "", "filename.xml");
			Assert.fail("Expected exception due to strict inequality operator.");
		}
		catch (AutomatonExportException e)
		{
			Assert.assertTrue("exception was due to strict inequality",
					e.toString().contains("doesn't support operator"));
		}
	}

	@Test
	public void testPrintHeli()
	{
		runAllPrintersOnModel("heli_large");
	}

	/**
	 * Hylaa should be able to print the drivetrain model, after python simplification
	 */
	@Test
	public void testHylaaPrintDrivetrain()
	{
		if (!PythonBridge.hasPython())
			return;

		DrivetrainGenerator gen = new DrivetrainGenerator();

		String param = "-theta 2";
		Configuration c = gen.generate(param);

		Assert.assertEquals("12 variables", 12, c.root.variables.size());
		ToolPrinter printer = new HylaaPrinter();
		printer.setOutputString();
		printer.print(c, "-python_simplify", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}

	/**
	 * Hylaa should be able to print the input osciallator model model
	 */
	@Test
	public void testHylaaPrintInputOscillator()
	{
		String path = UNIT_BASEDIR + "input_oscillator/input_oscillator";

		SpaceExDocument sd = SpaceExImporter.importModels(path + ".cfg", path + ".xml");
		Configuration c = ModelParserTest.flatten(sd);

		ToolPrinter printer = new HylaaPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);

		// two error conditions
		String cond1 = "trans.condition_list.append(LinearConstraint([-1, -0], -6.5)) # x >= 6.5";
		String cond2 = "trans.condition_list.append(LinearConstraint([1, 0], -10)) # x <= -10.0";

		Assert.assertTrue("first error condition exists", out.contains(cond1));
		Assert.assertTrue("second error condition exists", out.contains(cond2));
	}

	/**
	 * Hylaa should be able to print the motor w/input model
	 */
	@Test
	public void testHylaaPrintMotor()
	{
		String path = UNIT_BASEDIR + "motor/mcs_8";

		SpaceExDocument sd = SpaceExImporter.importModels(path + ".cfg", path + ".xml");
		Configuration c = ModelParserTest.flatten(sd);

		ToolPrinter printer = new HylaaPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);

		Assert.assertTrue("has error mode in outout", out.contains("error"));
		Assert.assertTrue("has input information in outout", out.contains("set_inputs"));
	}

	/**
	 * Flow* should correctly print time-varying inputs
	 */
	@Test
	public void testFlowstarInputs()
	{
		String path = UNIT_BASEDIR + "simple_inputs/simple_inputs";

		SpaceExDocument sd = SpaceExImporter.importModels(path + ".cfg", path + ".xml");
		Configuration c = ModelParserTest.flatten(sd);

		ToolPrinter printer = new FlowstarPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);

		Assert.assertFalse("inputs in invariants were not removed",
				out.contains("-0.5 - (u1) <= 0"));
		Assert.assertTrue("correctly converted inputs", out.contains("x' = y + [-0.5, 0.5]"));
	}

	@Test
	public void testPysimPrintPdLutLinear()
	{
		String cfgPath = UNIT_BASEDIR + "pd_lut_linear/pd_lut_linear.cfg";
		String xmlPath = UNIT_BASEDIR + "pd_lut_linear/pd_lut_linear.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);

		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(config, "", "model.xml");

		String out = printer.outputString.toString();

		// make sure empty hyperrectangle is not printed
		Assert.assertTrue("some output is printed", out.length() > 10);
		Assert.assertFalse("empty hyperrectangle is not printed",
				out.contains("HyperRectangle([])"));
	}
}
