grammar Vcgrammar;

defs  : def+;

def   : 'function' ID ':' expr1 '=' expr;

expr  : expr1                           # exprExpr1
      | '\\' ID+ '=>' expr              # lam
      ;

expr1 : expr1 expr1                     # app
      | <assoc=right> expr1 '->' expr1  # arr
      | <assoc=right> tele+ '->' expr1  # pi
      | '(' expr ')'                    # parens
      | UNIVERSE                        # universe
      | ID                              # id
      | 'N'                             # Nat
      | 'N-elim'                        # Nelim
      | '0'                             # zero
      | 'S'                             # suc
      ;

tele : '(' ID+ ':' expr1 ')'            # explicit
     | '{' ID+ ':' expr1 '}'            # implicit
     ;

UNIVERSE : 'Type' [0-9]+;
ID : [a-zA-Z_][a-zA-Z0-9_\-\']*;
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' .*? '\r'? '\n' -> skip;
COMMENT : '{-' .*? '-}' -> skip;