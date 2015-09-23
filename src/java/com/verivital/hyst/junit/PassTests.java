package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
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
import com.verivital.hyst.passes.complex.hybridize.HybridizeGridPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeTimeTriggeredPass;

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
	 * make a sample network configuration, which is used in multiple tests
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
	
	/**
	 * make a sample base configuration with a single mode, with x' == 1, and y' == 1
	 * @return the constructed Configuration
	 */
	private static Configuration makeSampleBaseConfiguration()
	{
		BaseComponent ha = new BaseComponent();
		ha.variables.add("x");
		ha.variables.add("y");
		
		Configuration c = new Configuration(ha);
		
		c.settings.plotVariableNames[0] = "x";
		c.settings.plotVariableNames[1] = "y"; 
		c.init.put("running", FormulaParser.parseLoc("x = 0 & y == 0"));
		
		AutomatonMode am1 = ha.createMode("running");
		am1.flowDynamics.put("x", new ExpressionInterval(new Constant(1)));
		am1.flowDynamics.put("y", new ExpressionInterval(new Constant(1)));
		am1.invariant = Constant.TRUE;
		
		c.validate();
		
		return c;
	}
	
	@Test
	public void testSimplifyExpressions()
	{
		Configuration c =  makeSampleNetworkConfiguration();
		
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
	 * An ExpresssionPrinter which prints constants to a certain number of digits after the decimel
	 *
	 */
	class RoundPrinter extends DefaultExpressionPrinter
	{
		public RoundPrinter(int digits)
		{
			super();
			constFormatter.setMaximumFractionDigits(digits);
		}
	}
	
	/**
	 * Test hybridization (grid) pass
	 */
	@Test
	public void testHybridGridPass()
	{
		Configuration c = makeSampleBaseConfiguration();
		BaseComponent ha = (BaseComponent)c.root;
		
		// update dynamics to be 3*t*x+t
		// approximation should be 7.5*x + 5.5*t
		// rest is in notebook
		AutomatonMode am = ha.modes.values().iterator().next();
		am.flowDynamics.put("x", new ExpressionInterval("3*y*x+y"));
		
		String params = "x,y,0,2,0,5,2,5,a";
		new HybridizeGridPass().runTransformationPass(c, params);
		
		Assert.assertEquals("10 modes", 10, ha.modes.size());
		
		AutomatonMode m = ha.modes.get("_m_1_2");
		Assert.assertNotEquals("mode named '_m_1_2 exists'", null, m);
	
		// dynamics should be y' == 7.5*x + 5.5*t + [-12, -10.5]
		Expression.expressionPrinter = new RoundPrinter(3);
		ExpressionInterval ei = m.flowDynamics.get("x");
		Assert.assertEquals("Hybrizied mode x=[1,2], y=[2,3] correctly", "7.5 * x + 5.5 * y + -12 + [0, 1.5]", ei.toString());
		
		Assert.assertEquals("single initial state", c.init.size(), 1);
	}
	
	/**
	 * Test hybridization (time-triggered) pass
	 */
	@Test
	public void testHybridTimeTriggeredPass()
	{
		RoundPrinter rp = new RoundPrinter(4);
		Configuration c = makeSampleBaseConfiguration();
		BaseComponent ha = (BaseComponent)c.root;
		AutomatonMode am = ha.modes.values().iterator().next();
		
		// we're going to follow the example in the powerpoint for this
		ha.variables.remove("y");
		ha.variables.add("c");
		am.flowDynamics.remove("y");
		am.flowDynamics.put("c", new ExpressionInterval("1"));
		am.flowDynamics.put("x", new ExpressionInterval("x^2"));
		am.invariant = FormulaParser.parseInvariant("c <= 1");
		
		c.settings.plotVariableNames[1] = "c";
		c.init.put("running", FormulaParser.parseGuard("x >= 0.24 & x <= 0.26 & c == 0"));
		c.validate();
		
		String params = "0.5,1.0,0.05"; // step time = 0.5, total time = 1.0, epsilon = 0.05
		new HybridizeTimeTriggeredPass().runTransformationPass(c, params);
		
		Assert.assertEquals("3 modes (2 + error)", 3, ha.modes.size());
		Assert.assertEquals("1 initial mode", 1, c.init.size());
		
		AutomatonMode m0 = ha.modes.get("_m_0");
		Assert.assertNotEquals("mode named '_m_0 exists'", null, m0);
		
		// dynamics should be c' == 1
		Expression.expressionPrinter = rp;
		Assert.assertEquals("c' == 1", "1", m0.flowDynamics.get("c").toString());
		
		// dynamics should be approximately x' =.536*x – 0.0718 + [0, 0.0046]
		String correctDynamics = "0.5357 * x + -0.0717 + [0, 0.0046]";
		Assert.assertEquals("mode0.x' == " + correctDynamics, correctDynamics, m0.flowDynamics.get("x").toString());

		AutomatonMode m1 = ha.modes.get("_m_1");
		Assert.assertNotEquals("mode named '_m_1 exists'", null, m1);
		
		// dynamics should be approx x=0.619 * x + -0.0958 + [0, 0.0054]
		correctDynamics = "0.619 * x + -0.0958 + [0, 0.0054]";
		Assert.assertEquals("mode1.x' == " + correctDynamics, correctDynamics, m1.flowDynamics.get("x").toString());

		// invariant c <= 1 should be present in first mode
		// time trigger invariant c <= 0.5 should be present in first mode as well
		// should be c <= 1 & x >= 0.2 & x <= 0.3357 & c <= 0.5
		Assert.assertEquals("mode0 invariant correct", "c <= 1 & x >= 0.2 & x <= 0.3357 & c <= 0.5", m0.invariant.toString());
		
		// mode 1 invariant correct
		// should be c <= 1 & x >= 0.2357 & x <= 0.3833
		Assert.assertEquals("mode1 invariant correct", "c <= 1 & x >= 0.2357 & x <= 0.3833 & c <= 1", m1.invariant.toString());
		
		// error transitions should exist from the first mode at the time trigger
		// error transitions should exist in the first mode due to the hyperrectangle constraints
		int numTransitions = 0; 
		boolean foundTriggerTransition = false;
		boolean foundOobTransition = false;
		
		for (AutomatonTransition at : ha.transitions)
		{
			if (at.from == m0)
			{
				++numTransitions;
				
				if (at.to == m1 && at.guard.toString().equals("c = 0.5"))
					foundTriggerTransition = true;
				
				if (at.to.name.equals("running_error") && at.guard.toString().equals("x >= 0.3357"))
					foundOobTransition = true;
			}
		}
		
		Assert.assertEquals("5 outgoing transitions from mode0", numTransitions, 5);
		Assert.assertTrue("transition exists at time trigger in mode0", foundTriggerTransition);
		Assert.assertTrue("transition to out of bounds error mode exists in mode0", foundOobTransition);
		
		Assert.assertEquals("single forbidden mode", 1, c.forbidden.size());
	}
}
