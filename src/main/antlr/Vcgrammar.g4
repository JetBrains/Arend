grammar Vcgrammar;

defs  : def+;

def   : ID ':' expr '=' expr ';';

expr  : expr1                           # exprExpr1
      | '\\' ID '->' expr               # lam
      ;

expr1 : expr1 expr1                     # app
      | <assoc=right> expr1 '->' expr1  # arr
      | '(' expr ')'                    # parens
      | ID                              # id
      | 'N'                             # Nat
      | 'N-elim'                        # Nelim
      | '0'                             # zero
      | 'S'                             # suc
      ;

/*
expr  : expr1                           # expr
      | '\\' ID '->' expr               # lam
      ;
expr1 : expr1 expr1                     # app
      | <assoc=right> expr1 '->' expr1  # arr
      | '(' expr1 ')'                   # parens
      | ID                              # id
      | 'N'                             # Nat
      | 'N-elim'                        # Nelim
      | '0'                             # zero
      | 'S'                             # suc
      ;
*/

/*
expr  : '\\' ID '->' expr               # lam
      | expr expr                       # app
      | <assoc=right> expr '->' expr    # arr
      | '(' expr ')'                    # parens
      | ID                              # id
      | 'N'                             # Nat
      | 'N-elim'                        # Nelim
      | '0'                             # zero
      | 'S'                             # suc
      ;
*/

/*
expr  : '\\' ID '->' expr+              # lam
      | <assoc=right> expr+ '->' expr+  # arr
      | '(' expr+ ')'                   # parens
      | ID                              # id
      | 'N'                             # Nat
      | 'N-elim'                        # Nelim
      | '0'                             # zero
      | 'S'                             # suc
      ;
*/

ID : [a-zA-Z_][a-zA-Z0-9_\-\']*;
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' .*? '\r'? '\n' -> skip;
COMMENT : '{-' .*? '-}' -> skip;