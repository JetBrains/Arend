grammar Arend;

statements : statement* EOF;

statement : definition                                                      # statDef
          | nsCmd longName nsUsing? ('\\hiding' '(' ID (',' ID)* ')')?      # statCmd
          ;

nsCmd : '\\open'                        # openCmd
      | '\\import'                      # importCmd
      ;

nsUsing : USING? '(' nsId? (',' nsId)* ')';

nsId : ID (AS precedence ID)?;

classFieldOrImpl : fieldMod precedence ID tele* ':' returnExpr # classField
                 | coClause                                    # classImpl
                 ;

fieldMod  : '|'           # fieldPipe
          | '\\field'     # fieldField
          | '\\property'  # fieldProperty
          ;

classStat : classFieldOrImpl                  # classFieldOrImplStat
          | definition                        # classDefinitionStat
          ;

definition  : funcKw precedence ID tele* (':' returnExpr)? functionBody where?                              # defFunction
            | TRUNCATED? '\\data' precedence ID tele* (':' expr)? dataBody where?                           # defData
            | classKw precedence ID fieldTele* ('\\extends' longName (',' longName)*)? classBody where?     # defClass
            | '\\module' ID where?                                                                          # defModule
            | '\\instance' precedence ID tele* (':' returnExpr)? instanceBody where?                        # defInstance
            ;

returnExpr  : expr                                  # returnExprExpr
            | '\\level' atomFieldsAcc atomFieldsAcc # returnExprLevel
            ;

funcKw    : '\\func'            # funcKwFunc
          | '\\lemma'           # funcKwLemma
          | '\\use' useMod      # funcKwUse
          ;

useMod    : '\\coerce'          # useCoerce
          | '\\level'           # useLevel
          ;

classKw   : '\\class'   # classKwClass
          | '\\record'  # classKwRecord
          ;

classBody : '{' classStat* '}'                                      # classBodyStats
          | classFieldOrImpl*                                       # classBodyFieldOrImpl
          ;

fieldSyn : ID '=>' precedence ID;

functionBody  : '=>' expr             # withoutElim
              | '\\cowith' coClauses  # cowithElim
              | elim? clauses         # withElim
              ;

instanceBody  : '=>' expr             # instanceWithoutElim
              | elim clauses          # instanceWithElim
              | '\\cowith' coClauses  # instanceCowithElim
              | coClause*             # instanceCoclauses
              ;

dataBody : elim constructorClauses                      # dataClauses
         | ('=>' '|'? constructor)? ('|' constructor)*  # dataConstructors
         ;

constructorClauses : '{' constructorClause* '}' # conClausesWithBraces
                   | constructorClause*         # conClausesWithoutBraces
                   ;

constructorClause : '|' pattern (',' pattern)* '=>' (constructor | '{' '|'? constructor ('|' constructor)* '}');

elim : '\\with' | '\\elim' ID (',' ID)*;

where : '\\where' ('{' statement* '}' | statement);

pattern : atomPattern (AS ID (':' expr)?)?                # patternAtom
        | longName atomPatternOrID* (AS ID)? (':' expr)?  # patternConstructor
        ;

atomPattern : '(' (pattern (',' pattern)*)? ')'   # patternExplicit
            | '{' pattern '}'                     # patternImplicit
            | NUMBER                              # patternNumber
            | NEGATIVE_NUMBER                     # patternNegativeNumber
            | '_'                                 # patternAny
            ;

atomPatternOrID : atomPattern     # patternOrIDAtom
                | longName        # patternID
                ;

constructor : precedence ID tele* /* TODO[hits] (':' expr)? */ (elim? '{' clause? ('|' clause)* '}')?;

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

expr  : NEW? appExpr (implementStatements argument*)?                                                   # app
      | <assoc=right> expr '->' expr                                                                    # arr
      | '\\Pi' tele+ '->' expr                                                                          # pi
      | '\\Sigma' tele*                                                                                 # sigma
      | '\\lam' tele+ '=>' expr                                                                         # lam
      | (LET | LETS) '|'? letClause ('|' letClause)* '\\in' expr                                        # let
      | '\\case' caseArg (',' caseArg)* ('\\return' returnExpr)? '\\with' '{' clause? ('|' clause)* '}' # case
      ;

caseArg : expr (AS ID)? (':' expr)?;

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

clauses : '{' clause? ('|' clause)* '}' # clausesWithBraces
        | ('|' clause)*                 # clausesWithoutBraces
        ;

coClauses : coClause*                         # coClausesWithoutBraces
          | '{' coClause* '}'                 # coClausesWithBraces
          ;

clause : pattern (',' pattern)* ('=>' expr)?;

coClause : '|' longName tele* ('=>' expr | '{' coClause* '}');

letClause : (ID tele* typeAnnotation? | tuplePattern) '=>' expr;

tuplePattern : ID typeAnnotation?                       # tuplePatternID
             | '(' tuplePattern (',' tuplePattern)* ')' # tuplePatternList
             ;

typeAnnotation : ':' expr;

levelAtom : '\\lp'              # pLevel
          | '\\lh'              # hLevel
          | '\\oo'              # infLevel
          | NUMBER              # numLevel
          | '(' levelExpr ')'   # parenLevel
          ;

levelExpr : levelAtom                     # atomLevel
          | '\\suc' levelAtom             # sucLevel
          | '\\max' levelAtom levelAtom   # maxLevel
          ;

onlyLevelAtom : '\\lp'                                                # pOnlyLevel
              | '\\lh'                                                # hOnlyLevel
              | '\\oo'                                                # infOnlyLevel
              | '\\level' (maybeLevelAtom maybeLevelAtom | '\\Prop')  # levelsOnlyLevel
              | '(' onlyLevelExpr ')'                                 # parenOnlyLevel
              ;

maybeLevelAtom : levelAtom  # withLevelAtom
               | '_'        # withoutLevelAtom
               ;

onlyLevelExpr : onlyLevelAtom                                         # atomOnlyLevel
              | '\\suc' levelAtom                                     # sucOnlyLevel
              | '\\max' levelAtom levelAtom                           # maxOnlyLevel
              ;

tupleExpr : expr (':' expr)?;

atom  : literal                               # atomLiteral
      | '(' (tupleExpr (',' tupleExpr)*)? ')' # tuple
      | NUMBER                                # atomNumber
      | NEGATIVE_NUMBER                       # atomNegativeNumber
      | '\\this'                              # atomThis
      ;

atomFieldsAcc : atom ('.' NUMBER)*;

implementStatements : '{' coClause* '}';

longName : ID ('.' ID)*;

literal : longName                      # name
        | '\\Prop'                      # prop
        | '_'                           # unknown
        | '{?' ID? ('(' expr? ')')? '}' # goal
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

fieldTele : '(' CLASSIFYING? ID+ ':' expr ')'        # explicitFieldTele
          | '{' CLASSIFYING? ID+ ':' expr '}'        # implicitFieldTele
          ;

LET : '\\let';
LETS : '\\let!';
AS : '\\as';
USING : '\\using';
TRUNCATED : '\\truncated';
CLASSIFYING : '\\classifying';
NEW : '\\new';
NUMBER : [0-9]+;
NEGATIVE_NUMBER : '-' [0-9]+;
UNIVERSE : '\\Type' [0-9]*;
TRUNCATED_UNIVERSE : '\\' (NUMBER | 'oo') '-Type' [0-9]*;
SET : '\\Set' [0-9]*;
COLON : ':';
ARROW : '->';
UNDERSCORE : '_';
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' '-'* (~[~!@#$%^&*\-+=<>?/|:[\u005Da-zA-Z_0-9'\r\n] ~[\r\n]* | ) -> skip;
COMMENT : '{-' (COMMENT|.)*? '-}' -> skip;
fragment START_CHAR : [~!@#$%^&*\-+=<>?/|:[\u005Da-zA-Z_];
ID : START_CHAR (START_CHAR | [0-9'])*;
INFIX : '`' ID '`';
POSTFIX : '`' ID;
INVALID_KEYWORD : '\\' ID;
ERROR_CHAR : .;
