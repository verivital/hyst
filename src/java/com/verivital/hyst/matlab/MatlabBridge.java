package com.verivital.hyst.matlab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.util.FileOperations;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

/**
 * This class is java <-> matlab interface using a matlab proxy
 * 
 * TODO: could operate via stdin / stdout and matlab interactive mode TODO: look at whether we can
 * do everything we need to do by calling matlab in its console mode as this will probably speed
 * things up a lot (no GUI, etc.)
 * 
 * It is a singleton, use getInstance() to get an instance of the bridge. The bridge is reused for
 * any passes or printers which use it, so don't put it into an inconsistent state.
 * 
 * Based on python bridge TODO: generalize and refactor bridges to move common functionality into a
 * parent class (like ToolPrinter) TODO: check duplication between Python bridge and the Z3Py
 * printer used for QBMC
 * 
 * @author Taylor Johnson (December 2015)
 *
 */
public class MatlabBridge
{
	private static MatlabBridge instance = null;

	private MatlabProxyFactory factory;

	private static final int DEFAULT_TIMEOUT = 30000; // 30 seconds: may have to
														// start matlab up
	private int timeoutMs;
	// private Process process = null;
	private BufferedReader stdout = null;
	private BufferedReader stderr = null;
	private Writer stdin = null;

	public enum Status
	{
		FALSE, TRUE, UNKNOWN
	}

	// these static flags
	private static boolean blockMatlab = false;
	private static Status statusMatlab = Status.UNKNOWN;

	/**
	 * This sets whether matlab should be blocked (pretend it doesn't exist). This is useful for
	 * unit testing.
	 * 
	 * @param val
	 */
	public static void setBlockMatlab(boolean val)
	{
		blockMatlab = val;
	}

	public static boolean hasMatlab()
	{
		boolean rv = false;

		if (!blockMatlab)
		{
			if (statusMatlab == Status.UNKNOWN)
			{
				try
				{
					getInstance();
				}
				catch (AutomatonExportException e)
				{
					statusMatlab = Status.FALSE;
				}
				catch (Exception e)
				{
					statusMatlab = Status.FALSE;
				}
			}

			rv = statusMatlab == Status.TRUE;
		}

		if (rv)
		{
			try
			{
				getInstance().getProxy().disconnect();
			}
			catch (MatlabInvocationException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (MatlabConnectionException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return rv;
	}

	public static MatlabBridge getInstance()
			throws MatlabInvocationException, MatlabConnectionException
	{
		return getInstance(DEFAULT_TIMEOUT);
	}

	public static MatlabBridge getInstance(int timeoutMs)
			throws MatlabInvocationException, MatlabConnectionException
	{
		if (blockMatlab)
		{
			// this occurs if the user programatically called
			// setBlockMatlab(true), and then tries to call getInstance()
			// for example, if we're unit testing we may want to check that
			// we're correctly checking if matlab
			// exists before calling getInstance(). If not, this exception may
			// be thrown. Use hasMatlab() to check.
			throw new AutomatonExportException(
					"MatlabBridge.getInstance() was called, but blockMatlab was set to true.");
		}

		if (instance == null)
		{
			instance = new MatlabBridge(timeoutMs);
		}
		else
		{
			instance.setTimeout(timeoutMs);
		}

		return instance;
	}

	/**
	 * Sets the timeout in milliseconds, use -1 for no timeout
	 * 
	 * @param timeoutMs
	 * @throws MatlabConnectionException
	 * @throws MatlabInvocationException
	 */
	public MatlabBridge(int timeoutMs) throws MatlabInvocationException, MatlabConnectionException
	{
		this.timeoutMs = timeoutMs;

		if (instance != null)
		{
			throw new RuntimeException("Multiple instances of MatlabBridge were created.");
		}

		open();

		final MatlabBridge bridge = this;

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				bridge.close();
			}
		});

		statusMatlab = Status.TRUE;
	}

	/**
	 * Sets the timeout in milliseconds, use -1 for no timeout
	 * 
	 * @param timeoutMs
	 */
	public void setTimeout(int timeoutMs)
	{
		this.timeoutMs = timeoutMs;
	}

	private void open() throws MatlabInvocationException, MatlabConnectionException
	{
		log("Opening Matlab process in interactive mode.");
		openProcess();

		/*
		 * log("Reading Matlab preamble"); String preamble = readPreamble(timeoutMs);
		 * 
		 * if (!preamble.startsWith("Python 2.7")) warn(
		 * "Python 2.7 was not detected in python interpreter preamble. " +
		 * "There may be version issues. Preamble:\n" + preamble);
		 */

		log("Matlab process opened successfully. Preamble: \n");
		// + preamble

		// remove the continuation prompt on multi-line commands in interactive
		// mode "... "
		// send("import sys");
		// send("sys.ps2 = ''");
		/*
		 * for (String pack : REQUIRED_PACKAGES) { try { send("import " + pack); } catch
		 * (AutomatonExportException e) { error( "Python import failed on required package '" + pack
		 * + "'"); } }
		 */
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
	 * 
	 * @param description
	 *            the error description
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

	private void close()
	{
		if (factory != null) // todo: add process?
		{
			try
			{
				// Disconnect the proxy from MATLAB
				factory.getProxy().disconnect(); // NOTE: important, this has to
													// be done frequently, as
													// otherwise new calls to
													// the bridge (i.e., that
													// would create accesses as
													// in subsequent calls to
													// getInstance, etc.) will
													// cause this to close
				// so, the contract is: call disconnect whenever you are done
				// using the proxy, and then the next call to getInstance will
				// not start a new matlab session (which is very slow)

				// TODO: actually, this didn't work, maybe there is some issue
				// with this being singleton, who knows, will try to fix later
				// to improve performance

				// process.getInputStream().close();
				// process.getOutputStream().close();
				// process.getErrorStream().close();
			}
			catch (MatlabConnectionException e)
			{
				e.printStackTrace();
			}

			// process.destroy();
			/*
			 * try { process.waitFor(); } catch (InterruptedException e) {}
			 */

			// process = null;

			factory = null;

			stdout = null;
			stdin = null;
			stderr = null;
		}
	}

	public MatlabProxy getProxy()
	{
		try
		{
			return this.factory.getProxy();
		}
		catch (MatlabConnectionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	private void openProcess() throws MatlabInvocationException, MatlabConnectionException
	{
		if (this.factory != null)
		{
			error("openProcess called but process is already open.");
		}

		String processNames[] = { "matlab", "matlab2014a" };
		final String ENV_VAR = "HYST_MATLAB_PATH";
		String loc = null;

		for (String processName : processNames)
		{
			try
			{
				loc = FileOperations.locate(processName, processName, ENV_VAR);
				Hyst.log("Using matlab process at path: " + loc);
				break;
			}
			catch (FileNotFoundException e)
			{
				Hyst.log(e.getMessage());
			}
		}

		if (loc == null)
			error("Error starting matlab process. Is 'matlab' on your PATH or " + ENV_VAR + "?");

		ProcessBuilder pb = new ProcessBuilder(loc, "-i");
		String workingDir = getJarBaseDirectory();
		pb.directory(new File(workingDir));

		// this will try to reconnect to existing session if possible
		MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
				.setUsePreviouslyControlledSession(true)
				.setMatlabStartingDirectory(new File(getJarBaseDirectory())).build();
		// TESTCASE: can use .setMatlabLocation("null") to check if tests will
		// pass on systems without matlab
		// TODO: use .setMatlabLocation(loc)
		// TODO: get this to work, some bad path problems: .setHidden(true)

		this.factory = new MatlabProxyFactory(options);

		factory.getProxy().eval("cd " + getJarBaseDirectory());

		// stdout = new BufferedReader(new
		// InputStreamReader(process.getInputStream()));
		// stderr = new BufferedReader(new
		// InputStreamReader(process.getErrorStream()));
		// stdin = new OutputStreamWriter(process.getOutputStream());
	}

	public static String getJarBaseDirectory()
	{
		String path = MatlabBridge.class.getProtectionDomain().getCodeSource().getLocation()
				.getPath();
		String rv;

		try
		{
			rv = URLDecoder.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}

		// if (!isInsideJar())
		// rv += ".." + File.separator;
		// else
		rv = new File(rv).getParent(); // should end in .jar

		return rv;
	}

	/*
	 * private static boolean isInsideJar() { return
	 * MatlabBridge.class.getResource("MatlaBridge.class").toString().startsWith ("jar:"); }
	 */

	/**
	 * Read the python preamble off of stderr. It ends when the prompy '>>> ' is detected.
	 * 
	 * @return the preamble read from stderr
	 */
	private String readPreamble(int timeoutMs)
	{
		String rv = null;
		StringBuilder sb = new StringBuilder();
		long timeout = System.currentTimeMillis() + timeoutMs;

		try
		{
			char[] prompt = { '>', '>', '>', ' ' };
			int index = 0;

			while (true)
			{
				// sleep until there's input
				while (!stderr.ready())
				{
					Thread.yield();

					if (timeoutMs >= 0 && System.currentTimeMillis() >= timeout)
						error("Timeout (" + timeoutMs
								+ " ms) reached while reading python preamble.");
				}

				int readInt = stderr.read();

				if (readInt == -1) // end of stream
					error("End of output stream was reached while looking for python prompt.");

				char c = (char) readInt;

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
	 * 
	 * @param quitOnStderr
	 *            should we raise an exception if stderr output is detected other than the prompt?
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
			char[] prompt = { '>', '>', '>', ' ' };
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

						error("Timeout (" + timeoutMs + " ms) reached during python interaction."
								+ out + err);
					}
				}

				// read stdout until blocked
				while (stdout.ready())
				{
					int readInt = stdout.read();

					if (readInt == -1) // end of stream
						error("End of output stream was reached while reading python response.");

					sbStdout.append((char) readInt);
				}

				// read stderr until blocked
				while (stderr.ready())
				{
					int readInt = stderr.read();

					if (readInt == -1) // end of stream
						error("End of output stream was reached while looking for python prompt.");

					char c = (char) readInt;
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
				error("Python produced output on stderr:\n" + sbStderr.toString()
						+ (sbStdout.length() > 0 ? "\n\nstdout was:\n" + sbStdout.toString() : ""));

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

		if (len > 0 && sb.charAt(len - 1) == c)
			sb.delete(len - 1, len);
	}

	/**
	 * Read a stream until it's blocked
	 * 
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

			sb.append((char) c);
		}

		return sb.toString();
	}

	/**
	 * Send a string over stdin to the python interpreter and get the result printed from stdout
	 * 
	 * @param cmd
	 *            the command to send (without the trailing newline, which is automatically added)
	 * @return the output from stdout until the next prompt. May be the empty string, but never
	 *         null.
	 */
	private String sendAndWait(String s)
	{
		String result = null;

		if (factory == null)
		{ // TODO: use process?
			error("send called but process is closed.");
		}

		try
		{
			String stdOutBefore = readUntilBlocked(stdout);
			String stdErrBefore = readUntilBlocked(stderr);

			if (stdOutBefore.length() != 0)
				error("stdout contained stale text before the send command: '" + stdOutBefore
						+ "'");

			if (stdErrBefore.length() != 0)
				error("stderr contained stale text before the send command: '" + stdErrBefore
						+ "'");

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
	 * 
	 * @param cmd
	 *            the command to send (without the trailing newline, which is automatically added)
	 * @return the output from stdout until the next prompt. May be the empty string, but never
	 *         null.
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
	 * @param cmd
	 *            the command to send (trailing newline is automatically added, so that your string
	 *            will have two trailing newlines)
	 * @return the output from stdout until the next prompt. May be the empty string, but never
	 *         null.
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

		// if (process == null)
		if (factory == null)
			error("send() called but process is not running (was open() called?)");

		if (s.length() == 0)
			error("send() called with empty string");
		else if (!allowNewlineEndings && s.endsWith("\n"))
			error("send() ends with \\n; the newline is added automatically and this would "
					+ "cause issues with PythonBridge's prompt detection.\nIf you want to end "
					+ "the command with a \\n, for example to declare a function, use sendWithTrailingNewline().");

		result = sendAndWait(s);

		return result;
	}

}
