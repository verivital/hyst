package com.verivital.hyst.ir;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.util.AutomatonUtil;

/**
 * A configuration is a hybrid automaton (network or base component) plus the settings (init states and such)
 * 
 * Class validation guarantees:
 * settings is not null
 * root is not null
 * init is not null and size > 0, each String is a mode in the automaton; expression may be null (these can be rejected in ToolPrinter)
 * forbidden is not null and if size > 0, each String is a mode in the automaton; expression may be null (these can be rejected in ToolPrinter)
 *
 */
public class Configuration
{
	public static boolean DO_VALIDATION = true;
	
	public AutomatonSettings settings = new AutomatonSettings(this);
	
	public LinkedHashMap <String, Expression> init = new LinkedHashMap <String, Expression>();
	public LinkedHashMap <String, Expression> forbidden = new LinkedHashMap <String, Expression>();
	
	public Component root = null;
	
	public Configuration(Component root)
	{
		this.root = root;
	}
	
	public Configuration copy()
	{
		Configuration rv = new Configuration(root.copy());
		rv.settings = settings.copy(rv);
		
		for (Entry<String, Expression> e : init.entrySet())
			rv.init.put(e.getKey(), e.getValue().copy());
		
		for (Entry<String, Expression> e : forbidden.entrySet())
			rv.forbidden.put(e.getKey(), e.getValue().copy());
		
		return rv;
	}
	
	public void validate()
	{
		if (!Configuration.DO_VALIDATION)
			return;
		
		validateMap(init, "init", false);
		validateMap(forbidden, "forbidden", true);
		
		// validate the children
		if (settings == null)
			throw new AutomatonValidationException("settings cannot be null");
		
		settings.validate();
		
		if (root == null)
			throw new AutomatonValidationException("root component cannot be null");
		
		// only root has a null parent and no instance name
		if (root.instanceName != null)
			throw new AutomatonValidationException("root component cannot have a defined instance name");
		
		if (root.parent != null)
			throw new AutomatonValidationException("root component vannot have a defined parent");
		
		// if root is a network component, initial and forbidden states MUST contain dots
		if (root instanceof NetworkComponent)
		{
			for (String mode : init.keySet())
			{
				if (!mode.contains("."))
					throw new AutomatonValidationException("Initial mode in network component must contain '.': " + mode);
			}
			
			for (String mode : forbidden.keySet())
			{
				if (!mode.contains("."))
					throw new AutomatonValidationException("Forbidden mode in network component must contain '.': " + mode);
			}
		}
		
		for (Expression e : init.values())
		{
			if (e == null)
				throw new AutomatonValidationException("Initial states contain null expression");
		}
		
		for (Expression e : forbidden.values())
		{
			if (e == null)
				throw new AutomatonValidationException("Forbidden states contain null expression");
		}
		
		root.validate();
	}

	private void validateMap(LinkedHashMap<String, Expression> map, String name, boolean allowEmpty)
	{
		if (map == null)
			throw new AutomatonValidationException("map was null");
		
		if (map.size() == 0 && !allowEmpty)
			throw new AutomatonValidationException(name + " was was empty (size 0)");
			
		for (Entry<String, Expression> e : map.entrySet())
		{
			String modeName = e.getKey();
			
			if (!AutomatonUtil.modeExistsInComponent(modeName, root))
			{
				throw new AutomatonValidationException(name + " contains mode " + modeName + ", which is not in the automaton");
			}
		}
	}
	
	@Override
	public String toString()
	{
		StringBuffer str = new StringBuffer();
				
		str.append("Hybrid Automaton Configuration:");
		str.append("\nInit: " + AutomatonUtil.getMapExpressionString(init));
		str.append("\nForbidden: " + AutomatonUtil.getMapExpressionString(forbidden));
		str.append("\nSettings: " + settings);
		str.append("\nRoot Component:");
		
		str.append(root.toString());
		
		return str.toString();
	}
}
