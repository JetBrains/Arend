grammar Vcgrammar;

defs  : def+;

def   : ID ':' typeTop '=' expr ';';

expr  : expr1                           # exprExpr1
      | '\\' ID+ '->' expr              # lam
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

tele : '(' ID+ ':' expr1 ')' ;

typeTopTele : '(' ID+ ':' expr1 ')'     # typeTopExplicit
            | '{' ID+ ':' expr1 '}'     # typeTopImplicit
            ;

typeTop : expr1                                     # typeTopExpr1
        | <assoc=right> typeTopTele+ '->' typeTop   # typeTopPi
        ;

UNIVERSE : 'Type' [0-9]+;
ID : [a-zA-Z_][a-zA-Z0-9_\-\']*;
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' .*? '\r'? '\n' -> skip;
COMMENT : '{-' .*? '-}' -> skip;