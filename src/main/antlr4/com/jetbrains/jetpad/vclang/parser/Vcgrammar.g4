grammar Vcgrammar;

defs  : def*;

def   : '\\function' precedence name tele* typeOpt termOpt             # defFunction
      | '\\data' precedence name tele* typeOpt arrowOpt constructor*   # defData
      | '\\class' ID '{' defs '}'                                      # defClass
      ;

arrow : '<='                            # arrowLeft
      | '=>'                            # arrowRight
      ;

arrowOpt : | '<=';

typeOpt :                               # noType
        | ':' expr                      # withType
        ;

termOpt :                               # noTerm
        | arrow expr                    # withTerm
        ;

constructor : '|' precedence name tele*;

precedence :                            # noPrecedence
           | associativity NUMBER       # withPrecedence
           ;

associativity : '\\infix'               # nonAssoc
              | '\\infixl'              # leftAssoc
              | '\\infixr'              # rightAssoc
              ;

name  : ID                              # nameId
      | '(' BIN_OP ')'                  # nameBinOp
      ;

expr  : binOpLeft* atomFieldsAcc argument* # binOp
      | <assoc=right> expr '->' expr    # arr
      | '\\Pi' tele+ '->' expr          # pi
      | '\\Sigma' tele+                 # sigma
      | '\\lam' tele+ '=>' expr         # lam
      | elimCase expr clause* ';'?      # exprElim
      ;

clauseName
      : '_'                             # clauseNoName
      | name tele*                      # clauseNameArgs
      ;

clause : '|' clauseName arrow expr;

elimCase : '\\elim'                     # elim
         | '\\case'                     # case
         ;

binOpLeft : atomFieldsAcc argument* infix;

fieldAcc : '.' name                     # classField
         | '.' NUMBER                   # sigmaField
         ;

infix : BIN_OP                          # infixBinOp
      | '`' name fieldAcc* '`'          # infixId
      ;

atom  : '(' expr (',' expr)* ')'        # tuple
      | literal                         # atomLiteral
      | NUMBER                          # atomNumber
      ;

atomFieldsAcc : atom fieldAcc*;

argument : atomFieldsAcc                # argumentExplicit
         | '{' expr '}'                 # argumentImplicit
         ;

literal : UNIVERSE                      # universe
        | TRUNCATED_UNIVERSE            # truncatedUniverse
        | PROP                          # prop
        | SET                           # set
        | name                          # id
        | '_'                           # unknown
        | '{?}'                         # hole
        ;

tele : literal                          # teleLiteral
     | '(' typedExpr ')'                # explicit
     | '{' typedExpr '}'                # implicit
     ;

typedExpr : expr                        # notTyped
          | expr ':' expr               # typed
          ;

NUMBER : [0-9]+;
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
ARROW : '->';
BIN_OP : [~!@#$%^&*\-+=<>?/:|.]+;
