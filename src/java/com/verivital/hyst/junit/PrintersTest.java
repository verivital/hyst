package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.internalpasses.ConvertToStandardForm;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.matlab.MatlabBridge;
import com.verivital.hyst.printers.DReachPrinter;
import com.verivital.hyst.printers.FlowPrinter;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.printers.SimulinkStateflowPrinter;
import com.verivital.hyst.printers.SpaceExPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.printers.hycreate2.HyCreate2Printer;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;

/**
 * A unit test suite for testing various types of printers. While
 * ModelParserTest focuses on validating that the models are input correctly,
 * this suite instead focuses on exporting models.
 * 
 * @author Stanley Bak
 *
 */
public class PrintersTest {
	@Before
	public void setUpClass() {
		Expression.expressionPrinter = null;
	}

	private String UNIT_BASEDIR = "tests/unit/models/";

	// tools to test here. Each test will run all of these
	private static final ArrayList<ToolPrinter> printers;

	static {
		printers = new ArrayList<ToolPrinter>();

		// System.out.println(". PrintersTest.java todo: uncomment all
		// printers");
		addPrinter(new FlowPrinter());
		addPrinter(new HyCreate2Printer());
		addPrinter(new DReachPrinter());
		addPrinter(new SpaceExPrinter());
	};

	private static void addPrinter(ToolPrinter p) {
		printers.add(p);
	}

	/**
	 * Test all the printers defined in the printers array on the passed-in
	 * model within the tests/unit/models/ directory
	 * 
	 * @param baseName
	 *            the name used to construct the directory, and names of the
	 *            .xml and .cfg files
	 */
	private void runAllPrintersOnModel(String baseName) {
		String path = UNIT_BASEDIR + baseName + "/";
		String xml = baseName + ".xml";
		String cfg = baseName + ".cfg";

		runAllPrintersOnModel(path, xml, cfg);
	}

	/**
	 * Test all the printers defined in the printers array on the passed-in
	 * model
	 * 
	 * @param path
	 *            the directory path, ends in '/'
	 * @param xmlName
	 *            the name of the xml file in the directory
	 * @param cfgName
	 *            the name of the cfg file in the directory
	 */
	private void runAllPrintersOnModel(String path, String xmlName, String cfgName) {
		boolean printedOk = false;

		for (ToolPrinter tp : printers) {
			// clear expression printer since no assumptions can be made about
			// it. If null pointer exceptinons are thrown, this means
			// it should have been assigned on printAutomaton()
			Expression.expressionPrinter = null;

			SpaceExDocument sd = SpaceExImporter.importModels(path + cfgName, path + xmlName);
			Configuration c = ModelParserTest.flatten(sd);

			try {
				String loadedFilename = "mymodel.xml";

				tp.setOutputNone();

				tp.print(c, "", loadedFilename);

				printedOk = true;
			} catch (PreconditionsFailedException e) {
				// preconditions error, ignore this model for this printer
			}
		}

		if (!printedOk)
			throw new RuntimeException(
					"No printer successfully printed the model (all precondition checks rejected it): " + xmlName);
	}

	/**
	 * Test all the printers defined in the printers array on the passed-in
	 * hybrid automaton
	 * 
	 * @param ha
	 *            the automaton to print
	 */
	private void runAllPrintersOnConfiguration(Configuration config) {
		config.validate();
		boolean printedOk = false;

		for (ToolPrinter tp : printers) {
			Configuration c = config.copy();

			try {
				String loadedFilename = "mymodel.xml";

				tp.setOutputNone();
				tp.print(c, "", loadedFilename);
				printedOk = true;
			} catch (PreconditionsFailedException e) {
				// preconditions error, ignore this model for this printer
			}
		}

		if (!printedOk) {
			System.out.println("Rejected model:\n" + config);
			throw new RuntimeException(
					"No printer successfully printed the model (all precondition checks rejected it)");
		}
	}

	/**
	 * Printers should be able to print a simple model with no errors.
	 */
	@Test
	public void testPrintSimpleModel() {
		runAllPrintersOnModel(UNIT_BASEDIR + "no_vars_check/", "has_vars.xml", "has_vars.cfg");
	}

	/**
	 * Printers should be able to print a slightly more complex model
	 */
	@Test
	public void testPrintMoreComplexModel() {
		runAllPrintersOnModel("controller_heater");
	}

	/**
	 * Printers should be able to print a model with havoc flows in the init
	 * state
	 */
	@Test
	public void testPrintHavocInitFlows() {
		runAllPrintersOnModel("havoc_flow");
	}

	@Test
	public void testPrintUrgentSimple() {
		runAllPrintersOnModel("urgent_simple");
	}

	@Test
	public void testPrintUrgent() {
		runAllPrintersOnModel("urgent_composition");
	}

	/**
	 * Printers should be able to print a model with havoc flows in a state that
	 * occurs after a transition
	 */
	@Test
	public void testPrintHavocTransitionFlows() {
		runAllPrintersOnModel("havoc_flow_transition");
	}

	/**
	 * Printers should be able to print a model with nondeterministic
	 * assignments and deterministic flows
	 */
	@Test
	public void testPrintNondeterministicAssignments() {
		runAllPrintersOnModel("nondeterm_reset");
	}

	/**
	 * make a sample configuration, which is used in multiple tests
	 * 
	 * @return the constructed Configuration
	 */
	private static Configuration makeSampleConfiguration() {
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
	 * Test the conversion of multiple initial modes for use in the Flow*
	 * printer
	 */
	@Test
	public void testFlowConvertMultipleInitialModes() {
		Configuration c = makeSampleConfiguration();

		// add a second initial mode
		c.init.put("stopped", FormulaParser.parseInitialForbidden("x = 5 & t = 6"));
		
		FlowPrinter.convertInitialStatesToUrgent(c);
		
		BaseComponent ha = (BaseComponent) c.root;
		AutomatonMode init = ConvertToStandardForm.getInitMode(ha);
		
		Assert.assertNotNull(init);
		
		boolean found = false;

		for (AutomatonTransition at : ha.transitions) {
			if (at.from == init && at.to.name.equals("stopped")) {
				found = true;

				Assert.assertTrue("reset sets x to 5", at.guard.toDefaultString().contains("x = 5"));
				Assert.assertTrue("reset sets t to 6", at.guard.toDefaultString().contains("t = 6"));
			}
		}

		if (!found)
			Assert.fail("Transition from init to stopped not found");
	}

	/**
	 * The printers should be able to print a hybrid automaton which has
	 * interval expressions in the flow dynamics
	 */
	@Test
	public void testPrintIntervalExpression() {
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
	public void testDisjunctiveGuard() {
		Configuration c = makeSampleConfiguration();

		BaseComponent ha = (BaseComponent) c.root;
		ha.transitions.get(0).guard = FormulaParser.parseGuard("t >= 5 | x >= 7");

		runAllPrintersOnConfiguration(c);
	}

	@Test
	public void testSpaceExHybrizized() {
		ToolPrinter tp = new SpaceExPrinter();

		String path = UNIT_BASEDIR + "hybridized/hybridized.";

		SpaceExDocument sd = SpaceExImporter.importModels(path + "cfg", path + "xml");
		Configuration c = ModelParserTest.flatten(sd);

		for (String scenario : new String[] { "supp", "stc", "phaver" }) {
			String loadedFilename = "hybridized.xml";

			tp.setOutputNone();
			tp.print(c, "scenario=" + scenario, loadedFilename);
		}
	}

	@Test
	public void testNonSematicStateFlowConverter() throws MatlabConnectionException, MatlabInvocationException {
		if (!MatlabBridge.hasMatlab())
			return;
		
		String example_name = "../examples/vanderpol/vanderpol.xml";
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.setToolParamsString("semantics=0");
		sp.printProcedure(example_name);
		// TODO: it would be ideal to call via the standard printer, e.g.,
		// sp.print(c, toolParamsString, originalFilename);
	}

	@Test
	public void testSematicStateFlowConverter() throws MatlabConnectionException, MatlabInvocationException {
		if (!MatlabBridge.hasMatlab())
			return;
		
		String example_name = "../examples/heaterLygeros/heaterLygeros.xml";
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.setToolParamsString("semantics=1");
		sp.printProcedure(example_name);
	}

	@Test
	public void testNetworkStateFlowConverter() throws MatlabConnectionException, MatlabInvocationException {
		if (!MatlabBridge.hasMatlab())
			return;
		
		String example_name = "../examples/buck_converter/buck_dcm_vs1.xml";
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.setToolParamsString("semantics=0");
		sp.printProcedure(example_name);
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
		Assert.assertTrue("didn't find 'Math.pow($t, 2)' in HyCreate output", 
				out.contains("Math.pow($t, 2)"));
	}
	
	@Test
	public void testDisjunctionSpaceExPrint()
	{
		// test model with input and output variables
		String cfgPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.cfg";
		String xmlPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc,
				componentTemplates);
		
		ToolPrinter printer = new SpaceExPrinter();
		printer.setOutputString();
		printer.print(config, "", "fakeinput.xml");
		
		String out = printer.outputString.toString();
		
		Assert.assertTrue("some output exists", out.length() > 10);
		
		String expected = "forbidden = \"loc(fakeinput) == loc1 & (x >= 5 | t >= 5) "
				+ "| loc(fakeinput) == loc3 & t <= 5\"";
		Assert.assertTrue("forbidden is correct (disjunction)", out.contains(expected));
	}
	
	@Test
	public void testDisjunctionFlowstarPrint()
	{
		// test model with input and output variables
		String cfgPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.cfg";
		String xmlPath = UNIT_BASEDIR + "disjunction_forbidden/disjunction_forbidden.xml";

		SpaceExDocument doc = SpaceExImporter.importModels(cfgPath, xmlPath);
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc,
				componentTemplates);
		
		ToolPrinter printer = new FlowPrinter();
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
		Map<String, Component> componentTemplates = TemplateImporter
				.createComponentTemplates(doc);
		Configuration config = ConfigurationMaker.fromSpaceEx(doc,
				componentTemplates);
		
		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(config, "", "model.xml");
		
		String out = printer.outputString.toString();
		
		Assert.assertTrue("some output exists", out.length() > 10);
		
		//System.out.println(out);
	}
}
