grammar Vcgrammar;

statements : statement*;

statement : staticMod definition                              # statDef
          | nsCmd nsCmdRoot fieldAcc* ('(' name (',' name)* ')')?  # statCmd
          | defaultStaticMod                                  # defaultStatic
          ;

nsCmdRoot : modulePath | name;

definition  : '\\function' precedence name tele* (':' expr)? arrow expr where?            # defFunction
            | '\\abstract' precedence name tele* ':' expr                                 # defAbstract
            // | '\\override' name ('\\as' name)? tele* typeTermOpt where?                   # defOverride
            | '\\data' precedence name tele* (':' expr)? constructorDef* conditionDef? # defData
            | classKindMod ID '{' statement* '}'                                          # defClass
            ;

conditionDef : '\\with' '|'? condition ('|' condition)*;

condition : name patternArg* '=>' expr;

classKindMod : '\\class'                # classClassMod
             | '\\module'               # moduleClassMod
             ;

staticMod : '\\static'                  # staticStatic
          | '\\dynamic'                 # dynamicStatic
          |                             # noStatic
          ;

defaultStaticMod : '\\allstatic'        # staticDefaultStatic
                 | '\\alldynamic'       # dynamicDefaultStatic
                 ;

where : '\\where' ('{' statement+ '}' | statement);

nsCmd : '\\open'                        # openCmd
      | '\\close'                       # closeCmd
      | '\\export'                      # exportCmd
      ;

arrow : '<='                            # arrowLeft
      | '=>'                            # arrowRight
      ;

typeTermOpt : ':' expr (arrow expr)?    # withType
            | arrow expr                # withoutType
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

patternArg : '(' pattern ')' # patternArgExplicit
           | '{' pattern '}' # patternArgImplicit
           | anyPattern      # patternArgAny
           | ID              # patternArgID
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

expr  : (binOpLeft+ | ) maybeNew atomFieldsAcc argument*    # binOp
      | <assoc=right> expr '->' expr                        # arr
      | '\\Pi' tele+ '->' expr                              # pi
      | '\\Sigma' tele+                                     # sigma
      | '\\Type' '(' expr ',' expr ')'                      # polyUniverse
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

binOpLeft : maybeNew atomFieldsAcc argument* infix;

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


atomFieldsAcc : atom fieldAcc* ('{' implementStatement* '}')?;

implementStatement : '|'? name '=>' expr;

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
