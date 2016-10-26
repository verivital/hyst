# you should source this file using a shell in the current directory. This assumes antlr4 is linked to run antlr, as instructed by the antrl4 setup. To source it, you can type ". make_java_files.sh"

antlr4 HystExpression.g4 -visitor -no-listener -package com.verivital.hyst.grammar.antlr

# to test locally, make sure you first have aliases set:
#
# export CLASSPATH=".:/usr/local/lib/antlr-4.5-complete.jar:$CLASSPATH"
# alias antlr4='java -Xmx500M -cp "$CLASSPATH" org.antlr.v4.Tool'
# alias grun='java org.antlr.v4.runtime.misc.TestRig'
#
# then run: antlr4 HystExpression.g4 -visitor -no-listener
# then compile: javac *.java
# then test: grun HystExpression guardExpression -gui
# y < 3  <Ctrl+d>

# or, for a single-line script that runs everything (after you set the aliases up), do: 
#
# printf "\n\n\n\nRunning..." && antlr4 HystExpression.g4 -visitor -no-listener && javac *.java && echo "[1, -1]" | grun HystExpression addSub -gui && rm *.class


