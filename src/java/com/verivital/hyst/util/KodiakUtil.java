package com.verivital.hyst.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.main.Hyst;

/**
 * Kodaik is a NASA tool for finding enclosures of solutions for bounded nonlinear equations. It is
 * run as a separate executable, expected to be on the PATH or KODIAK_PATH
 * 
 */

public class KodiakUtil
{
	private static KodiakExpressionPrinter printer = new KodiakExpressionPrinter();

	/**
	 * Optimize a list of functions in a hyper-rectangle using kodiak This requires the Kodiac
	 * executable is on PATH or KODIAK_PATH, or null is returned
	 *
	 * @param pb
	 *            the PythonBridge interface to use
	 * @param exp
	 *            the expression to minimize and maximize
	 * @param boundsList
	 *            a list of interval bounds for each variable used in the expression
	 * @return a list of resultant interval bounds
	 */
	public static List<Interval> kodiakOptimize(List<Expression> exps,
			List<HashMap<String, Interval>> boundsList)
	{
		List<Interval> rv = null;

		// This splits problems into 30 optimizations each to get around
		// kodiac's long-input bug
		List<Expression> subExps = new ArrayList<Expression>();
		List<HashMap<String, Interval>> subBounds = new ArrayList<HashMap<String, Interval>>();

		final int OPT_PER_CALL = 30;
		int index = 0;

		while (index < exps.size())
		{
			subExps.add(exps.get(index));
			subBounds.add(boundsList.get(index));

			++index;

			if (index == exps.size() || index % OPT_PER_CALL == 0)
			{
				List<Interval> result = kodiakOptimizeSingle(subExps, subBounds);

				if (result == null) // kodiak executable not found
					break;

				if (rv == null)
					rv = result;
				else
					rv.addAll(result);

				// run opt and add to rv
				subExps.clear();
				subBounds.clear();
			}
		}

		return rv;
	}

	private static List<Interval> kodiakOptimizeSingle(List<Expression> exps,
			List<HashMap<String, Interval>> boundsList)
	{
		List<Interval> rv = null;
		Process p = null;

		try
		{
			String inputFilename = makeInputFile(exps, boundsList);
			p = openProcess(inputFilename);

			if (p != null)
			{
				BufferedReader stdout = new BufferedReader(
						new InputStreamReader(p.getInputStream()));

				try
				{
					rv = parseOutput(stdout);
				}
				finally
				{
					closeProcess(p);
				}
			}
		}
		catch (IOException e)
		{
			throw new AutomatonExportException("Error making Kodiak input file: " + e.toString(),
					e);
		}

		if (rv != null && rv.size() != exps.size())
			throw new AutomatonExportException("kodiak output size (" + rv.size()
					+ ") didn't match input size (" + exps.size() + ")");

		return rv;
	}

	private static List<Interval> parseOutput(BufferedReader stdout) throws IOException
	{
		List<Interval> rv = new ArrayList<Interval>();

		for (String line = stdout.readLine(); line != null; line = stdout.readLine())
		{
			// outclosure: [-0.24862617, 0.029801058]

			if (line.contains("Error"))
				throw new AutomatonExportException("Error while running Kodiak: " + line);

			String prefix = "outclosure: [";
			if (line.startsWith(prefix))
			{
				line = line.substring(prefix.length(), line.length() - 1);

				String[] parts = line.split(",");
				double min = Double.parseDouble(parts[0]);
				double max = Double.parseDouble(parts[1]);

				rv.add(new Interval(min, max));
			}
		}

		return rv;
	}

	private static String makeInputFile(List<Expression> exps,
			List<HashMap<String, Interval>> boundsList) throws IOException
	{
		File f = File.createTempFile("hyst_kodiak", ".kdk");
		// File f = new File("/home/stan/Desktop/temp/kodiak/hyst_kodiak.kdk");

		BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsolutePath()));

		for (int index = 0; index < exps.size(); ++index)
		{
			Expression exp = exps.get(index);
			HashMap<String, Interval> bounds = boundsList.get(index);

			bw.write("reset;\n");
			bw.write("set safe input = false;\n");
			bw.write("set precision = -10;\n");
			bw.write("set bp = true;\n");
			bw.write("set resolution = 0.00001;\n\n");

			for (Entry<String, Interval> e : bounds.entrySet())
			{
				String var = e.getKey();
				Interval i = e.getValue();

				bw.write("var " + var + " in [approx(" + i.min + "), approx(" + i.max + ")];\n");
			}

			bw.write("\nobjfn " + printer.print(exp) + ";\n");
			bw.write("minmax;\n\n");
		}

		bw.close();
		return f.getAbsolutePath();
	}

	private static Process openProcess(String inputFilename)
	{
		Process process = null;
		String processNames[] = { "kodiak", "kodiak.exe" };
		final String ENV_VAR = "KODIAK_PATH";
		String loc = null;

		for (String processName : processNames)
		{
			try
			{
				loc = FileOperations.locate(processName, processName, ENV_VAR);
				Hyst.log("Using kodiak process at path: " + loc);
				break;
			}
			catch (FileNotFoundException e)
			{
				Hyst.log(e.getMessage());
			}
		}

		if (loc != null)
		{
			ProcessBuilder pb = new ProcessBuilder(loc, inputFilename);
			pb.directory(new File(System.getProperty("java.io.tmpdir")));

			try
			{
				process = pb.start();
			}
			catch (IOException e)
			{
				throw new AutomatonExportException(
						"Exception while starting kodiak process: " + e.toString(), e);
			}
		}

		return process;
	}

	private static void closeProcess(Process process)
	{
		try
		{
			process.getInputStream().close();
			process.getOutputStream().close();
			process.getErrorStream().close();
		}
		catch (IOException e)
		{
		}

		process.destroy();
		try
		{
			process.waitFor();
		}
		catch (InterruptedException e)
		{
		}

		process = null;
	}

	public static class KodiakExpressionPrinter extends DefaultExpressionPrinter
	{
		@Override
		protected String printConstantValue(double d)
		{
			String p = super.printConstantValue(d);

			return "approx(" + p + ")";
		}
	}
}
