package com.verivital.hyst.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.matlab.MatlabBridge;
import com.verivital.hyst.passes.complex.OrderReductionPass;
import com.verivital.hyst.printers.SimulinkStateflowPrinter;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;

/**
 * JUnit tests for model generators
 * 
 * @author sbak
 *
 */
@RunWith(Parameterized.class)
public class MatlabTests
{
	public static String UNIT_BASEDIR = PassTests.UNIT_BASEDIR;

	@Before
	public void setUpClass()
	{
		Expression.expressionPrinter = null;
	}

	@Parameters
	public static Collection<Object[]> data()
	{
		System.out.println(
				"MatlabTests.java: WARNING: always blocking matlab until auto-detection is fixed "
						+ "(see https://github.com/verivital/hyst/issues/32 ).");
		return Arrays.asList(new Object[][] { { true } });

		// original
		// return Arrays.asList(new Object[][] { { false }, { true } });
	}

	public MatlabTests(boolean block)
	{
		MatlabBridge.setBlockMatlab(block);
	}

	@Test
	public void testNonSematicStateFlowConverter()
			throws MatlabConnectionException, MatlabInvocationException
	{
		if (!MatlabBridge.hasMatlab())
			return;

		String example_name = "../examples/vanderpol/vanderpol.xml";
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.semantics = "0";
		sp.printProcedure(example_name);
		// TODO: it would be ideal to call via the standard printer, e.g.,
		// sp.print(c, toolParamsString, originalFilename);
	}

	@Test
	public void testSematicStateFlowConverter()
			throws MatlabConnectionException, MatlabInvocationException
	{
		if (!MatlabBridge.hasMatlab())
			return;

		String example_name = "../examples/heaterLygeros/heaterLygeros.xml";
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.semantics = "1";
		sp.printProcedure(example_name);
	}

	@Test
	public void testNetworkStateFlowConverter()
			throws MatlabConnectionException, MatlabInvocationException
	{
		if (!MatlabBridge.hasMatlab())
			return;

		String example_name = "../examples/buck_converter/buck_dcm_vs1.xml";
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.semantics = "0";
		sp.printProcedure(example_name);
	}

	@Test
	public void testOrderReductionpass()
	{
		if (!MatlabBridge.hasMatlab())
			return;

		String path = UNIT_BASEDIR + "order_reduction/";
		System.out.println(path);
		SpaceExDocument doc = SpaceExImporter.importModels(path + "building_full_order.cfg",
				path + "building_full_order.xml");
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);

		Configuration c = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);
		String OrderReductionPassParam = "-reducedOrder 3";

		new OrderReductionPass().runTransformationPass(c, OrderReductionPassParam);
		BaseComponent ha = (BaseComponent) c.root;
		// check variables
		Assert.assertEquals("[x1, x2, x3, y1, time]", ha.variables.toString());
		String flow = "{x1=0.006132 * u1 - 0.00751 * x1 - 5.275 * x2 + 0.0009639 * x3, x2=5.275 * x1 "
				+ "- 0.06453 * u1 - 0.8575 * x2 + 0.09063 * x3, x3=0.0009639 * x1 - 0.0006972 * u1 - 0.09063 * x2 - 0.0001258 * x3, "
				+ "time=1}";
		String invariant = "y1 = 0.0006972 * x3 - 0.06453 * x2 - 0.006132 * x1 && time <= stoptime";

		for (Map.Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			// Check flow and invariant
			Assert.assertEquals(flow, e.getValue().flowDynamics.toString());
			Assert.assertEquals(invariant, e.getValue().invariant.toString());
		}
	}

	/*
	 * @Test public void testLargeModelOrderReductionpass() { String path = UNIT_BASEDIR +
	 * "order_reduction/"; System.out.println(path); SpaceExDocument doc =
	 * SpaceExImporter.importModels( path + "iss_full_model.cfg", path + "iss_full_model.xml"); Map
	 * <String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
	 * 
	 * Configuration c = ConfigurationMaker.fromSpaceEx(doc, componentTemplates); String
	 * OrderReductionPassParam = "10"; try{ new OrderReductionPass().runTransformationPass(c,
	 * OrderReductionPassParam); } catch (RuntimeException e){ System.out.println(
	 * "The order reduction pass is failed" ); throw e; }
	 * 
	 * }
	 */
}
