grammar Vcgrammar;

defs  : def*;

def   : '\\function' name tele* typeOpt arrow expr          # defFunction
      | '\\data' name tele* typeOpt arrowOpt constructor*   # defData
      ;

arrow : '<='                            # arrowLeft
      | '=>'                            # arrowRight
      ;

arrowOpt : | '<=';

typeOpt :                               # noType
        | ':' expr                      # withType
        ;

constructor : '|' name tele*;

name  : ID                              # nameId
      | '(' BIN_OP ')'                  # nameBinOp
      ;

expr  : binOpLeft* atom+                # binOp
      | <assoc=right> expr '->' expr    # arr
      | '\\Pi' tele+ '->' expr          # pi
      | '\\Sigma' tele+                 # sigma
      | '\\lam' tele+ '=>' expr         # lam
      | elimCase expr clause*           # exprElim
      ;

clause : '|' name ID* arrow expr;

elimCase : '\\elim'                     # elim
         | '\\case'                     # case
         ;

binOpLeft : atom+ infix;

infix : BIN_OP                          # infixBinOp
      | '`' ID '`'                      # infixId
      ;

atom  : '(' expr (',' expr)* ')'        # tuple
      | literal                         # atomLiteral
      ;

literal : UNIVERSE                      # universe
        | TRUNCATED_UNIVERSE            # truncatedUniverse
        | PROP                          # prop
        | SET                           # set
        | ID                            # id
        | 'N'                           # Nat
        | 'N-elim'                      # Nelim
        | '0'                           # zero
        | 'S'                           # suc
        | '_'                           # unknown
        ;

tele : literal                          # teleLiteral
     | '(' typedExpr ')'                # explicit
     | '{' typedExpr '}'                # implicit
     ;

typedExpr : expr                        # notTyped
          | expr ':' expr               # typed
          ;

UNIVERSE : '\\Type' [0-9]+;
TRUNCATED_UNIVERSE : '\\' [0-9]+ '-Type' [0-9]+;
PROP : '\\Prop';
SET : '\\Set' [0-9]+;
ID : [a-zA-Z_][a-zA-Z0-9_\-\']*;
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' .*? '\r'? '\n' -> skip;
COMMENT : '{-' .*? '-}' -> skip;
COLON : ':';
LAMBDA : '\\lam';
BIN_OP : [~!@#$%^&*-+=<>?/:|.]+;
