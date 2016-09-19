package com.verivital.hyst.main;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GuiState
{
	private String xml = "";
	private String cfg = "";
	private String output = "";
	private int printerIndex = 0;
	private ArrayList<Integer> passIndex = new ArrayList<Integer>();
	private ArrayList<String> passParam = new ArrayList<String>();
	private ArrayList<String> printerParams = new ArrayList<String>();

	public static final int NORMAL = 0;
	public static final int VERBOSE = 1;
	public static final int DEBUG = 2;
	private int debugMode = NORMAL;

	public GuiState()
	{
	}

	public String getXml()
	{
		return xml;
	}

	public void setXml(String xml)
	{
		this.xml = xml;
	}

	public String getCfg()
	{
		return cfg;
	}

	public void setCfg(String cfg)
	{
		this.cfg = cfg;
	}

	public String getOutput()
	{
		return output;
	}

	public void setOutput(String output)
	{
		this.output = output;
	}

	public int getPrinterIndex()
	{
		return printerIndex;
	}

	public void setPrinterIndex(int printerIndex)
	{
		this.printerIndex = printerIndex;
	}

	public ArrayList<Integer> getPassIndex()
	{
		return passIndex;
	}

	public void setPassIndex(ArrayList<Integer> passIndex)
	{
		this.passIndex = passIndex;
	}

	public ArrayList<String> getPassParam()
	{
		return passParam;
	}

	public void setPassParam(ArrayList<String> passParam)
	{
		this.passParam = passParam;
	}

	public static void save(GuiState gs, String filename)
	{
		try
		{
			saveObject(gs, filename);
		}
		catch (IOException e)
		{
			System.err.println("Error saving " + filename + ": " + e);
		}
	}

	public static GuiState load(String filename)
	{
		GuiState rv = null;

		try
		{
			rv = ((GuiState) loadObject(filename));
		}
		catch (IOException e)
		{
			// silently ignore load errors
		}

		if (rv == null)
			rv = new GuiState();

		return rv;
	}

	private static void saveObject(Object bean, String path) throws IOException
	{
		XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(path)));
		e.writeObject(bean);
		e.close();
	}

	private static Object loadObject(String path) throws IOException
	{
		Object rv = null;

		XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(path)));
		try
		{
			rv = d.readObject();
		}
		catch (Exception e)
		{
			String error = "Error loading file: " + e;
			d.close();
			throw new IOException(error);
		}

		d.close();

		return rv;
	}

	public int getDebugMode()
	{
		return debugMode;
	}

	public void setDebugMode(int debugMode)
	{
		this.debugMode = debugMode;
	}

	public ArrayList<String> getPrinterParams()
	{
		if (printerParams == null)
			printerParams = new ArrayList<String>();

		return printerParams;
	}

	public void setPrinterParams(ArrayList<String> printerParam)
	{
		this.printerParams = printerParam;
	}
}
