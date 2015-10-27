package com.verivital.hyst.python;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.util.FileOperations;

/**
 * This class is a java <-> python interface using stdin / stdout
 * Each instance represents a process. For reasonable performance, you should use an single 
 * session for multiple calls, and not spawn a process for each function you need to call.
 * 
 * Use open() to open the process, which should be called before interacting with python, and close() to close it.
 * Failure to call close() will leave the python process running, which is a bug.
 * 
 * DO NOT INSTANTIATE THIS CLASS IN A STATIC CONTEXT. If you do this, running Hyst will be dependent on having python installed, 
 * which we don't want.
 * 
 * Overhead:
 * In performance tests, I measured around 15000 function calls per second using this bridge
 * In native python, I measured 5.5 million function calls per second
 * 
 * You can reduce overhead by passing all of your data to python at once (or as much as is possible), and then having python 
 * do an extended computation (even in parallel) and only then print back the result. 
 * 
 * @author Stanley Bak (May 2015)
 *
 */
public class PythonBridge
{
	private static final int DEFAULT_TIMEOUT = 10000; // 10 seconds
	private int timeoutMs;
	private Process process = null;
	private BufferedReader stdout = null;
	private BufferedReader stderr = null;
	private Writer stdin = null;
	
	public PythonBridge()
	{
		this(DEFAULT_TIMEOUT);
	}
	
	/**
	 * Sets the timeout in milliseconds, use -1 for no timeout
	 * @param timeoutMs
	 */
	public PythonBridge(int timeoutMs)
	{
		this.timeoutMs = timeoutMs;
	}
	
	/**
	 * Sets the timeout in milliseconds, use -1 for no timeout
	 * @param timeoutMs
	 */
	public void setTimeout(int timeoutMs)
	{
		this.timeoutMs = timeoutMs;
	}
	
	public void open()
	{
		log("Opening Python process in interactive mode.");
		openProcess();
		
		log("Reading Python preamble");
		String preamble = readPreamble(timeoutMs);
		
		if (!preamble.startsWith("Python 2.7"))
			warn("Python 2.7 was not detected in python interpreter preamble. " +
					"There may be version issues. Preamble:\n" + preamble);
		
		log("Python process opened successfully. Preamble: \n" + preamble);
		
		// remove the continuation prompt on multi-line commands in interactive mode "... "
		send("import sys");
		send("sys.ps2 = ''"); 
	}
	
	private static void log(String description)
	{
		Hyst.log(description);
	}
	
	private static void logDebug(String description)
	{
		Hyst.logDebug(description);
	}
	
	/**
	 * Close the process (if needed), and raise an error
	 * @param description the error description
	 */
	private void error(String description)
	{
		Hyst.logDebug(description);
		close();
		
		throw new AutomatonExportException(description);
	}
	
	private void error(String description, Exception e)
	{
		close();
		
		throw new AutomatonExportException(description, e);
	}
	
	private void warn(String description)
	{
		System.err.println("Warning: " + description);
	}
	
	public void close()
	{
		if (process != null)
		{
			try
			{
				process.getInputStream().close();
				process.getOutputStream().close();
		        process.getErrorStream().close();
			} 
			catch (IOException e) {}
	        
			process.destroy();
			try
			{
				process.waitFor();
			} 
			catch (InterruptedException e) {}
			
	        process = null;
	        stdout = null;
	        stdin = null;
	        stderr = null;
		}
	}
	
	private void openProcess()
	{
		if (process != null)
			error("openProcess called but process is already open.");
			
		String processNames[] = {"python2.7", "python"};
		final String ENV_VAR = "HYST_PATH";
		String loc = null;
		
		for (String processName : processNames)
		{
			try
			{
				loc = FileOperations.locate(processName, processName, ENV_VAR);
				Hyst.log("Using python process at path: " + loc);
				break;
			}
			catch (FileNotFoundException e)
			{
				Hyst.log(e.getMessage());
			}
		}
		
		if (loc == null)
			error("Error starting python process. Is 'python2.7' or 'python' on your PATH or " + ENV_VAR + "?");
		
		ProcessBuilder pb = new ProcessBuilder(loc, "-i");
		String workingDir = getJarBaseDirectory();
		pb.directory(new File(workingDir));
		
		try
		{
			process = pb.start();
		}
        catch (IOException e) 
        {
        	error("Exception while starting python process: " + e.toString());
        }
		
		stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        stdin = new OutputStreamWriter(process.getOutputStream());
	}
	
	public static String getJarBaseDirectory()
	{
		String path = PythonBridge.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String rv;
		
		try
		{
			rv = URLDecoder.decode(path, "UTF-8");
		} 
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
		
		if (!isInsideJar())
			rv += ".." + File.separator;
		else 
			rv = new File(rv).getParent(); // should end in .jar
		
		return rv;
	}
	
	private static boolean isInsideJar()
	{
		return PythonBridge.class.getResource("PythonBridge.class").toString().startsWith("jar:");
	}
	
	/**
	 * Read the python preamble off of stderr. It ends when the prompy '>>> ' is detected.
	 * @return the preamble read from stderr
	 */
	private String readPreamble(int timeoutMs)
	{
		String rv = null;
		StringBuilder sb = new StringBuilder();
		long timeout = System.currentTimeMillis() + timeoutMs;
		
		try
		{
			char[] prompt = {'>', '>', '>', ' '};
			int index = 0;
	        
	        while (true)
	        {
	        	// sleep until there's input
	        	while (!stderr.ready())
	        	{
	        		Thread.yield();
	        		
	        		if (timeoutMs >= 0 && System.currentTimeMillis() >= timeout)
	        			error("Timeout (" + timeoutMs + " ms) reached while reading python preamble.");
	        	}
	        		
	        	int readInt = stderr.read();
	        	
	        	if (readInt == -1) // end of stream
	        		error("End of output stream was reached while looking for python prompt.");
	        	
	        	char c = (char)readInt;
	        	
	        	sb.append(c);
	        	
	        	if (c == prompt[index])
	        	{
	        		++index;
	        		
	        		if (index == prompt.length)
	        		{
	        			// exit only if there are no more bytes to be read
	        			if (!stderr.ready())
	        				break;
	        			else
	        				index = 0;
	        		}
	        	}
	        	else
	        		index = 0;
	        }
	        
	        // trim off the prompt
	        int len = sb.length();
	        sb.delete(len - prompt.length, len);
	        
	        // remove \n and \r if they're at the end of the string
	        trimSuffix(sb, '\n');
	        trimSuffix(sb, '\r');
	        
	        rv = sb.toString();
		}
		catch (IOException e)
		{
			error("Error while reading python preamble.", e);
		}
		
		return rv;
	}
	
	/**
	 * Read stdout until the prompt '>>> ' is given on stderr
	 * @param quitOnStderr should we raise an exception if stderr output is detected other than the prompt?
	 * @return output the received stdout / stderr strings
	 */
	private String readUntilPrompt()
	{
		String rv = null;
		StringBuilder sbStdout = new StringBuilder();
		StringBuilder sbStderr = new StringBuilder();
		long timeout = System.currentTimeMillis() + timeoutMs;
		
		try
		{
			char[] prompt = {'>', '>', '>', ' '};
			int index = 0;
	        boolean done = false;
			
	        while (!done)
	        {
	        	// sleep until there's input
	        	while (!stdout.ready() && !stderr.ready())
	        	{
	        		Thread.yield();
	        		
	        		if (timeoutMs >= 0 && System.currentTimeMillis() >= timeout)
	        		{
	        			String out = "";
	        			String err = "";
	        			
	        			if (sbStdout.length() > 0)
	        				out = "\nStdout was: '" + sbStdout.toString() + "'";
	        			
	        			if (sbStderr.length() > 0)
	        				err = "\nStderr was: '" + sbStderr.toString() + "'";
	        			
	        			error("Timeout (" + timeoutMs + " ms) reached during python interaction." + out + err);
	        		}
	        	}
	        	
	        	// read stdout until blocked
	        	while (stdout.ready())
	        	{
	        		int readInt = stdout.read();
	        		
	        		if (readInt == -1) // end of stream
		        		error("End of output stream was reached while reading python response.");
	        		
	        		sbStdout.append((char)readInt);
	        	}
	        	
	        	// read stderr until blocked
	        	while (stderr.ready())
	        	{
	        		int readInt = stderr.read();
	        		
	        		if (readInt == -1) // end of stream
		        		error("End of output stream was reached while looking for python prompt.");
	        		
	        		char c = (char)readInt;
		        	sbStderr.append(c);
		        	
		        	if (c == prompt[index])
		        	{
		        		++index;
		        		
		        		if (index == prompt.length)
		        		{
		        			// exit only if there are no more bytes to be read
		        			if (!stderr.ready())
		        			{
		        				done = true;
		        				break;
		        			}
		        			else
		        				index = 0;
		        		}
		        	}
		        	else
		        		index = 0;
	        	}
	        }
	        
	        // read any remaining stdout characters
	        sbStdout.append(readUntilBlocked(stdout));
	        
	        // trim off the prompt
	        sbStderr.delete(sbStderr.length() - prompt.length, sbStderr.length());
	        
	        // trim off \n if it's at the end
	        trimSuffix(sbStdout, '\n');
	        trimSuffix(sbStdout, '\r');
	        
	        // if anything was printed to stderr, it's an error
	        if (sbStderr.length() > 0)
	        	error("Python produced output on stderr:\n" + sbStderr.toString() + 
	        			(sbStdout.length() > 0 ? "\n\nstdout was:\n" + sbStdout.toString() : ""));
	        
	        rv = sbStdout.toString();
		}
		catch (IOException e)
		{
			error("Error while reading python output", e);
		}
        
        return rv;
	}

	private void trimSuffix(StringBuilder sb, char c)
	{
		int len = sb.length();
        
        if (len > 0 && sb.charAt(len-1) == c)
        	sb.delete(len-1, len);
	}

	/**
	 * Read a stream until it's blocked
	 * @param stream
	 * @return
	 */
	private String readUntilBlocked(BufferedReader stream) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		
		while (stream.ready())
		{
			int c = stream.read();
			
			// end of stream
			if (c == -1)
				break;
			
			sb.append((char)c);
		}
		
		return sb.toString();
	}

	/**
	 * Send a string over stdin to the python interpreter and get the result printed from stdout
	 * @param cmd the command to send (without the trailing newline, which is automatically added)
	 * @return the output from stdout until the next prompt. May be the empty string, but never null.
	 */
	private String sendAndWait(String s)
	{
		String result = null;
		
		if (process == null)
			error("send called but process is closed.");
		
		try
		{
			String stdOutBefore = readUntilBlocked(stdout);
			String stdErrBefore = readUntilBlocked(stderr);
			
			if (stdOutBefore.length() != 0)
				error("stdout contained stale text before the send command: '" + stdOutBefore + "'");
			
			if (stdErrBefore.length() != 0)
				error("stderr contained stale text before the send command: '" + stdErrBefore + "'");
			
			logDebug("Sending to python: " + s);
			stdin.write(s);
			stdin.write("\n");
			stdin.flush();
			
			logDebug("Reading from python with timeout " + timeoutMs + " ms");
			result = readUntilPrompt();
			logDebug("Read result from python: " + result);
		}
		catch (IOException e)
		{
			error("Error while interacting with python during send()", e);
		}
		
		return result;
	}
	
	/**
	 * Send a string over stdin to the python interpreter and get the result printed from stdout
	 * @param cmd the command to send (without the trailing newline, which is automatically added)
	 * @return the output from stdout until the next prompt. May be the empty string, but never null.
	 */
	public String send(String s)
	{
		return send(s, false);
	}
	
	/**
	 * Send a string over stdin to the python interpreter and get the result printed from stdout.
	 * 
	 * This version allows the string to end with a newline, which is typically an error, but may be
	 * necessary for things like function declarations
	 * 
	 * @param cmd the command to send (trailing newline is automatically added, so that your string will have
	 * 			  two trailing newlines)
	 * @return the output from stdout until the next prompt. May be the empty string, but never null.
	 */
	public String sendWithTrailingNewline(String s)
	{
		if (!s.endsWith("\n"))
			error("sendNewlineIsOnPurpose() used by command didn't end with newline: " + s);
			
		return send(s, true);
	}
	
	private String send(String s, boolean allowNewlineEndings)
	{
		String result = null;
		
		if (process == null)
			error("send() called but process is not running (was open() called?)");
		
		if (s.length() == 0)
			error("send() called with empty string");
		else if (!allowNewlineEndings && s.endsWith("\n"))
			error("send() ends with \\n; the newline is added automatically and this would " +
					"cause issues with PythonBridge's prompt detection.\nIf you want to end " +
					"the command with a \\n, for example to declare a function, use sendWithTrailingNewline().");
		
		result = sendAndWait(s);
		
		return result;
	}
	
	public void importPythonBridgeModule(String module)
	{
		String result = send("from pythonbridge import " + module);
		
		if (result.length() != 0)
			error("import file created output on stdout: '" + result + "'");
	}
}
