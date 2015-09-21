package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.passes.complex.ContinuizationPass;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * JUnit tests for transformation passes
 * @author sbak
 *
 */
public class PassTests
{
	@Before 
	public void setUpClass() 
	{      
	    Expression.expressionPrinter = null;
	}
	
	private String UNIT_BASEDIR = "tests/unit/models/";
	private String REGRESSION_BASEDIR = "tests/regression/models/";
	 
	/**
	 * make a sample configuration, which is used in multiple tests
	 * @return the constructed Configuration
	 */
	private static Configuration makeSampleConfiguration()
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
		c.init.put("running", FormulaParser.parseLoc("x = 0 & t == 0"));
		
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
		Configuration c =  makeSampleConfiguration();
		
		NetworkComponent nc = (NetworkComponent)c.root;
		BaseComponent bc = (BaseComponent)nc.children.values().iterator().next().child;
		
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
		
		Map <String, Component> componentTemplates = TemplateImporter.createComponentTemplates(spaceExDoc);
		Configuration config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);
		
		new SubstituteConstantsPass().runTransformationPass(config, null);
		new SimplifyExpressionsPass().runTransformationPass(config, null);
	}
	
	public List <PassRun> makePassList()
	{
		ArrayList <PassRun> rv = new ArrayList <PassRun>();
		
		String path = REGRESSION_BASEDIR + "continuization/";
		String spaceExFile = path + "cont_approx.xml";
		String configFile = path + "cont_approx.cfg";
		
		SpaceExDocument spaceExDoc = SpaceExImporter.importModels(configFile, spaceExFile);
		Map <String, Component> componentTemplates = TemplateImporter.createComponentTemplates(spaceExDoc);
		Configuration config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);
				
			/*{
				new PseudoInvariantPass(),
				new PseudoInvariantSimulatePass(),
				new TimeScalePass(),
				new SubstituteConstantsPass(),
				new SimplifyExpressionsPass(),
				new SplitDisjunctionGuardsPass(),
				new RemoveSimpleUnsatInvariantsPass(),
				new ShortenModeNamesPass(),
				new RegularizePass(),
				new ContinuizationPass(),
				new HybridizePass(),
				new FlattenAutomatonPass(),
			};*/
		
		String continuizationParam = "-var a -period 0.05 -times 5 -timevar t -bloats 0.1";
		rv.add(new PassRun(new ContinuizationPass(), continuizationParam, config.copy()));
		
		return rv;
	}
	
	@Test
	public void testPassesRun()
	{
		for (PassRun pr : makePassList())
		{
			pr.run();
		}
	}
	
	@Test
	/**
	 * A null-pointer exception is thrown if the printer tries to use expressionPrinter without setting it
	 */
	public void testPassesNoPrinterAssumption()
	{
		for (PassRun pr : makePassList())
		{
			Expression.expressionPrinter = null;
			pr.run();
		}
	}
	
	private class PassRun
	{
		public TransformationPass tp;
		public String params;
		public Configuration config;
		
		public PassRun(TransformationPass pass, String params, Configuration c)
		{
			tp = pass;
			this.params = params;
			this.config = c;
		}
		
		public void run()
		{
			tp.runTransformationPass(config, params);
		}
	}
	
	/**
	 * Test hybridization pass
	 */
	@Test
	public void testHybridizationPass()
	{
		Configuration c =  makeSampleConfiguration();
		
		System.out.println("Todo: make this test");
		// steps: remove 'stopped' mode
		// update dynamics to be 3*y*x+y
		// approximation sohuld be 7.5*x + 5.5*y
		// rest is in notebook
		Assert.fail("this test needs to be written");
		
		Assert.fail("Also test that there is a single initial mode (initial mode trimming as Pradyot had it)");
	}
}
