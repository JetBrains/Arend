grammar Vcgrammar;

defs  : def+;

def   : '\\function' name ':' expr1 '=>' expr;

name  : ID                              # nameId
      | '(' BIN_OP ')'                  # nameBinOp
      ;

expr  : expr1                           # exprExpr1
      | '\\lam' lamArg+ '=>' expr       # lam
      ;

lamArg  : ID                            # lamArgId
        | tele                          # lamArgTele
        ;

expr1 : binOpLeft* atom+                # expr1BinOp
      | <assoc=right> expr1 '->' expr1  # arr
      | '\\Pi' tele+ '->' expr1         # pi
      | '\\Sigma' tele+                 # sigma
      ;

binOpLeft : atom+ infix;

infix : BIN_OP                          # infixBinOp
      | '`' ID '`'                      # infixId
      ;

atom  : '(' expr (',' expr)* ')'        # tuple
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
BIN_OP : [~!@#$%^&*-+=<>?/:|.]+;
