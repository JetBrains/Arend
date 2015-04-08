grammar Vcgrammar;

defs  : def+;

def   : '\\function' ID ':' expr1 '=>' expr;

expr  : expr1                           # exprExpr1
      | '\\lam' lamArg+ '=>' expr       # lam
      ;

lamArg  : ID                            # lamArgId
        | tele                          # lamArgTele
        ;

expr1 : expr1 expr1                     # app
      | <assoc=right> expr1 '->' expr1  # arr
      | '\\Pi' tele+ '->' expr1         # pi
      | '\\Sigma' tele+                 # sigma
      | '(' expr (',' expr)* ')'        # tuple
      | UNIVERSE                        # universe
      | ID                              # id
      | 'N'                             # Nat
      | 'N-elim'                        # Nelim
      | '0'                             # zero
      | 'S'                             # suc
      ;

tele : '(' typedExpr ')'                # explicit
     | '{' typedExpr '}'                # implicit
     ;

typedExpr : expr1                       # notTyped
          | expr1 ':' expr1             # typed
          ;

UNIVERSE : '\\Type' [0-9]+;
ID : [a-zA-Z_][a-zA-Z0-9_\-\']*;
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' .*? '\r'? '\n' -> skip;
COMMENT : '{-' .*? '-}' -> skip;
COLON : ':';
LAMBDA : '\\lam';