package com.verivital.hyst.main;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.importer.ConfigurationMaker;
import com.verivital.hyst.importer.SpaceExImporter;
import com.verivital.hyst.importer.TemplateImporter;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.AddIdentityResetPass;
import com.verivital.hyst.passes.basic.RemoveSimpleUnsatInvariantsPass;
import com.verivital.hyst.passes.basic.ShortenModeNamesPass;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.basic.SplitDisjunctionGuardsPass;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.passes.basic.TimeScalePass;
import com.verivital.hyst.passes.complex.PseudoInvariantPass;
import com.verivital.hyst.passes.complex.PseudoInvariantSimulatePass;
import com.verivital.hyst.passes.complex.RegularizePass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeGridPass;
import com.verivital.hyst.passes.complex.hybridize.HybridizeTimeTriggeredPass;
import com.verivital.hyst.passes.flatten.FlattenAutomatonPass;
import com.verivital.hyst.printers.DReachPrinter;
import com.verivital.hyst.printers.FlowPrinter;
import com.verivital.hyst.printers.HyCompPrinter;
import com.verivital.hyst.printers.LayoutPrinter;
import com.verivital.hyst.printers.PythonQBMCPrinter;
import com.verivital.hyst.printers.SMTPrinter;
import com.verivital.hyst.printers.SpaceExPrinter;
import com.verivital.hyst.printers.StateflowSpPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.printers.hycreate2.HyCreate2Printer;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * Main start class for Hyst
 * If run without args, a GUI will be used. If run with args, the command-line version is assumed.
 */
public class Hyst
{
	public static String TOOL_NAME = "Hyst v1.2";
	public static String programArguments;

	private static ArrayList <String> xmlFilenames = new ArrayList <String>();
	private static String cfgFilename = null, outputFilename = null;
	private static int printerIndex = -1; // index into printers array
	public static boolean verboseMode = false; // flag used to toggle verbose printing with Main.log()
	public static boolean debugMode = false; // flag used to toggle debug printing with Main.logDebug()
	private static String toolParamsString = null; // tool parameter string set using -toolparams or -tp

	public static boolean silentUsage = false; // should usage printing be omitted (for unit testing)
	private static HystFrame guiFrame = null; // set if gui mode is being used

	public final static String FLAG_HELP = "-help";
	public final static String FLAG_GUI = "-gui";
	public final static String FLAG_TOOLPARAMS = "-toolparams";
	public final static String FLAG_TOOLPARAMS_SHORT = "-tp";
	public final static String FLAG_HELP_SHORT = "-h";
	public final static String FLAG_VERBOSE = "-verbose";
	public final static String FLAG_VERBOSE_SHORT = "-v";
	public final static String FLAG_DEBUG = "-debug";
	public final static String FLAG_DEBUG_SHORT = "-d";
	public final static String FLAG_NOVALIDATE = "-novalidate";
	public final static String FLAG_OUTPUT = "-o";

	// add new tool support here
	private static final ToolPrinter[] printers =
			{
					new FlowPrinter(),
					new DReachPrinter(),
					new HyCreate2Printer(),
					new HyCompPrinter(),
					new PythonQBMCPrinter(),
					new SpaceExPrinter(),
					new SMTPrinter(),
					new StateflowSpPrinter(),
					new LayoutPrinter(),
			};

	// passes that are run only if the user selects them
	private static final TransformationPass[] availablePasses =
			{
					new AddIdentityResetPass(),
					new PseudoInvariantPass(),
					new PseudoInvariantSimulatePass(),
					new TimeScalePass(),
					new SubstituteConstantsPass(),
					new SimplifyExpressionsPass(),
					new SplitDisjunctionGuardsPass(),
					new RemoveSimpleUnsatInvariantsPass(),
					new ShortenModeNamesPass(),
					new RegularizePass(),
					//new ContinuizationPass(), // TODO: add back, but the commons-cli stuff is breaking the stateflow converter
					new HybridizeGridPass(),
					new HybridizeTimeTriggeredPass(),
					new FlattenAutomatonPass(),
			};

	// passes that the user has selected
	private static ArrayList <RequestedTransformationPass> requestedPasses =
			new ArrayList <RequestedTransformationPass>();

	public enum ExitCode
	{
		SUCCESS, // 0
		EXPORT_EXCEPTION, // 1
		PRECONDITIONS_EXCEPTION, // 2
		ARG_PARSE_ERROR,
		INTERNAL_ERROR,
		GUI_QUIT,
		EXPORT_AUTOMATON_EXCEPTION,
	};

	public static void main(String[] args)
	{
		if (!checkPrintersPasses())
			System.exit(ExitCode.INTERNAL_ERROR.ordinal());

		if (args.length > 0 && !args[0].equals(FLAG_GUI))
		{
			int code = convert(args);
			
			System.exit(code);
		}
		else
		{
			final String loadFilename = (args.length >= 2) ? args[1] : null;

			// use gui
			System.out.println("Started in GUI mode. For command-line help use the -help flag.");

			fixLookAndFeel();

			SwingUtilities.invokeLater(new Runnable(){

				@Override
				public void run()
				{
					guiFrame = new HystFrame(printers, availablePasses);

					if (loadFilename != null)
						guiFrame.guiLoad(loadFilename);

					guiFrame.setVisible(true);
				}
			});
		}
	}


	/**
	 * Main conversion thread
	 * @param args
	 * @return the exit code
	 */
	public static int convert(String[] args)
	{
		resetVars();

		if (!parseArgs(args))
			return ExitCode.ARG_PARSE_ERROR.ordinal();

		if (debugMode)
			log("Debug mode (even more verbose) printing enabled.\n");
		else if (verboseMode)
			log("Verbose mode printing enabled.\n");

		programArguments = makeSingleArgument(args);
		Expression.expressionPrinter = null; // this should be assigned by the pass / printer as needed

		long startMs = System.currentTimeMillis();
		ToolPrinter printer = printers[printerIndex];

		try
		{
			// 1. import the SpaceExDocument
			SpaceExDocument spaceExDoc = SpaceExImporter.importModels(cfgFilename,
					xmlFilenames.toArray(new String[xmlFilenames.size()]));

			// 2. convert the SpaceEx data structures to template automata
			Map <String, Component> componentTemplates = TemplateImporter.createComponentTemplates(spaceExDoc);

			// 3. run any component template passes here (future)

			// 4. instantiate the component templates into a networked configuration
			Configuration config = ConfigurationMaker.fromSpaceEx(spaceExDoc, componentTemplates);

			// 5. run passes
			runPasses(config);

			// 6. run printer
			runPrinter(printer, config);
		}
		catch (AutomatonExportException aee)
		{
			logError("Automaton Export Exception while exporting: " + aee.getLocalizedMessage());

			if (verboseMode)
			{
				log("Stack trace from exception:");

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				aee.printStackTrace(pw);
				log(sw.toString());
			}
			else
				logError("For more information about the error, use the -verbose or -debug flag.");

			return ExitCode.EXPORT_AUTOMATON_EXCEPTION.ordinal();
		}
		catch (PreconditionsFailedException ex)
		{
			logError("Preconditions not met for exporting: " + ex.getLocalizedMessage());

			if (verboseMode)
			{
				log("Stack trace from exception:");

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);
				log(sw.toString());
			}

			return ExitCode.PRECONDITIONS_EXCEPTION.ordinal();
		}
		catch (Exception ex)
		{
			logError("Exception while exporting: " + ex.getLocalizedMessage());

			if (verboseMode)
			{
				log("Stack trace from exception:");

				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);
				log(sw.toString());
			}

			return ExitCode.EXPORT_EXCEPTION.ordinal();
		}

		long difMs = System.currentTimeMillis() - startMs;

		printer.flush();
		Hyst.logInfo("\nFinished converting in " + difMs + " ms");

		return ExitCode.SUCCESS.ordinal();
	}

	private static void runPrinter(ToolPrinter printer, Configuration config)
	{
		String originalFilename = joinStrings(xmlFilenames, " ");

		if (outputFilename != null)
			printer.setOutputFile(outputFilename);
		else if (guiFrame != null)
			printer.setOutputGui(guiFrame);

		printer.print(config, toolParamsString, originalFilename);
	}

	private static void runPasses(Configuration config)
	{
		for (RequestedTransformationPass rp : requestedPasses)
		{
			Hyst.log("Running pass " + rp.tp.getName() + " with params " + rp.params);

			rp.tp.runTransformationPass(config, rp.params);

			Hyst.logDebug("\n----------After running pass " + rp.tp.getName()
					+ ", configuration is:\n" + config);
		}
	}

	private static void resetVars()
	{
		xmlFilenames = new ArrayList <String>();
		cfgFilename = null;
		outputFilename = null;
		printerIndex = -1;
		verboseMode = false;
		debugMode = false;
		toolParamsString = "";
		requestedPasses.clear();
	}

	private static void fixLookAndFeel()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {}
	}

	public static String joinStrings(ArrayList<String> list, String sep)
	{
		String rv = null;

		for (String s : list)
		{
			if (rv == null)
				rv = s;
			else
				rv += sep + s;
		}

		return rv;
	}

	public static String makeSingleArgument(String[] ar)
	{
		String rv = "";

		for (String s : ar)
		{
			if (rv.length() != 0)
				rv += " ";

			// escape spaces and special handling of empty string
			if (s.length() == 0)
				rv += "\"\"";
			else if (s.contains(" "))
				rv += "\"" + s + "\"";
			else
				rv += s;
		}

		return rv;
	}

	/**
	 * Parse arguments, return TRUE if they're alright, FALSE if not
	 * @param args
	 * @return
	 */
	private static boolean parseArgs(String[] args)
	{
		boolean rv = true;

		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];

			boolean processedArg = false;

			for (int pi = 0; pi < printers.length; ++pi)
			{
				String flag = printers[pi].getCommandLineFlag();

				if (flag.equals(arg))
				{
					if (printerIndex != -1)
					{
						String other = printers[printerIndex].getCommandLineFlag();

						logError("Error: multiple printers selected " + arg + " and " + other);
						rv = false;
					}
					else
					{
						printerIndex = pi;
						processedArg = true;
					}
				}
			}

			if (processedArg)
				continue;

			for (TransformationPass tp : availablePasses)
			{
				if (tp.getCommandLineFlag().equals(arg))
				{
					if (i + 1 >= args.length)
					{
						logError("Error: Custom pass flags always needs a subsequent argument: " + arg);
						rv = false;
					}
					else
					{
						String passParam = args[++i];

						requestedPasses.add(new RequestedTransformationPass(tp, passParam));
					}

					processedArg = true;
				}
			}

			if (processedArg)
				continue;

			if (arg.equals(FLAG_HELP) || arg.equals(FLAG_HELP_SHORT) || arg.endsWith(FLAG_GUI))
				; // ignore
			else if (arg.equals(FLAG_VERBOSE) || arg.equals(FLAG_VERBOSE_SHORT))
				verboseMode = true;
			else if (arg.equals(FLAG_DEBUG) || arg.equals(FLAG_DEBUG_SHORT))
			{
				verboseMode = true;
				debugMode = true;
			}
			else if (arg.equals(FLAG_TOOLPARAMS) || arg.equals(FLAG_TOOLPARAMS_SHORT))
			{
				if (toolParamsString.length() > 0)
				{
					logError("Error: " + FLAG_TOOLPARAMS + " argument used twice.");
					rv = false;
				}
				else if (i + 1 < args.length)
				{
					toolParamsString = args[++i];
				}
				else
				{
					logError("Error: " + FLAG_TOOLPARAMS + " argument expects parameter after");
					rv = false;
				}
			}
			else if (arg.equals(FLAG_NOVALIDATE))
			{
				Hyst.setModeNoValidate();
			}
			else if (arg.equals(FLAG_OUTPUT))
			{
				if (i + 1 < args.length)
				{
					outputFilename = args[++i];
				}
				else
				{
					logError("Error: " + FLAG_OUTPUT + " argument expects filename after");
					rv = false;
				}

			}
			else if (arg.endsWith(".xml"))
			{
				xmlFilenames.add(arg);

				if (cfgFilename == null)
				{
					String base = arg.substring(0, arg.length() - 4);
					cfgFilename = base + ".cfg";
				}
			}
			else if (arg.endsWith(".cfg"))
				cfgFilename = arg;
			else
			{
				logError("Error: Unknown argument: " + arg);
				rv = false;
			}
		}

		if (!rv || xmlFilenames.size() == 0 || cfgFilename == null || printerIndex < 0 || printerIndex >= printers.length)
		{
			if (silentUsage)
				return false;

			// show usage

			System.out.println(TOOL_NAME);
			System.out.println("Usage:");
			System.out.println("hyst [OutputType] (args) XMLFilename(s) "
					+ "(CFGFilename)");
			System.out.println();
			System.out.println("OutputType:");

			for (ToolPrinter tp : printers)
			{
				String arg = tp.getCommandLineFlag();

				String experimental = tp.isInRelease() ? "" : "(Experimental)";
				System.out.println("\t" + arg + " " + tp.getToolName() + " " + experimental + " format");

				// also print the default params
				Map<String, String> params = tp.getDefaultParams();

				if (params != null)
				{
					System.out.print("\t\t" + FLAG_TOOLPARAMS + " ");
					boolean first = true;

					for (Entry<String, String> e : params.entrySet())
					{
						if (first)
							first = false;
						else
							System.out.print(":");

						System.out.print(e.getKey() + "=" + e.getValue());
					}
					System.out.println();
				}
			}

			System.out.println("\nAvailable Model Transformation Passes:");

			for (TransformationPass tp : availablePasses)
			{
				String p = tp.getParamHelp();

				if (p == null)
					p = "[no param]";

				System.out.println("\t" + tp.getCommandLineFlag() + " " + tp.getName()
						+ " " + p);
			}

			System.out.println();
			System.out.println(FLAG_TOOLPARAMS + " name1=val1:name2=val2:... Specify printer-specific parameters");
			System.out.println(FLAG_HELP + " show this command-line help text");
			System.out.println(FLAG_GUI + " [filename] force gui mode with the given input model");
			System.out.println(FLAG_VERBOSE + " Enable verbose printing");
			System.out.println(FLAG_DEBUG + " Enable debug printing (even more verbose)");
			System.out.println(FLAG_NOVALIDATE + " skip internal model validation (may result in Exceptions being thrown)");
			System.out.println(FLAG_OUTPUT + " [filename] output to the given filename");
			System.out.println("XMLFilename: The SpaceEx XML automaton to be "
					+ "processed (*.xml)");
			System.out.println("CFGFilename: The automaton's config file. Will "
					+ "be derived from the XML filename if not explicitly stated (*.cfg)");
			rv = false;
		}

		return rv;
	}

	/**
	 * Check for internal consistency of the defined Printers and TransformationPasses
	 * @return true if they are internally consistent
	 */
	private static boolean checkPrintersPasses()
	{
		boolean rv = true;

		// make sure command line flags do not collide
		TreeMap<String, String> flags = new TreeMap <String, String>();

		flags.put(FLAG_GUI, "force gui flag");
		flags.put(FLAG_HELP, "help flag");
		flags.put(FLAG_HELP_SHORT, "help flag (short version)");
		flags.put(FLAG_VERBOSE, "verbose printing mode flag");
		flags.put(FLAG_VERBOSE_SHORT, "verbose printing mode flag (short version)");
		flags.put(FLAG_DEBUG, "debug printing mode flag");
		flags.put(FLAG_DEBUG_SHORT, "debug printing mode flag (short version)");
		flags.put(FLAG_NOVALIDATE, "no validation flag");
		flags.put(FLAG_OUTPUT, "output to filename flag");
		flags.put(FLAG_TOOLPARAMS, "tool params flag");
		flags.put(FLAG_TOOLPARAMS_SHORT, "tool params flag (short version)");

		for (ToolPrinter tp : printers)
		{
			String name = "tool argument for " + tp.getToolName();
			String arg = tp.getCommandLineFlag();

			if (flags.get(arg) != null)
			{
				logError("Error: Command-line argument " + arg + " is defined both as " +
						name + " as well as " + flags.get(arg));
				rv = false;
			}
			else
				flags.put(arg, name);
		}

		if (rv)
		{
			for (TransformationPass p : availablePasses)
			{
				String name = "transformation pass argument for " + p.getClass().getName();
				String arg = p.getCommandLineFlag();

				// command line argument must be defined for optionalPasses
				if (arg == null)
				{
					rv = false;
					logError("Command-line flag for " + p.getClass().getName() + " is not defined.");
				}
				else
				{
					if (flags.get(arg) != null)
					{
						logError("Error: Command-line argument " + arg + " is defined both as " +
								name + " as well as " + flags.get(arg));
						rv = false;
					}
					else
						flags.put(arg, name);
				}

				if (p.getName() == null)
				{
					rv = false;
					logError("getName() for pass " + p.getClass().getName() + " is not defined.");
				}
			}
		}

		return rv;
	}

	/**
	 * Print an info message to stderr, if the -v flag has been set (verbose mode is enabled)
	 * @param message the message to print
	 */
	public static void log(String message)
	{
		if (verboseMode || debugMode)
		{
			if (guiFrame != null)
				guiFrame.addOutput(message);

			System.err.println(message);
		}
	}

	/**
	 * Print an info message to stderr, regardless of verbose / debug flags
	 * @param message the message to print
	 */
	public static void logInfo(String message)
	{
		if (guiFrame != null)
			guiFrame.addOutput(message);
		else
			System.err.println(message);
	}

	/**
	 * Print an info message to stderr, if the -d flag has been set (debug mode is enabled). This is
	 * even more verbose
	 * @param message the message to print
	 */
	public static void logDebug(String message)
	{
		if (debugMode)
		{
			if (guiFrame != null)
				guiFrame.addOutput(message);

			System.err.println(message);
		}
	}

	/**
	 * Print an error message to stderr
	 * @param message the message to print
	 */
	public static void logError(String message)
	{
		if (guiFrame != null)
			guiFrame.addOutput(message);

		System.err.println(message);
	}

	/**
	 * Disable internal model validation (and removes some error checking)
	 */
	public static void setModeNoValidate()
	{
		Configuration.DO_VALIDATION = false;
		System.err.println("Internal model validatation disabled.");
	}
}
