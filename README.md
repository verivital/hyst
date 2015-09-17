HyST: A Source Transformation and Translation Tool for Hybrid Automaton Models 
http://verivital.com/hyst/

Stanley Bak, http://stanleybak.com
Sergiy Bogomolov, http://swt.informatik.uni-freiburg.de/staff/bogom
Taylor T. Johnson, http://www.taylortjohnson.com/

Hyst has been tested on Windows and Linux using Java 1.7.


BUILDING HYST: 

To build Hyst, proceed to the exporter/Hyst directory and run "ant". This will create the Hyst.jar file.


GUI USAGE:

Hyst can be run through a GUI or using the command line. To use the GUI, after building Hyst.jar simply run it as an executable .jar file with no arguments:

$ java -jar Hyst.jar


CHECKING COMMAND-LINE USAGE: 

After building Hyst.jar, you can run it as an executable .jar file with the -help flag to see usage:

$ java -jar Hyst.jar -help
Hyst v1.17
Usage:
hyst [OutputType] (args) XMLFilename(s) (CFGFilename)

OutputType:
	-flowstar Flow* format
	-dreach dReach format
        -hycomp HyComp/HyDI format
	-hycreate HyCreate2 format
	-hycreate_sim HyCreate2 format
	-qbmc Python QBMC (Testing) format
	-spaceex SpaceEx format
	-z SMT-LIB printer format
Optional Model Transformation Passes:
	-pass_pi Pseudo-Invariant at Point Pass [(modename|)pt1;inv_dir1|pt2;inv_dir2|...] (point/invariant direction is a comma-separated list of reals)
	-pass_pi_sim Pseudo-Invariant Simulation Pass [time1;time2;...]
	-pass_scale_time Scale Time Pass [multiplier;ignorevar]
	-pass_sub_constants Substitute Named Constants for Values Pass [no param]
	-pass_simplify Simplify Expressions Pass [no param]
	-pass_split_disjunctions Split Guards with Disjunctions [no param]
	-pass_remove_unsat Remove Unsatisfiable Modes Pass [no param]
	-shorten Shorten Mode Names Pass [no param]
	-regularize Regularization (eliminate zeno behaviors) Pass [<num jumps>;<delta>;<epsilon>]

-help show this command-line help text
-v Enable verbose printing
-debug Enable debug printing (even more verbose)
-novalidate skip internal model validation (may result in Exceptions being thrown)
-o [filename] output to the given filename
XMLFilename: The SpaceEx XML automaton to be processed (*.xml)
CFGFilename: The automaton's config file. Will be derived from the XML filename if not explicitly stated (*.cfg)


CONVERTING AN EXAMPLE: 

To convert from a SpaceEx model, you run Hyst, provide the proper flag for the format you want to output, and the path to the SpaceEx .xml and, if named differently the .cfg file. You can also provide an output filename with the -o flag (stdout will be used otherwise, which may be incompatible with model formats that require multiple files).

$ java -jar Hyst.jar -flowstar ../../examples/toy/toy.xml

In this case -flowstar indicates we want a model in the Flow* format (see the usage above). The .cfg file will be assumed to be ../../examples/toy/toy.cfg since it is not explicitly specified. Since no filename is given using the -o flag, the output will be printed to stdout.


HYCREATE2:

java -jar Hyst.jar ../../examples/heaterLygeros/heaterLygeros.xml -hycreate -o heaterLygeros.hycreate

This will convert the heater/thermostat example described in the paper to the HyCreate2 format, and write the result to the file heaterLygeros.hycreate.

FLOW*:

java -jar Hyst.jar ../../examples/heaterLygeros/heaterLygeros.xml -flowstar -o heaterLygeros.flow

DREACH:

NOTE: dReach (as of this writing) requires files to have the extension .drh to execute.

java -jar Hyst.jar ../../examples/heaterLygeros/heaterLygeros.xml -dreach -o heaterLygeros.drh




EXAMPLES DIRECTORY:

Some examples are not yet complete, as we are currently working to convert all the ARCH 2014 workshop (http://cps-vo.org/group/ARCH) benchmarks to SpaceEx format (where possible).

ADVANCED USAGE:

To generate the output plots provided across all the examples, we have included Python scripts to call HYST against all examples in all formats.  See them in:

exporter\batch_run

Additionally, the scripts provided the ability to call the tools themselves and generate plots.  If you are interested to do this, please contact us and we will share our repository to help get you set up.

To run Hyst on all the models, as well as the associated tools in a batch run, simply do:

python run_examples.py

In run_examples.py there are a bunch of global variables you can set for different parameters (such as different timeouts, the paths to the tool binaries to run). The tool binaries are NOT included in this package (they each have their own licenses you should read before using). The result is placed in the result folder. 

A copy of the expected model files and plot files produced is in the expected_results directory. Also included is stdout.txt which includes stdout when running the script.


TESTS:

There are included unit tests as well as regression tests. You can run these using "ant test". The regression tests may require tool step in the file exporter/Hyst/tests/regression/run_tests.py


ECLIPSE SETUP:

You need to compile with antlt-4.4-runtime.jar on your classpath. In Eclipse, you do this by going to: 

Project -> Properties -> Java Build Path -> Libraries -> Add External Jar -> select the antlt-4.4-complete.jar file (should be in the project directory) -> Ok

RUNNING DIRECTLY FROM .CLASS FILES:

To run the .class files directly, rather than from the .jar, you also need this jar on your classpath (option -cp to java).

*******************************************************************************
HYPY (by Stanley Bak)
*******************************************************************************

DEPENDENCIES:

* sympy: https://github.com/sympy/sympy/releases

pip install sympy

*******************************************************************************
MODEL TRANSFORMATION PASSES:
*******************************************************************************

* Hybridization (by Pradyot Prakash)

taylor2pradyot: can we call it from Hyst.jar or what? Can you please add a full call for an example that is executable (see above, for example, e.g., such as java -jar Hyst.jar ../../examples/heaterLygeros/heaterLygeros.xml -dreach -o heaterLygeros.drh )

DEPENDENCIES: scipy

INSTALLATION:

Windows:

0)

It may or may not be necessary to install MinGW, I gave up, it's possible the below C++ compiler is the only dependency, as I did get it to finish building, but it apparently did not properly install the package as calling the hybridize pass still failed.

Scipy is a pain to install via pip. But if you want to try, here are some ideas. Alternative is just to install YET ANOTHER Python runtime on Windows (but then would have to get this properly set up on the PATH to make this work with Hyst, since there's not a way to specify which Python install to use in Hyst [as far as I know]).

http://stackoverflow.com/questions/12628164/trouble-installing-scipy-on-windows

Have to have MingGW etc. with a GNU compiler to use pip, alternative is to find some binary distribution, but the Python community (and scipy community) are hugely fragmented, so there doesn't seem to be a great version to use, unfortunately.

After installing MingGW, have to add the C:\MinGW\bin directory (or wherever installed) to the Windows path.

1) Download and install this (Visual C++ Compiler for Python 2.7):  http://aka.ms/vcpython27
2) pip install scipy

Linux:

To install via pip, must have fortan compiler and other dependencies installed, see here for overview: http://stackoverflow.com/questions/2213551/installing-scipy-with-pip

1) 
sudo apt-get install python-pip python-dev build-essential

2)
sudo pip install numpy
sudo apt-get install libatlas-base-dev gfortran
sudo pip install scipy

3) 
sudo pip install matplotlib   OR  sudo apt-get install python-matplotlib
sudo pip install -U scikit-learn
sudo pip install pandas

You can call the HybridizePass.java as follows:

-hybridize <variable corresponding to first dimension>,..<variable corresponding to nth dimension>,<lower bound for the first dimension>,<upper bound for the same dimension>,....,<lower bound for the last dimension>,<upper bound for the last dimension>,<number of partitions along the first dimension>,...,<number of partitions along the last dimension>,<'a' if you want a affine system/'l' if you want a piecewise constant system>

For eg,
Consider the van der pol system.
It has two variables x and y.

Suppose we want to consider the interval [-1,2] along the x-axis and [3,4] along y-axis and want 2 partitions along x axis and 3 along y axis, then you should call
-hybridize x,y,-1,2,3,4,2,3,a

You can also call
-hybridize y,x,3,4,-1,2,3,2,a

The last argument 'a' means that you want a final system with affine dynamics in each of the modes. It can also be 'l' if you want a Linear Hybrid Automata with piecewise constant dynamics.

USER README:

HyST: A Source Transformation and Translation Tool for Hybrid Automaton Models 
http://verivital.com/hyst/

Stanley Bak, (stanleybak [at] gmail.com) http://stanleybak.com
Sergiy Bogomolov, http://swt.informatik.uni-freiburg.de/staff/bogom
Taylor T. Johnson, http://www.taylortjohnson.com/

Hyst has been tested on Windows and Linux using Java 1.7.

If you run into any issues, please don't hesitate to contact the authors. If you want to add support for your reachability or falsification tool, please don't hesitate to contact the authors.


GUI USAGE:

Hyst can be run through a GUI or using the command line. To use the GUI, after building Hyst.jar simply run it as an executable .jar file with no arguments. Additionally, depending on your OS you may be able to just double click the icon to run the GUI (on Linux you must set it as executable).

$ java -jar Hyst.jar


CHECKING COMMAND-LINE USAGE: 

You can run it as an executable .jar file with the -help flag to see command-line usage (no GUI):

$ java -jar Hyst.jar -help

This will show you the Hyst version as well as the flags for the various printers and transformation passes. Run the tool with -verbose or -debug is there is any trouble converting to try to get more insight into the Error.


CONVERTING AN EXAMPLE: 

To convert from a SpaceEx model, you run Hyst, provide the proper flag for the format you want to output, and the path to the SpaceEx .xml and, if named differently the .cfg file. You can also provide an output filename with the -o flag (stdout will be used otherwise, which may be incompatible with model formats that require multiple files).

$ java -jar Hyst.jar -flowstar examples/toy/toy.xml

In this case -flowstar indicates we want a model in the Flow* format (see the usage above). The .cfg file will be assumed to be examples/toy/toy.cfg since it is not explicitly specified. Since no filename is given using the -o flag, the output will be printed to stdout.


HYCREATE2:

java -jar Hyst.jar examples/heaterLygeros/heaterLygeros.xml -hycreate -o heaterLygeros.hyc2

This will convert the heater/thermostat example described in the paper to the HyCreate2 format, and write the result to the file heaterLygeros.hyc2.

FLOW*:

java -jar Hyst.jar examples/heaterLygeros/heaterLygeros.xml -flowstar -o heaterLygeros.model

DREACH:

NOTE: dReach (as of this writing) requires files to have the extension .drh to execute.

java -jar Hyst.jar examples/heaterLygeros/heaterLygeros.xml -dreach -o heaterLygeros.drh

SPACEEX:

You may want to convert from a SpaceEx model back to SpaceEx to run some transformation passes or just to do flattening.

java -jar Hyst.jar examples/heaterLygeros/heaterLygeros.xml -spaceex -o heaterLygeros.drh


EXAMPLES AND RESULTS DIRECTORY:

Several examples have been included which can be converted in the examples directory. The result shows the result of converting the models and running them with the various tools using the default settings (not all tools complete on all models).


*************** PRINTER *****************
*****************************************
Continuous-Time Stateflow Charts in MathWorks' Simulink/Stateflow
Modes of operation:
* Semantics preserving
* Non-semantics preserving (but preserves semantics if the automaton is deterministic)

This is supplementary material for the submission at Real-Time Systems Symposium 2015.
Title: Embedding Hybrid Automata into Simulation-Equivalent Simulink/Stateflow Models
Authors: Stanley Bak, Sergiy Bogomolov, Taylor T. Johnson, Luan Viet Nguyen, and Christian Schilling


To run the translator, do the following:

1) Open Matlab
2) Move to the folder where you extracted the files
3) Run the following command (where you enter a meaningful name for MYMODEL and MYCONFIG):
  SpaceExToStateflow('MYMODEL.xml', 'MYCONFIG.cfg', '--folder', 'buckboost', '-s')

This produces a Stateflow model (might take some seconds, especially when the
 Simulink libraries have not been loaded yet).

To run the simulation loop, run the following (parametrized) command:
  simulationLoop('MYMODEL', NUM_SIMULATION, MAX_TIME, NUM_BACKTRACK)

Here the parameters denote the following:
1) the model name (automatically taken from the *.xml file),
2) the number of simulations
3) the maximum simulation time
4) the number of backtrackings


In the following, we list the commands for the main models used in the evaluation:

---

1) Yaw damper model:

Create model:
SpaceExToStateflow('periodic.xml', 'periodic_init_region.cfg', '--folder', 'yaw_damper', '-s');

Run simulations and plot results:
simulationLoop('periodic', 50, 40, 3, 0.00001, {'x4'}, -1, 0);

---

2) Glycemic control model:

Create model:
SpaceExToStateflow('glycemic_control_poly1.xml', 'glycemic_control_poly1.cfg', '--folder', 'glycemic_control_polynomial','-s');

Run simulations and plot results:
simulationLoop('glycemic_control_poly1', 100, 360, 10, 0.001, {'G'}, 1, 1);

---

3) Buck converter model:

Create model:
SpaceExToStateflow('buck_hysteresis_nodcm.xml', 'buck_hysteresis_nodcm.cfg', '--folder', 'buckboost', '-s');

Run simulations and plot results:
simulationLoop('buck_hysteresis_nodcm', 10, 0.5, 3, 0.0001, {'vc'});

---

4) Fischer mutual exclusion model:

UNSAFE:

Create model:
ha = SpaceExToStateflow('fischer_N2_flat_unsafe.xml', 'fischer_N2_flat_unsafe.cfg', '--folder', 'fischer', '-s');

Run simulations:
[time, valuesALL, labels] = simulationLoop('fischer_N2_flat_unsafe', 1000, 1000, 3);

Plot results:
plotExecution;


SAFE:

Create model:
ha = SpaceExToStateflow('fischer_N2_flat_safe.xml', 'fischer_N2_flat_safe.cfg', '--folder', 'fischer', '-s');

Run simulations:
[time, valuesALL, labels] = simulationLoop('fischer_N2_flat_safe', 1000, 1000, 3);

Plot results:
plotExecution;

HYRG README (TO REMOVE):

HyRG: A Random Generation Tool for Affine Hybrid Automata
Luan Viet Nguyen, Christian Schilling, Sergiy Bogomolov, Taylor T. Johnson 
verivital.uta.edu/hyrg/

HyRG has been tested in Matlab 2013b, 2014a, and 2014b.  You call it from a Matlab command prompt as follows.

Function call:
randgen_hybridautomaton(m, n, options)

Inputs:
m	: number of locations
n	: number of variables
options	: string list of options, specified below

Output:
Output SpaceEx model files are generated in ..\examples\rangen

Example calls:

1) The following generates an automaton with 3 state variables and 3 locations, where each mode has unstable dynamic(every mode's A matrix has all eigenvalues are positive real numbers). A generated automaton is state-depedent switching system, and it has no self-transition.

randgen_hybridautomaton(3,3)

2) The following generates an automaton with 4 state variables and 6 locations, where each mode has unstable dynamic(every mode's A matrix has all eigenvalues are positive real numbers). It also generates a SpaceEx model included of this automaton and another time dependent switching system(functions as a global time clock) for sanity checking.  

randgen_hybridautomaton(6,4,'-s')

2) The following generates an automaton with 4 state variables and 20 locations, where each mode has stable dynamic (e.g., every mode's A matrix is Hurwitz). A generated automaton is state-depedent switching sytem, and it has no self-transition.

randgen_hybridautomaton(20,4,'-nr')

3) The following generates an automaton with 3 state variables and 5 locations, where each mode may have different classes of dynamic. A generated automaton is state-depedent switching sytem, and it has no self-transition.

randgen_hybridautomaton(5,3,'-rd')

4) The following generates an automaton with 3 variables and 4 locations. This automaton is time-dependent swithcing('-t'). Each mode may have different classes of dynamic('-rd'). A generated automaton may have self-loop transitions('-g'). Invariants and guard conditions are randomly generated as inequalities of state variables and constant numbers('-ci'). State variables are updated to their random symbolic expression algebra('-sr'). And a generated automaton is also translated to Simulink/Stateflow(SLSF) model('-ss').

Output SLSF files (.mdl) are generated in .\output_SLSF
An SLSF files are automatically hooked up to scopes and can executable without anyadditional manual setting.

randgen_hybridautomaton(4,3,'-rd','-t','-g','-ss','-ci','-sr')


The following options are:
-t 	: randomly generate time-dependent switching system.
-g 	: randomly generate model included self-loop transitions.
-s 	: randomly generate model for sanity checking.
-ss 	: enable translation from SpaceEx XML model to SLSF model.
-ci 	: randomly generate constant invariants, constant guard conditions.
-si 	: randomly generate invariants and guard conditions as symbolic expression algebra of state variables.
-cr 	: update state variables to random constants.
-sr 	: update state variables to their random symbolic expression algebra.


Options for randomly generating different flow dynamics:  

Default	: randomly generate a flow dynamic based on a matrix whose all eigenvalues are positive real numbers. 

-z 	: randomly generate a flow dynamic based on a matrix whose all eigenvalues are zero. 
-n 	: randomly generate a flow dynamic based on a matrix whose all eigenvalues are negative real numbers
-pn 	: randomly generate a flow dynamic based on a matrix whose eigenvalues are either positive or negative real numbers.
-nz	: randomly generate a flow dynamic based on a matrix whose eigenvalues are either zero or negative real numbers.
-pnz 	: randomly generate a flow dynamic based on a matrix whose eigenvalues are positive, zero or negative real numbers.
-pr 	: randomly generate a flow dynamic based on a matrix whose eigenvalues are complex numbers with positive real parts.
-nr 	: generate a flow dynamic based on a matrix whose eigenvalues are complex numbers with negative real parts.
-i 	: generate a flow dynamic based on a matrix whose all eigenvalues are are complex numbers with purely imaginary.
-rd 	: generate a flow dynamic by randomly selecting one of all previous options.

Generating k examples with qualitatively similar behavior to known hybrid systems examples.
Inputs:
* k	: number of trials
* m	: number of locations
* n	: number of variables

Generate bouncing ball examples:

Bouncing ball system has one location and two variables:
ball_production(k,1,2)
E.g if we want to generate 100 examples of bouncing ball system:
ball_production(100,1,2)	


Generate thermostat(heater) system examples:
Themostat system has two locations and one variable
heater_production(k,2,1)
E.g if we want to generate 100 examples of thermostat system:
heater_production(100,2,1)
