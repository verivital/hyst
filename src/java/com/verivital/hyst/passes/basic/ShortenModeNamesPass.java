package com.verivital.hyst.passes.basic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;

/**
 * A model transformation pass which shortens mode names. They will be renamed to mode_#, and the
 * mapping will be printed using Hyst.log() (use -v for verbose printing)
 * 
 * @author Stanley Bak (March 2015)
 *
 */
public class ShortenModeNamesPass extends TransformationPass
{
	@Override
	public String getName()
	{
		return "Shorten Mode Names Pass";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "shorten";
	}

	@Override
	protected void runPass()
	{
		BaseComponent ha = (BaseComponent) config.root;
		HashMap<String, String> nameMap = makeNameMap(ha);

		ha.modes = renameModes(nameMap, ha.modes);
		config.init = renameInitForbidden(nameMap, config.init);
		config.forbidden = renameInitForbidden(nameMap, config.forbidden);
	}

	private LinkedHashMap<String, Expression> renameInitForbidden(HashMap<String, String> nameMap,
			LinkedHashMap<String, Expression> old)
	{
		LinkedHashMap<String, Expression> rv = new LinkedHashMap<String, Expression>();

		for (Entry<String, Expression> e : old.entrySet())
		{
			Expression val = e.getValue();
			String newKey = nameMap.get(e.getKey());

			rv.put(newKey, val);
		}

		return rv;
	}

	private LinkedHashMap<String, AutomatonMode> renameModes(Map<String, String> nameMap,
			LinkedHashMap<String, AutomatonMode> oldModes)
	{
		LinkedHashMap<String, AutomatonMode> rv = new LinkedHashMap<String, AutomatonMode>();

		for (Entry<String, AutomatonMode> e : oldModes.entrySet())
		{
			AutomatonMode am = e.getValue();
			String newKey = nameMap.get(e.getKey());

			am.name = newKey;
			rv.put(newKey, am);
		}

		return rv;
	}

	private HashMap<String, String> makeNameMap(BaseComponent ha)
	{
		HashMap<String, String> nameMap = new HashMap<String, String>();
		int mode = 0;

		for (String name : ha.modes.keySet())
		{
			String newName = "mode_" + mode++;

			Hyst.log("Shortened mode name to " + newName + " from " + name);
			nameMap.put(name, newName);
		}

		return nameMap;
	}
}
