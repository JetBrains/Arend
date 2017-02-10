grammar Vcgrammar;

statements : statement*;

statement : definition                                                        # statDef
          | nsCmd nsCmdRoot fieldAcc* (hidingOpt '(' name (',' name)* ')')?   # statCmd
          ;

hidingOpt : '\\hiding'  # withHiding
          |             # withoutHiding
          ;

nsCmdRoot : modulePath | name;

definition  : '\\function' precedence name tele* (':' expr)? arrow expr where?                            # defFunction
            | '\\field' precedence name ':' expr                                                          # defAbstract
            | '\\implement' name '=>' expr                                                                # defImplement
            | isTruncated '\\data' precedence name tele* (':' expr)? constructorDef* conditionDef?        # defData
            | '\\class' ID tele* ('\\extends' expr (',' expr)*)? ('{' statements '}')? where?             # defClass
            | '\\view' ID '\\on' expr '\\by' name '{' classViewField* '}'                                 # defClassView
            | defaultInst '\\instance' ID tele* '=>' expr                                                 # defInstance
            ;

isTruncated : '\\truncated' # truncated
            |               # notTruncated
            ;

defaultInst :             # noDefault
            | '\\default' # withDefault
            ;

classViewField : name ('=>' precedence name)? ;

conditionDef : '\\with' '|'? condition ('|' condition)*;

condition : name patternArg* '=>' expr;

where : '\\where' ('{' statements '}' | statement);

nsCmd : '\\open'                        # openCmd
      | '\\export'                      # exportCmd
      ;

arrow : '<='                            # arrowLeft
      | '=>'                            # arrowRight
      ;

constructorDef : '|' name patternArg* '=>' constructor ('|' constructor)* ';'? # withPatterns
               | '|' constructor                                               # noPatterns
               ;

anyPattern : '_'  # anyPatternAny
           | '_!' # anyPatternConstructor
           ;

pattern : anyPattern       # patternAny
        | name patternArg* # patternConstructor
        ;

patternArg : '(' pattern ')'    # patternArgExplicit
           | '{' pattern '}'    # patternArgImplicit
           | anyPattern         # patternArgAny
           | ID                 # patternArgID
           ;

constructor : precedence name tele*;

precedence :                            # noPrecedence
           | associativity NUMBER       # withPrecedence
           ;

associativity : '\\infix'               # nonAssoc
              | '\\infixl'              # leftAssoc
              | '\\infixr'              # rightAssoc
              ;

name  : ID                              # nameId
      | '(' BIN_OP ')'               # nameBinOp
      ;

expr  : (binOpLeft+ | ) binOpArg                            # binOp
      | <assoc=right> expr '->' expr                        # arr
      | '\\Pi' tele+ '->' expr                              # pi
      | '\\Sigma' tele+                                     # sigma
      | '\\lam' tele+ '=>' expr                             # lam
      | '\\let' '|'? letClause ('|' letClause)* '\\in' expr # let
      | elimCase expr (',' expr)* clause* ';'?              # exprElim
      ;

letClause : ID tele* typeAnnotation? arrow expr;

typeAnnotation : ':' expr;

clause : '|' pattern (',' pattern)* (arrow expr)?;

elimCase : '\\elim'                     # elim
         | '\\case'                     # case
         ;

levelMaxExpr : atom | '(max' '(' expr (',' expr)* ') )';

binOpArg : maybeNew atomFieldsAcc argument*                            # binOpArgument
      |  (TRUNCATED_UNIVERSE_PREFIX | UNIVERSE_PREFIX) (levelMaxExpr)? # polyUniverse
      | SET_PREFIX (levelMaxExpr)?                                     # polySet
      ;

binOpLeft : binOpArg infix;

maybeNew :                              # noNew
         | '\\new'                      # withNew
         ;

fieldAcc : '.' name                     # classField
         | '.' NUMBER                   # sigmaField
         ;

infix : BIN_OP                      # infixBinOp
      | '`' ID '`'                  # infixId
      ;

modulePath : ('::' ID)+;

atom  : literal                         # atomLiteral
      | '(' expr (',' expr)* ')'        # tuple
      | NUMBER                          # atomNumber
      | modulePath                      # atomModuleCall
      ;

atomFieldsAcc : atom fieldAcc* implementStatements?;

implementStatements : '{' implementStatement* '}';

implementStatement : '|'? name '=>' expr;

argument : atomFieldsAcc                # argumentExplicit
         | '{' expr '}'                 # argumentImplicit
         ;

literal : name                          # id
        | UNIVERSE                      # universe
        | TRUNCATED_UNIVERSE            # truncatedUniverse
        | SET                           # set
        | '\\Prop'                      # prop
        | 'Lvl'                         # lvl
        | '\\lp'                        # pParam
        | '\\lh'                        # hParam
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
LAMBDA : '\\lam';
TRUNCATED_UNIVERSE_PREFIX : '\\'[0-9]+'-Type';
UNIVERSE_PREFIX : '\\Type';
SET_PREFIX : '\\Set';
UNIVERSE : UNIVERSE_PREFIX[0-9]+;
TRUNCATED_UNIVERSE : TRUNCATED_UNIVERSE_PREFIX[0-9]+;
SET : SET_PREFIX[0-9]+;
COLON : ':';
ARROW : '->';
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' .*? '\r'? '\n' -> skip;
COMMENT : '{-' .*? '-}' -> skip;
fragment BIN_OP_CHAR : [~!@#$%^&*\-+=<>?/|.:];
BIN_OP : BIN_OP_CHAR+;
fragment ID_FRAGMENT : [a-zA-Z_] [a-zA-Z0-9_\']* | BIN_OP_CHAR+;
ID : ID_FRAGMENT ('-' ID_FRAGMENT)*;
ERROR_CHAR : .;
