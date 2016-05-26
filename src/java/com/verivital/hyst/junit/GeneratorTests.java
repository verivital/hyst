package com.verivital.hyst.junit;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.generators.IntegralChainGenerator;
import com.verivital.hyst.generators.NavigationGenerator;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.printers.FlowPrinter;
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
		
		Configuration c = gen.generate("-dims 6");
		
		Assert.assertEquals("six variables", 6, c.root.variables.size());
		
		ToolPrinter printer = new FlowPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");
		
		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}
	
	@Test
	public void testNav()
	{
		NavigationGenerator gen = new NavigationGenerator();
		
		Configuration c = gen.generate("-dims 6");
		
		Assert.assertEquals("six variables", 6, c.root.variables.size());
		
		ToolPrinter printer = new FlowPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");
		
		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}
}
