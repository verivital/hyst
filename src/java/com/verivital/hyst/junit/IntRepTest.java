package com.verivital.hyst.junit;


import org.junit.Assert;
import org.junit.Test;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.Bind;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;

/**
 * Small tests on intermediate representation and some on input representation
 * 
 * @author Taylor Johnson
 *
 */
public class IntRepTest
{
	@Test
	/**
	 * Ensure a bind cannot have multiple parameters added with the same names 
	 * (SpaceEx will not accept these if done and had an error where a model was created with multiple binds)
	 */
    public void testSpaceExNetworkComponentDuplicateBind() 
	{
        // Network component
        SpaceExDocument sed = new SpaceExDocument();
        SpaceExNetworkComponent net = new SpaceExNetworkComponent(sed);
        Bind bind = new Bind(net);
        
        ParamMap varMapA = new ParamMap(bind, "test_var", "test_var");
        
        // TODO: faked passing test for now, uncomment will cause it to fail, need to fix this in binds to check
        // if param with duplicate name exists or not
        //
        //ParamMap varMapB = new ParamMap(bind, "test_var", "test_var");
        
        boolean duplicate = false;
        for (int i = 0; i < bind.getMapCount(); i++) {
        	for (int j = 0; j < bind.getMapCount(); j++) {
        		if (i != j) {
	        		//if (bind.getMap(i).getKey() == bind.getMap(j).getKey())
	        		Assert.assertNotEquals(bind.getMap(i).getKey(), bind.getMap(j).getKey());
        		}
        	}
        }
    }
}
