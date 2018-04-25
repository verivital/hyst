/**
 * 
 */
package com.verivital.hyst.importer;

import java.io.File;

import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.main.Hyst;

import de.uni_freiburg.informatik.swt.spaxeexxmlreader.SpaceExXMLReader;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * Reads in a SpaceEx model into the internal automaton format
 * 
 * Based on Boogie converter by Christopher Dillo (dilloc@informatik.uni-freiburg.de) Stanley Bak
 * (8-2014)
 */
public class SpaceExImporter
{
	/**
	 * Read a file in the SpaceEx format and produce the internal HybridAutomaton representation
	 * 
	 * @param xmlFilename
	 * @param cfgFilename
	 * @return
	 */
	public static SpaceExDocument importModels(String cfgFilename, String... xmlFilenames)
	{
		for (String name : xmlFilenames)
		{
			if (!new File(name).exists())
				throw new AutomatonExportException(
						name + " not found; full path tried: " + new File(name).getAbsolutePath());
		}

		if (!new File(cfgFilename).exists())
			Hyst.log(cfgFilename + " not found. Using default settings.");

		if (xmlFilenames.length < 1)
			throw new AutomatonExportException("must have at least one xml filename");

		SpaceExDocument rv = new SpaceExXMLReader(xmlFilenames[0], cfgFilename).read();

		for (int i = 1; i < xmlFilenames.length; ++i)
		{
			String xml = xmlFilenames[i];

			SpaceExDocument doc = new SpaceExXMLReader(xml, null).read();

			// merge into rv
			addToDocument(rv, doc);
		}

		return rv;
	}

	/**
	 * Add all the components from 'from' into rv
	 * 
	 * @param rv
	 *            the place to store components
	 * @param from
	 *            the place to take them from
	 */
	private static void addToDocument(SpaceExDocument rv, SpaceExDocument from)
	{
		for (int cIndex = 0; cIndex < from.getComponentCount(); ++cIndex)
		{
			SpaceExComponent c = from.getComponent(cIndex);

			rv.addComponent(c);
		}
	}
}
