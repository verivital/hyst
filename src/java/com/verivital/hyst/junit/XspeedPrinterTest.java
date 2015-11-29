package com.verivital.hyst.junit;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
package com.verivital.hyst.junit;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.printers.XspeedPrinter;
import com.verivital.hyst.printers.FlowPrinter;
import com.verivital.hyst.printers.SpaceExPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * A unit test suite for testing various types of printers. While ModelParserTest focuses on validating that
 * the models are input correctly, this suite instead focuses on exporting models.
 * 
 * @author Stanley Bak
 *
 */
public class XspeedPrinterTest
{
	@Before 
	public void setUpClass() 
	{      
	    Expression.expressionPrinter = null;
	}
	
	private String UNIT_BASEDIR = "tests/unit/XspeedModel/";
	private String REGRESSION_BASEDIR = "tests/regression/models/";
	
	// tools to test here. Each test will run all of these
	private static final ArrayList <ToolPrinter> printers; 
	
	static 	
	{
		printers = new ArrayList <ToolPrinter>();
		
		//System.out.println(". PrintersTest.java todo: uncomment all printers");
		addPrinter(new XspeedPrinter());
	};
	
	private static void addPrinter(ToolPrinter p)
	{			
		printers.add(p);
	}

	/**
	 * Test all the printers defined in the printers array on the passed-in model within the
	 * tests/unit/models/ directory
	 * @param baseName the name used to construct the directory, and names of the .xml and .cfg files
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
	 * @param path the directory path, ends in '/'
	 * @param xmlName the name of the xml file in the directory
	 * @param cfgName the name of the cfg file in the directory
	 */
	private void runAllPrintersOnModel(String path, String xmlName, String cfgName)
	{
		boolean printedOk = false;
		
		for (ToolPrinter tp : printers)
		{
			// clear expression printer since no assumptions can be made about it. If null pointer exceptinons are thrown, this means
			// it should have been assigned on printAutomaton()
			Expression.expressionPrinter = null;
			
			SpaceExDocument sd = SpaceExImporter.importModels(
					path + cfgName,
					path + xmlName);
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
			throw new RuntimeException("No printer successfully printed the model (all precondition checks rejected it): " + xmlName);
	}
	
	/**
	 * Test all the printers defined in the printers array on the passed-in hybrid automaton
	 * @param ha the automaton to print
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
			throw new RuntimeException("No printer successfully printed the model (all precondition checks rejected it)");
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
	
	
	private static Configuration makeSampleConfiguration()
	{
		BaseComponent ha = new BaseComponent();
		Configuration c = new Configuration(ha);
		
		ha.variables.add("x");
		ha.variables.add("t");
		c.settings.plotVariableNames[0] = "t";
		c.settings.plotVariableNames[1] = "x"; 
		c.init.put("running", FormulaParser.parseLoc("x = 0 & t = 0"));
		
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
	
	
	public void testPrintIntervalExpression()
	{
		Configuration c = makeSampleConfiguration();
		
		BaseComponent ha = (BaseComponent)c.root;
		AutomatonMode am = ha.modes.get("running");
		ExpressionInterval ei = new ExpressionInterval(new Constant(0), new Interval(-1, 1));
		am.flowDynamics.put("x", ei);
		
		runAllPrintersOnConfiguration(c);
	}

	/**
	 * The preconditions should split a disjunctive condition directly
	 */
	@Test
	public void testDisjunctiveGuard()
	{
		Configuration c = makeSampleConfiguration();
		
		BaseComponent ha = (BaseComponent)c.root;
		ha.transitions.get(0).guard = FormulaParser.parseGuard("t >= 5 | x >= 7");
		
		runAllPrintersOnConfiguration(c);
	}
	
    
}
