grammar HystExpression;

WS : [ \t\r\n]+ -> skip;
TICK : '\'';

SIN : 'sin';
COS : 'cos';
TAN : 'tan';
EXP : 'exp';
SQRT : 'sqrt';
LN : 'ln';
LUT : 'lut';

LOC : 'loc';
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

varListExpression
	: LBRAC VAR (COMMA? VAR)* RBRAC # VarList
	;

matrixRowExpression
	: (NUM COMMA?)+		# MatrixRow
	;

matrixExpression
	: LBRAC matrixRowExpression (SEMICOLON matrixRowExpression)* (SEMICOLON)? RBRAC # Matrix
	;

lutExpression
	: LUT LPAR varListExpression COMMA matrixExpression COMMA matrixExpression RPAR # Lut
	;

resetSubExpression
	: VAR EQUAL_RESET addSub # ResetSubEq
	| addSub op addSub (op addSub)* # ResetSubOp
	;

resetExpression
	:
    (resetSubExpression (AND resetSubExpression))* EOF    # Reset
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
    : (VAR TICK? EQUAL addSub (AND VAR TICK? EQUAL addSub))* EOF	# Flow
	| FALSE # FlowFalse
    ;

dottedVar
	: VAR (DOT VAR)* TICK?	# DotVar
	;

locSubExpression
	: LOC LPAR dottedVar RPAR EQUAL VAR # LocSubExp
	| LOC LPAR RPAR EQUAL VAR # LocSubBlankExp
	| and # LocAndExp
	;

locExpression
	: locSubExpression (AND locSubExpression)* EOF # LocExp
	| FALSE # LocFalse
    ;

or
    : and OR or	# OrExpression
    | and   	# ToAnd
    ;

and
    : not AND and # AndExpression
    | not 	      # ToNot
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
    : addSub op addSub (op addSub)*	# BoolOp
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
    : lutExpression 		  # LutFunc
	| TAN LPAR addSub RPAR    # TanFunc
    | SQRT LPAR addSub RPAR   # SqrtFunc
    | SIN LPAR addSub RPAR    # SinFunc
    | COS LPAR addSub RPAR    # CosFunc
    | EXP LPAR addSub RPAR    # ExpFunc
    | LN  LPAR addSub RPAR    # LnFunc
    | NUM      	   	      	  # Number
	| dottedVar				  # DottedVariable
    | LPAR addSub RPAR	      # Parentheses
    ;
