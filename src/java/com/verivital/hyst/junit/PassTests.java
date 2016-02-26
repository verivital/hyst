package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.ComponentMapping;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.matlab.MatlabBridge;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.passes.complex.OrderReductionPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeGridPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeMixedTriggeredPass;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.RangeExtractor;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * JUnit tests for transformation passes
 * 
 * @author sbak, ttj
 *
 */
@RunWith(Parameterized.class)
public class PassTests {
	@Before
	public void setUpClass() {
		Expression.expressionPrinter = null;
	}

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] { { false }, { true } });
	}

	public PassTests(boolean block) {
		PythonBridge.setBlockPython(block);
	}

	private String UNIT_BASEDIR = "tests/unit/models/";

	/**
	 * make a sample network configuration, which is used in multiple tests
	 * 
	 * @return the constructed Configuration
	 */
	private static Configuration makeSampleNetworkConfiguration() {
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
	public void testSimplifyExpressions() {
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
	public void testSubConstantsPll() {
		String path = UNIT_BASEDIR + "pll/";
		String spaceExFile = path + "pll_orig.xml";
		String configFile = path + "pll_orig.cfg";

		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(configFile, spaceExFile);

		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(spaceExDoc);
		Configuration config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);

		new SubstituteConstantsPass().runTransformationPass(config, null);
		new SimplifyExpressionsPass().runTransformationPass(config, null);
	}

	@Test
	public void testOrderReductionpass() {
		if (!MatlabBridge.hasMatlab())
			return;
		
		String path = UNIT_BASEDIR + "order_reduction/";
		System.out.println(path);
		SpaceExDocument doc = SpaceExImporter.importModels(path + "building_full_order.cfg",
				path + "building_full_order.xml");
		Map<String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);

		Configuration c = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);
		String OrderReductionPassParam = "3";

		new OrderReductionPass().runTransformationPass(c, OrderReductionPassParam);
		BaseComponent ha = (BaseComponent) c.root;
		// check variables
		Assert.assertEquals("[x1, x2, x3, y1, time]", ha.variables.toString());
		String flow = "{x1=0.006132 * u1 - 0.00751 * x1 - 5.275 * x2 + 0.0009639 * x3, x2=5.275 * x1 "
				+ "- 0.06453 * u1 - 0.8575 * x2 + 0.09063 * x3, x3=0.0009639 * x1 - 0.0006972 * u1 - 0.09063 * x2 - 0.0001258 * x3, "
				+ "time=1}";
		String invariant = "y1 = 0.0006972 * x3 - 0.06453 * x2 - 0.006132 * x1 && time <= stoptime";

		for (Map.Entry<String, AutomatonMode> e : ha.modes.entrySet()) {
			// Check flow and invariant
			Assert.assertEquals(flow, e.getValue().flowDynamics.toString());
			Assert.assertEquals(invariant, e.getValue().invariant.toString());
		}
	}

	/*
	@Test
	public void testLargeModelOrderReductionpass()
	{
		String path = UNIT_BASEDIR + "order_reduction/";
		System.out.println(path);
		SpaceExDocument doc = SpaceExImporter.importModels(
				path + "iss_full_model.cfg",
				path + "iss_full_model.xml");
                Map <String, Component> componentTemplates = TemplateImporter.createComponentTemplates(doc);
		
		Configuration c = ConfigurationMaker.fromSpaceEx(doc, componentTemplates);
                String OrderReductionPassParam = "10";
		try{
                    new OrderReductionPass().runTransformationPass(c, OrderReductionPassParam);
                }
                catch (RuntimeException e){
                    System.out.println("The order reduction pass is failed" );
                    throw e;
                }       
                        
	}*/

}
