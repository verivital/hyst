# you should source this file using a shell in the current directory. This assumes antlr4 is linked to run antlr, as instructed by the antrl4 setup. To source it, you can type ". make_java_files.sh"

antlr4 HystExpression.g4 -visitor -no-listener -package com.verivital.hyst.grammar.antlr

# to test locally exclude package: antlr4 HystExpression.g4 -visitor -no-listener
# then compile: javac *.java
# then test: grun HystExpression guardExpression -gui
# y < 3  <Ctrl+d>

# or, for a single-line script that does everything, do: 
#
# printf "\n\n\n\nRunning..." && antlr4 HystExpression.g4 -visitor -no-listener && javac *.java && echo "variable < 4" | grun HystExpression guardExpression -gui && rm *.class


