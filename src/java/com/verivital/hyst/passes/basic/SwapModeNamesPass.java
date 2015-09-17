package com.verivital.hyst.passes.basic;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.TransformationPass;


/**
 * Swaps mode names for alternates.
 * Params are a colon separated list of names: oldname1:newname1:oldname2:newname2:...
 * 
 * If new name exists, a number will be appended to it (the number starts at 2 and is incremented until a fresh variable is found)
 * 
 * @author sbak
 *
 */
public class SwapModeNamesPass extends TransformationPass
{
	@Override
	protected void runPass(String params)
	{
		BaseComponent ha = (BaseComponent)config.root;
		Map <String, String> convertMap = getConversionMap(ha, params);
		
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

	private void renameInMap(Map<String, String> convertMap, LinkedHashMap<String, Expression> map)
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

	/**
	 * Gets the map for the conversion. Ensures that the variables we're changing to are fresh.
	 * @param params the colon separated list of pairs
	 * @return
	 */
	private Map<String, String> getConversionMap(BaseComponent ha, String params)
	{
		Map<String, String> rv = new TreeMap<String, String>();
		String[] parts = params.split(":");
		
		if (parts.length % 2 != 0)
			throw new AutomatonExportException("swap names param needs pairs of names");
		
		for (int i = 0; i < parts.length; i += 2)
		{
			String from = parts[i];
			String originalTo = parts[i + 1];
			String to = originalTo;
			int suffix = 2;
			
			while (ha.modes.containsKey(to) || rv.containsKey(to))
				to = originalTo + (suffix++);
			
			rv.put(from,  to);
		}
		
		return rv;
	}
}
