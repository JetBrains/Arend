grammar Vcgrammar;

statements : statement* EOF;

statement : definition                                                      # statDef
          | nsCmd atomFieldsAcc nsUsing? ('\\hiding' '(' ID (',' ID)* ')')? # statCmd
          ;

nsCmd : '\\open'                        # openCmd
      | '\\import'                      # importCmd
      ;

nsUsing : USING? '(' nsId? (',' nsId)* ')';

nsId : ID ('\\as' precedence ID)?;

classStat : '|' precedence ID tele* ':' expr  # classField
          | '|' coClause                      # classImplement
          | definition                        # classDefinition
          ;

definition  : '\\func' precedence ID tele* (':' expr)? functionBody where?                              # defFunction
            | TRUNCATED? '\\data' precedence ID tele* (':' expr)? dataBody where?                       # defData
            | classKw precedence ID fieldTele* ('\\extends' classCall (',' classCall)*)? classBody? where? # defClass
            | '\\instance' ID tele* ':' expr coClauses where?                                           # defInstance
            ;

classKw   : '\\class'   # classKwClass
          | '\\record'  # classKwRecord
          ;

classBody : '{' classStat* '}'                      # classImpl
          | '=>' atomFieldsAcc ('{' fieldSyn? ('|' fieldSyn)* '}')? # classSyn
          ;

fieldSyn : ID '=>' precedence ID;

classCall : atomFieldsAcc;

functionBody  : '=>' expr     # withoutElim
              | elim? clauses # withElim
              ;

dataBody : elim constructorClause*                      # dataClauses
         | ('=>' '|'? constructor)? ('|' constructor)*  # dataConstructors
         ;

constructorClause : '|' pattern (',' pattern)* '=>' (constructor | '{' '|'? constructor ('|' constructor)* '}');

elim : '\\with' | '\\elim' atomFieldsAcc (',' atomFieldsAcc)*;

where : '\\where' ('{' statement* '}' | statement);

pattern : atomPattern             # patternAtom
        | ID atomPatternOrID*     # patternConstructor
        ;

atomPattern : '(' (pattern (',' pattern)*)? ')'   # patternExplicit
            | '{' pattern '}'                     # patternImplicit
            | NUMBER                              # patternNumber
            | '_'                                 # patternAny
            ;

atomPatternOrID : atomPattern     # patternOrIDAtom
                | ID              # patternID
                ;

constructor : precedence ID tele* (elim? '{' clause? ('|' clause)* '}')?;

precedence :                            # noPrecedence
           | associativity NUMBER       # withPrecedence
           ;

associativity : '\\infix'               # nonAssocInfix
              | '\\infixl'              # leftAssocInfix
              | '\\infixr'              # rightAssocInfix
              | '\\fix'                 # nonAssoc
              | '\\fixl'                # leftAssoc
              | '\\fixr'                # rightAssoc
              ;

expr  : NEW? appExpr (implementStatements argument*)?                         # app
      | <assoc=right> expr '->' expr                                          # arr
      | '\\Pi' tele+ '->' expr                                                # pi
      | '\\Sigma' tele*                                                       # sigma
      | '\\lam' tele+ '=>' expr                                               # lam
      | '\\let' '|'? letClause ('|' letClause)* '\\in' expr                   # let
      | '\\case' expr (',' expr)* '\\with' '{' clause? ('|' clause)* '}'      # case
      ;

appExpr : atomFieldsAcc onlyLevelAtom* argument*      # appArgument
        | TRUNCATED_UNIVERSE maybeLevelAtom?          # truncatedUniverse
        | UNIVERSE (maybeLevelAtom maybeLevelAtom?)?  # universe
        | SET maybeLevelAtom?                         # setUniverse
        ;

argument : atomFieldsAcc                # argumentExplicit
         | NEW appExpr implementStatements? # argumentNew
         | universeAtom                 # argumentUniverse
         | '{' expr '}'                 # argumentImplicit
         | INFIX                        # argumentInfix
         | POSTFIX                      # argumentPostfix
         ;

clauses : ('|' clause)*                 # clausesWithoutBraces
        | '{' clause? ('|' clause)* '}' # clausesWithBraces
        ;

coClauses : ('|' coClause)*                   # coClausesWithoutBraces
          | '{' coClause? ('|' coClause)* '}' # coClausesWithBraces
          ;

clause : pattern (',' pattern)* ('=>' expr)?;

coClause : atomFieldsAcc tele* ('=>' expr | '{' coClause? ('|' coClause)* '}');

letClause : ID tele* typeAnnotation? '=>' expr;

typeAnnotation : ':' expr;

levelAtom : '\\lp'              # pLevel
          | '\\lh'              # hLevel
          | NUMBER              # numLevel
          | '(' levelExpr ')'   # parenLevel
          ;

levelExpr : levelAtom                     # atomLevel
          | '\\suc' levelAtom             # sucLevel
          | '\\max' levelAtom levelAtom   # maxLevel
          ;

onlyLevelAtom : '\\lp'                                                # pOnlyLevel
              | '\\lh'                                                # hOnlyLevel
              | '\\levels' (maybeLevelAtom maybeLevelAtom | '\\Prop') # levelsOnlyLevel
              | '(' onlyLevelExpr ')'                                 # parenOnlyLevel
              ;

maybeLevelAtom : levelAtom  # withLevelAtom
               | '_'        # withoutLevelAtom
               ;

onlyLevelExpr : onlyLevelAtom                                         # atomOnlyLevel
              | '\\suc' levelAtom                                     # sucOnlyLevel
              | '\\max' levelAtom levelAtom                           # maxOnlyLevel
              ;

fieldAcc : ID                       # classFieldAcc
         | NUMBER                   # sigmaFieldAcc
         ;

atom  : literal                         # atomLiteral
      | '(' (expr (',' expr)*)? ')'     # tuple
      | NUMBER                          # atomNumber
      ;

atomFieldsAcc : atom ('.' fieldAcc)*;

implementStatements : '{' coClause? ('|' coClause)* '}';

literal : ID                            # name
        | '\\Prop'                      # prop
        | '_'                           # unknown
        | '{?' ID? ('{' expr '}')? '}'  # goal
        ;

universeAtom : TRUNCATED_UNIVERSE       # uniTruncatedUniverse
             | UNIVERSE                 # uniUniverse
             | SET                      # uniSetUniverse
             ;

tele : literal                          # teleLiteral
     | universeAtom                     # teleUniverse
     | '(' typedExpr ')'                # explicit
     | '{' typedExpr '}'                # implicit
     ;

typedExpr : expr                        # notTyped
          | expr ':' expr               # typed
          ;

fieldTele : '(' ID+ ':' expr ')'        # explicitFieldTele
          | '{' ID+ ':' expr '}'        # implicitFieldTele
          ;

USING : '\\using';
TRUNCATED : '\\truncated';
NEW : '\\new';
NUMBER : [0-9]+;
UNIVERSE : '\\Type' [0-9]*;
TRUNCATED_UNIVERSE : '\\' (NUMBER | 'oo') '-Type' [0-9]*;
SET : '\\Set' [0-9]*;
COLON : ':';
ARROW : '->';
UNDERSCORE : '_';
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' ~[\r\n]* -> skip;
COMMENT : '{-' .*? '-}' -> skip;
fragment START_CHAR : [~!@#$%^&*\-+=<>?/|:;[\]a-zA-Z_];
ID : START_CHAR (START_CHAR | [0-9'])*;
INFIX : '`' ID '`';
POSTFIX : '`' ID;
ERROR_CHAR : .;
