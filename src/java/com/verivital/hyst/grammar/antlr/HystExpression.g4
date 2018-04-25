grammar HystExpression;

WS : [ \t\r\n]+ -> skip;
TICK : '\'';

TRUE : 'true';
FALSE : 'false';

NUM : (([0-9]+ ('.' [0-9]+)?) | ('.' [0-9]+)) (('E' | 'e') [+|-]? [0-9]+)?;
VAR : [a-zA-Z_][a-zA-Z_0-9]*;

LPAR : '(';
RPAR : ')';
LBRAC : '[';
RBRAC : ']';
COMMA : ',';
SEMICOLON : ';';
COLON : ':';

PLUS : '+';
MINUS : '-';
TIMES : '*';
DIV : '/';
POW : '^';
DOT : '.';

AND : '&'['&']?;
OR : '|'['|']?;
NOT : '!';

LESS : '<';
GREATER : '>';
LESSEQUAL : '<=';
GREATEREQUAL : '>=';
NOTEQUAL : '!=';
EQUAL : '=='|'=';
EQUAL_RESET : ':=';

matrixRow
	: addSub (COMMA addSub)*  # MatrixRowExp
	;
	
matrixRange
	: addSub COLON addSub COLON addSub # MatrixRangeExp
	;

matrixExpression
	: LBRAC matrixRow (SEMICOLON matrixRow)* RBRAC # MatrixExplicit
	| LBRAC matrixRange RBRAC # MatrixGenerated
	;

functionExpression
	: VAR LPAR (addSub (COMMA addSub)*)? RPAR # Function
	;

// transition resets (guards)
resetSubExpression
	: VAR EQUAL_RESET addSub # ResetSubEq
	| addSub op addSub (op addSub)* # ResetSubOp
	;

resetExpression
    : EOF								    # ResetBlank
    | resetSubExpression (AND resetSubExpression)* EOF    # Reset
    ;

guardExpression
    : EOF	# GuardBlank
    | or EOF	# Guard
    ;

invariantExpression
    : EOF	# InvariantBlank
    | or EOF	# Invariant
    ;

flowExpression
    : EOF							# FlowBlank
    | VAR TICK? EQUAL addSub (AND VAR TICK? EQUAL addSub)* EOF	# Flow
	| FALSE # FlowFalse
    ;

dottedVar
	: VAR (DOT VAR)* TICK?	# DotVar
	;

locExpression
	: or EOF # LocExp
	| EOF # LocFalse
    ;

or
    : and OR or	# OrExpression
    | and   	# ToAnd
    ;

and
    : (not AND)* not # AndExpression
	;

not
	: NOT LPAR or RPAR	# NotExpression
    | LPAR or RPAR 		# BoolParentheses
	| compare			# ToCompare
    ;

op
	: EQUAL			# EqualOp
	| LESS 			# LessOp
	| LESSEQUAL		# LessEqualOp
	| GREATER		# GreaterOp
	| GREATEREQUAL	# GreaterEqualOp
	| NOTEQUAL		# NotEqualOp
	;

compare
    : addSub (op addSub)+	# BoolOp
    | TRUE   	      		    	# ConstTrue
    | FALSE			    			# ConstFalse
    ;

addSub
    : addSub PLUS timesDiv		# Plus
    | addSub MINUS timesDiv 	# Minus
    | timesDiv 	     	    	# ToTimesDiv
    ;

timesDiv
    : timesDiv TIMES pow    # Multiplication
    | timesDiv DIV pow      # Division
    | pow     		    # ToPow
    ;

pow
    : pow POW negativeUnary # PowExpression
    | negativeUnary         # ToNegativeUnary
    ;

negativeUnary
	: MINUS negativeUnary	# Negative
	| unary					# ToUnary
	;

unary
    : matrixExpression        # MatrixExp
	| functionExpression      # FuncExp
    | NUM      	   	      	  # Number
	| dottedVar				  # DottedVariable
    | LPAR addSub RPAR	      # Parentheses
    ;
