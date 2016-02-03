package com.verivital.hyst.internalpasses;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;

/**
 * Internal passes are similar to transformation passes, but instead are called programmatically.
 * They are like utility functions, but perform in-place modifications of a Configuration object.
 * By convention, call the static run() method to perform the transformation.
 * 
 * @author Stanley Bak
 */
public class SwapModeNames
{
	/**
	 * Swaps mode names for alternates.
	 * Params are a colon separated list of names: oldname1:newname1:oldname2:newname2:...
	 * 
	 * If new name exists, a number will be appended to it (the number starts at 2 and is incremented until a fresh variable is found)
	 * 
	 * @param convertMap the map of old_name -> new_name
	 *
	 */
	public static void run(Configuration config, Map <String, String> convertMap)
	{
		BaseComponent ha = (BaseComponent)config.root;
		
		// rename in modes
		for (Entry<String, String> e : convertMap.entrySet())
		{
			String oldName = e.getKey();
			String newName = e.getValue();
			
			AutomatonMode m = ha.modes.get(oldName);
			
			if (m == null)
				throw new AutomatonExportException("Mode name for swapping not found: " + oldName);
			
			m.name = newName;
			
			ha.modes.remove(oldName);
			ha.modes.put(newName, m);
		}
		
		// rename in init / fodbidden
		renameInMap(convertMap, config.init);
		
		if (config.forbidden != null)
			renameInMap(convertMap, config.forbidden);
	}

	private static void renameInMap(Map<String, String> convertMap, LinkedHashMap<String, Expression> map)
	{
		for (Entry<String, String> e : convertMap.entrySet())
		{
			String oldName = e.getKey();
			String newName = e.getValue();
			
			Expression val = map.get(oldName);
			
			if (val == null)
				continue;
			
			map.remove(oldName);
			map.put(newName, val);
		}
	}
}
