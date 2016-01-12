/**
 * 
 */
package com.verivital.hyst.printers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Map;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.main.HystFrame;
import com.verivital.hyst.util.Preconditions;


/**
 * A generic tool printer class. Printers for individual tools will override this abstract class. The model is printed by using
 * printConfiguration().
 */
public abstract class ToolPrinter 
{
	// configuration being printer
	protected Configuration config;
	
	// parameters
	protected String originalFilename = null; // assigned in setParameters
	protected String baseName = null; // assigned in setParameters from originalFilename
	protected String outputFilename = null; // assigned in setParameters, can be null
	private String toolParamsString = null; // assigned in setParameters
	protected Map <String, String> toolParams = getDefaultParams(); // assigned before printing

	// don't need to be modified
	protected String indentation = "";
	protected String indentationAmount = "    ";
	protected String commentChar = getCommentPrefix();
	protected String decreaseIndentationString = "}";
	
	// checks to do before printing (assign to the preconditions.skip in your ToolPrinter constructor to omit checks)
	protected Preconditions preconditions = new Preconditions(false); // run all checks by default
	
	// printing
	public enum OutputType
	{
		STDOUT,
		GUI,
		FILE,
		NONE,
	};
	
	public void setConfig(Configuration c) {
		this.config = c;
	}
	
	protected OutputType outputType = OutputType.STDOUT;
	protected PrintStream outputStream; // used if printType = STDOUT or FILE
	protected HystFrame outputFrame; // used if printType = GUI
	
	
	// static 
	private static DecimalFormat df = new DecimalFormat("0.#");
		
	public void setOutputFile(String filename)
	{
		outputType = OutputType.FILE;
		outputFilename = filename;
	}
	
	public void setOutputGui(HystFrame frame)
	{
		outputType = OutputType.GUI;
		outputFrame = frame;
	}
	
	public void setOutputNone()
	{
		outputType = OutputType.NONE;
	}
	
	/**
	 * Set tool param string: needed to set tool parameters for tests
	 * @param s
	 */
	public void setToolParamsString(String s) {
		this.toolParamsString = s;
	}
	
	/**
	 * Prints the networked automaton out to the given file
	 * @param networkedAutomaton the automaton to print
	 */
	public void print(Configuration c, String toolParamsString, String originalFilename)
	{
		this.toolParamsString = toolParamsString;
		this.originalFilename = originalFilename;

		boolean shouldCloseStream = false;
		
		if (toolParamsString == null) 
			throw new AutomatonExportException("toolsParamString was null in ToolPrinter.print()");
		
		setBaseName(originalFilename);
		
		populateParams();
		
		try
		{
			if (outputType == OutputType.STDOUT) {
				outputStream = System.out;
			}
			else if (outputType == OutputType.FILE)
			{
				shouldCloseStream = true;
				outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFilename)));
			}
			
			this.config = c;
			checkPreconditions(c);
			printAutomaton();
		}
		catch (FileNotFoundException e)
		{
			throw new AutomatonExportException("File Not Found", e);
		}
		catch (SecurityException e)
		{
			throw new AutomatonExportException("Security Error", e);
		}
		finally
		{
			if (shouldCloseStream && outputStream != null)
				outputStream.close();
		}
	}

	private void setBaseName(String originalFilename)
	{
		if (originalFilename == null || originalFilename.length() == 0)
			baseName = "root";
		else
		{
			baseName = new File(originalFilename).getName();
			int i = baseName.lastIndexOf(".");
			
			if (i != -1)
				baseName = baseName.substring(0, i);
		}
	}
	
	/**
	 * Print a newline in the output file stream
	 */
	protected void printNewline() 
	{
		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.println();
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput("");
	}

	/**
	 * Increase indent while printing
	 */
	protected void increaseIndentation() 
	{
		indentation += indentationAmount;
	}

	/**
	 * Decrease indent while printing
	 */
	protected void decreaseIndentation() 
	{
		if (indentation.length() > 0)
			indentation = indentation.substring(indentationAmount.length());
	}
	
	/**
	 * Print several lines of text in a comment block
	 * @param comment
	 */
	protected void printCommentblock(String comment) 
	{
		String s = this.indentation + commentChar + " " + 
				comment.replace("\n", "\n" + this.indentation + commentChar + " ") + "\n";
		
		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.println(s);
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput(s);
	}
	
	/**
	 * print a short comment
	 * @param comment
	 */
	protected void printComment(String comment) {
		printLine(this.commentChar + comment);
	}
	
	protected String getCommentHeader() {
		return "Created by " + Hyst.TOOL_NAME + "\n" +
				"Hybrid Automaton in " + this.getToolName() + "\n" +
				"Converted from file: " + originalFilename + "\n" +
				"Command Line arguments: " + Hyst.programArguments;
	}
	
	/**
	 * Print header information as a comment with parameters, etc.
	 */
	protected void printCommentHeader() {
		printCommentblock(getCommentHeader());
	}
	
	protected void printLine(String line) {
		this.printLine(line, true);
	}

	protected void printLine(String line, boolean indent) 
	{
		if (indent && line.equals(decreaseIndentationString)) 
			decreaseIndentation();
		
		
		String s = "";
		if (indent) 
			s += this.indentation;
		
		s += line;
		
		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.println(s);
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput(s);
		
		if (indent && line.equals("{")) 
			increaseIndentation();
	}
	
	protected void print(String s) 
	{
		this.print(s, true);
	}
	
	protected void print(String s, boolean indent) 
	{
		String newS;
		if (indent) 
			newS = this.indentation + s;
		else 
			newS = s;
		
		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.print(newS);
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput(newS);
	}
	
	/**
	 * Get a string representation of the name of the tool, such as "SpaceEx" or "Flow*"
	 * @return the name of the tool
	 */
	public abstract String getToolName();
	
	/**
	 * Get the command line flag for this tool 
	 * @return
	 */
	public abstract String getCommandLineFlag();

	/**
	 * Get a single-line comment character for the tool's output format
	 * @return
	 */
	protected abstract String getCommentPrefix();

	/**
	 * Should this tool be considered release-quality, which will make it show up in the GUI
	 * @return 
	 */
	public boolean isInRelease()
	{
		return false;
	}
	
	static
	{
		df.setMaximumFractionDigits(50);
	}
	
	public static String doubleToString(double n)
	{
		return df.format(n);
	}

	public void flush()
	{
		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.flush();
	}
	
	/**
	 * convert from toolParamsString to toolParams
	 */
	private void populateParams()
	{
		toolParams = getDefaultParams();
		
		Map <String, String> defParams = getDefaultParams();
		
		if (defParams == null && toolParamsString.length() > 0)
			throw new AutomatonExportException("Printer does not expect any tool-specific paramers, but some were given.");
		else if (defParams != null)
			toolParams.putAll(defParams);
		
		if (toolParamsString.length() > 0 && toolParamsString.length() > 0)
		{
			String[] assignments = toolParamsString.split(":");
			
			for (String assignment : assignments)
			{
				String[] parts = assignment.split("=");
				
				if (parts.length != 2)
				{
					throw new AutomatonExportException("Tool Param must have a single '=' sign (params are separated by colons): " 
							+ assignment);
				}
				
				if (!toolParams.containsKey(parts[0]))
					throw new AutomatonExportException("Invalid Tool Parameter: '" + parts[0] + "' in assignment " + assignment);
				
				toolParams.put(parts[0], parts[1]);
				Hyst.log("Assigned tool parameter key '" + parts[0] + "' to value '" + parts[1] + "'");
			}
		}
	}
	
	/**
	 * Override this method to have tool-specific parameters set through the -toolparams flag
	 * This method returns a map ParamName -> ParamValue, for every parameter you want to have, with the
	 * default values already set.
	 * 
	 * In your printer, you can get the manually-set parameters using getToolParams()
	 * @return a map of all the parameters for your printer, set to their default values
	 */
	public abstract Map <String, String> getDefaultParams();

	/**
	 * Get the default extension for model files for this printer
	 * @return the default extension, or null
	 */
	public String getExtension()
	{
		return null;
	}
	
	/**
	 * Print the automaton. The configuration is stored in the global config variable. checkPreconditions() is called
	 * before this method, which enforces printer assumptions (for example, that the model is flat).
	 */
	protected abstract void printAutomaton();
	
	/**
	 * Check the preconditions for the printer (for example, the modes should have at least 1 variable, no urgrent modes, ect)
	 * Typically, you'll change preconditions.skip direction to indicate which checks should be skipped
	 * Alternatively, printers can override this if they don't want to use the PrinterPreconditions way of doing it.
	 * This should throw a PrinterPreconditionException if assumptions about the model are violated.
	 * @param c the configuration before passing it to the printer
	 */
	protected void checkPreconditions(Configuration c)
	{
		preconditions.check(c, this.getClass().getName());
	}
}
