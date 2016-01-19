/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.verivital.hyst.printers;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.microsoft.z3.ApplyResult;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Goal;
import com.microsoft.z3.Log;
import com.microsoft.z3.Status;
import com.microsoft.z3.Symbol;
import com.microsoft.z3.Tactic;
import com.microsoft.z3.Version;
import com.microsoft.z3.Z3Exception;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;


/**
 *
 * @author Luan Nguyen
 */
public class SMTPrinter extends ToolPrinter 

{
    private BaseComponent ha;
 
    public static void addDir(String s) throws IOException {
	    try {
	        // This enables the java.library.path to be modified at runtime
	        // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
	        //
	        Field field = ClassLoader.class.getDeclaredField("usr_paths");
	        field.setAccessible(true);
	        String[] paths = (String[])field.get(null);
	        for (int i = 0; i < paths.length; i++) {
	            if (s.equals(paths[i])) {
	                return;
	            }
	        }
	        String[] tmp = new String[paths.length+1];
	        System.arraycopy(paths,0,tmp,0,paths.length);
	        tmp[paths.length] = s;
	        field.set(null,tmp);
	        System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
	    } catch (IllegalAccessException e) {
	        throw new IOException("Failed to get permissions to set library path");
	    } catch (NoSuchFieldException e) {
	        throw new IOException("Failed to get field handle to set library path");
	    }
	}
    
    
    public SMTPrinter() {
		/*
    	
    	//java.library.path.
		// workaround to add to java.library.path:
		
		// http://stackoverflow.com/questions/4871051/getting-the-current-working-directory-in-java
		String pwd = System.getProperty("user.dir"); // get pwd
		System.out.println("Present working directory: " + pwd);
		String libDir = pwd + File.separator + ".." + File.separator + "lib" + File.separator + "win" + File.separator + "x64";
		//System.load("D:\\Dropbox\\Research\\verivital_repos\\spaceex-converter\\lib\\win\\x64\\libz3java.dll");
		System.out.println(System.getenv("PATH"));
		//System.setpr
		String newPath = System.getenv("PATH") + ";" + pwd + ";" + libDir + ";";
		System.setProperty("PATH", newPath);
		System.setProperty("java.library.path", newPath);
		System.out.println(System.getenv("PATH"));
		System.out.println(System.getProperty("java.library.path"));
		
		
		
		// http://stackoverflow.com/questions/5419039/is-djava-library-path-equivalent-to-system-setpropertyjava-library-path
		try {
			addDir(libDir);
    		addDir(pwd);
    		Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
    		fieldSysPath.setAccessible( true );
    		fieldSysPath.set( null, null );
    		System.out.println("Success: updated lib paths");
		}
		catch (Exception ex) {
			System.out.println("Error: failed to update lib paths");
		}
		System.out.println(System.getProperty("java.library.path"));
		
		// http://stackoverflow.com/questions/7016391/difference-between-system-load-and-system-loadlibrary-in-java
		System.load(libDir + File.separator + "libz3.dll");
		System.load(libDir + File.separator + "libz3.so");
		//System.loadLibrary("libz3java");
		
		// tried to add to eclipse jar to specify native libraries (no luck):
		// http://stackoverflow.com/questions/661320/how-to-add-native-library-to-java-library-path-with-eclipse-launch-instead-of
		
		// no luck:
		// http://stackoverflow.com/questions/12078457/java-util-usbuirt-dll-cant-find-dependent-libraries
		
		// http://stackoverflow.com/questions/1087054/jni-dependent-libraries
		
		// other ideas:
		// https://github.com/epfl-lara/ScalaZ3/wiki/ScalaZ3-and-Visual-Studio-2012-Express-for-Windows-Desktop
		
		// http://www.dependencywalker.com/
		
		// need to call java with: -Djava.library.path="%PATH%;../lib/win/x64;${env_var:PATH};${workspace_loc:project}\..\lib;
		
		// example java call for example: https://z3.codeplex.com/discussions/451539
		// java -cp com.microsoft.z3.jar;. JavaExample
		
		// compilation command:
		//JavaExample.class: com.microsoft.z3.jar ..\examples\java\JavaExample.java "C:\Program Files\Java\jdk1.6.0_45\bin\javac.exe" -cp com.microsoft.z3.jar  ..\examples\java\JavaExample.java -d .
		//_ex_java_example: JavaExample.class
		
		*/
		/*
		try 
		{
			Context.ToggleWarningMessages(true);
	        Log.open("test.log");
	
	        System.out.print("Z3 Major Version: ");
	        System.out.println(Version.getMajor());
	        System.out.print("Z3 Full Version: ");
	        System.out.println(Version.getString());
		}
		catch (Z3Exception ex)
        {
            System.out.println("Z3 Managed Exception: " + ex.getMessage());
            System.out.println("Stack trace: ");
            ex.printStackTrace(System.out);
        } 
		catch (Exception ex)
        {
            System.out.println("Unknown Exception: " + ex.getMessage());
            System.out.println("Stack trace: ");
            ex.printStackTrace(System.out);
        }*/
    }
    
    
    /**
     * TODO: update, this is just copied
     */
    public Map <String, String> getDefaultParams()
	{
		LinkedHashMap <String, String> params = new LinkedHashMap <String, String>();
		
		params.put("time", "auto");
		params.put("step", "auto-auto");
		params.put("remainder", "1e-4");
		params.put("precondition", "auto");
		params.put("plot", "auto");
		params.put("orders", "3-8");
		params.put("cutoff", "1e-15");
		params.put("precision", "53");
		params.put("jumps", "99999999");
		params.put("print", "on");
		params.put("aggregation", "parallelotope");
		
		return params;
	}
    
    private void printDocument(String originalFilename) 
    {          
		Expression.expressionPrinter = new SMTExpressionPrinter();
		printVars();
		printConstants();
		printAssert();
		// printLine("(get-model)");
		printNewline();
		// check z3 working properly
		// simpleExample();
		System.out.print("Z3 Major Version: ");
		try {
			//Context.ToggleWarningMessages(true);
			Log.open("test.log");

			System.out.print("Z3 Major Version: ");
			System.out.println(Version.getMajor());
			System.out.print("Z3 Full Version: ");
			System.out.println(Version.getString());
			try {
				simpleExample();
			} catch (TestFailedException ex) {
			}

		} catch (Z3Exception ex) {
		};
		
    }
    
    // add basic example to check whether z3 works in Hyst
    public void simpleExample() throws Z3Exception, TestFailedException
    {
        System.out.println("SimpleExample");
        Log.append("SimpleExample");

        {
            Context ctx = new Context();
            Symbol x = ctx.mkSymbol("x");
            Symbol y = ctx.mkSymbol("y");
            Goal g3 = ctx.mkGoal(true, true, false);
            Expr xc = ctx.mkConst(ctx.mkSymbol("x"), ctx.getIntSort());
            Expr yc = ctx.mkConst(ctx.mkSymbol("y"), ctx.getIntSort());
            g3.add(ctx.mkEq(xc, ctx.mkNumeral(1, ctx.getIntSort())));
            g3.add(ctx.mkEq(yc, ctx.mkNumeral(2, ctx.getIntSort())));
            BoolExpr constr = ctx.mkEq(xc, yc);
            g3.add(constr);
            ApplyResult ar = applyTactic(ctx, ctx.mkTactic("smt"), g3);
            if (ar.getNumSubgoals() != 1 || !ar.getSubgoals()[0].isDecidedUnsat())
                throw new TestFailedException();

            //modelConverterTest(ctx);
            /* do something with the context */

            /* be kind to dispose manually and not wait for the GC. */
            //ctx.dispose();
        }
    }
    // add basic example to check whether z3 works in Hyst
    ApplyResult applyTactic(Context ctx, Tactic t, Goal g) throws Z3Exception
    {
        System.out.println("\nGoal: " + g);

        ApplyResult res = t.apply(g);
        System.out.println("Application result: " + res);

        Status q = Status.UNKNOWN;
        for (Goal sg : res.getSubgoals())
            if (sg.isDecidedSat())
                q = Status.SATISFIABLE;
            else if (sg.isDecidedUnsat())
                q = Status.UNSATISFIABLE;

        switch (q)
        {
        case UNKNOWN:
            System.out.println("Tactic result: Undecided");
            break;
        case SATISFIABLE:
            System.out.println("Tactic result: SAT");
            break;
        case UNSATISFIABLE:
            System.out.println("Tactic result: UNSAT");
            break;
        }

        return res;
    }
    
    @SuppressWarnings("serial")
    class TestFailedException extends Exception
    {
        public TestFailedException()
        {
            super("Check FAILED");
        }
    };    
    /**
     * Declare all variables
     */
    private void printVars() 
    {		
            for (String v : ha.variables)
                    printLine("(declare-fun " + v + " () Real)"); 
    }
        
    /**
     * Declare all constants
     */
    private void printConstants()
    {
        printLine("");
            if (!ha.constants.isEmpty()){
                for (Entry <String, Interval> e : ha.constants.entrySet()){
                    printLine("(declare-const " + e.getKey() + " (Real))"); 
                    //e.getKey(),Double.toString(e.getValue())
                }
            }
        printLine("");
    }
    /**
     * Print the SMT syntax to check the satisfiability of intersection between initial and forbidden conditions,  initial conditions and invariants, invariant and outgoing guards for each location
     */
    private void printAssert() 
    {		
            int check = 1; // avoid duplicated printing
            for (Entry <String, AutomatonMode> e : ha.modes.entrySet()) 
            {
                    AutomatonMode mode = e.getValue();                   
                    if (!mode.invariant.equals(Constant.TRUE)) 

                            for (Entry <String, Expression> en : config.init.entrySet())
                            {

                                if (en.getKey().equals(mode.name))
                                {
                                    printLine(commentChar + "check the satisfiability of the intersections between initial condition and invariant at location: " + en.getKey());  
                                    printSMTCheck(en.getValue(),mode.invariant);
                                }
                                for (Entry <String, Expression> entry : config.forbidden.entrySet())
                                {
                                    if (en.getKey().equals(entry.getKey())&&(check == 1))
                                    {
                                        printLine(commentChar + "check the satisfiability of the intersections between initial and forbidden conditions at location: " + entry.getKey());  
                                        printSMTCheck(en.getValue(),entry.getValue());
                                        ++check;
                                    }
                                }
                            } 

                        printLine(commentChar + "check the satisfiability of the intersections between invariant and outgoing guards of location: " + mode.name);
                            for (AutomatonTransition t : ha.transitions)
                            {
                                if (t.from == mode)
                                {
                                    printSMTCheck(mode.invariant,t.guard);
                                }
                            } 

            }
    }
    /**
     * Print the SMT syntax to check the satisfiability of intersection between two expression a and b
     */
    private void printSMTCheck(Expression a,  Expression b) 
    {
        printLine("(push)");
        printLine("(assert " + a + ")");
        printLine("(assert " + b + ")");
        printLine("(check-sat)");
        printLine("(pop)");
    } 
        
    public static class SMTExpressionPrinter extends DefaultExpressionPrinter
    {
            public SMTExpressionPrinter()
            {
                    super();

                    opNames.put(Operator.AND, "and");
                    opNames.put(Operator.OR, "or");
                    opNames.put(Operator.POW, "^");
            }
            
            public String printOperation(Operation o)
            {
                    String rv = null;

                    Operator op = o.op;

                    switch (op) 
                    {
                            case MULTIPLY :
                            case DIVIDE :
                            case ADD :
                            case SUBTRACT :
                                    // default
                                    //rv = super.printOperation(o);
                                    //break;
                            case EQUAL :
                            case LESS :
                            case GREATER :
                            case LESSEQUAL :
                            case GREATEREQUAL :
                            case POW:
                            case NOTEQUAL :
                            case NEGATIVE:
                                    //break;
                            default :
                                    // prefix
                                    rv = "(" + this.opNames.get(op);

                                    for (Expression e : o.children)
                                            rv += " " + this.print(e);

                                    rv += ")";
                                    break;
                    }
                    return rv;
            }
            
    }
    

    @Override
    protected void printAutomaton() {
    	this.ha = (BaseComponent)config.root;
            printDocument(originalFilename);
    }

    @Override
    public String getToolName() {
        return ("SMT-LIB printer"); 
    }

    @Override
    public String getCommandLineFlag() {
        return "-smtlib";
    }

    @Override
    protected String getCommentPrefix() {
        return ";";
    }
    
}