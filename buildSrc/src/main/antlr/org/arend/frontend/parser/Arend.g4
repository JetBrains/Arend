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

classFieldDef : defId tele* ':' returnExpr;

classFieldOrImpl : '|' classFieldDef    # classField
                 | localCoClause        # classImpl
                 ;

fieldMod  : '\\field'     # fieldField
          | '\\property'  # fieldProperty
          ;

classStat : classFieldOrImpl                            # classFieldOrImplStat
          | definition                                  # classDefinitionStat
          | fieldMod classFieldDef                      # classFieldStat
          | '\\override' longName tele* ':' returnExpr  # classOverrideStat
          ;

definition  : funcKw defId tele* (':' returnExpr)? functionBody where?                                          # defFunction
            | TRUNCATED? '\\data' defId tele* (':' expr)? dataBody where?                                       # defData
            | classKw defId NO_CLASSIFYING? fieldTele* ('\\extends' longName (',' longName)*)? classBody where? # defClass
            | '\\module' defId where?                                                                           # defModule
            | '\\meta' defId where?                                                                             # defMeta
            | instanceKw defId tele* (':' returnExpr)? instanceBody where?                                      # defInstance
            ;

returnExpr  : expr ('\\level' expr)?                # returnExprExpr
            | '\\level' atomFieldsAcc atomFieldsAcc # returnExprLevel
            ;

funcKw      : '\\func'            # funcKwFunc
            | '\\sfunc'           # funcKwSFunc
            | '\\lemma'           # funcKwLemma
            | '\\use' useMod      # funcKwUse
            ;

instanceKw  : '\\instance'        # funcKwInstance
            | '\\cons'            # funcKwCons
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

constructor : defId tele* /* TODO[hits] (':' expr)? */ (elim? '{' clause? ('|' clause)* '}')?;

defId : precedence ID alias?;

alias : '\\alias' precedence ID;

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

expr  : newExpr                                                           # app
      | <assoc=right> expr '->' expr                                      # arr
      | '\\Pi' tele+ '->' expr                                            # pi
      | '\\Sigma' tele*                                                   # sigma
      | '\\lam' tele+ ('=>' expr?)?                                       # lam
      | (LET | LETS) '|'? letClause ('|' letClause)* ('\\in' expr?)?      # let
      | (EVAL | PEVAL)? (CASE | SCASE) caseArg (',' caseArg)*
          ('\\return' returnExpr)? '\\with' withBody                      # case
      ;

withBody : '{' clause? ('|' clause)* '}';

newExpr : appPrefix? appExpr (implementStatements argument*)? (USING withBody)?;

appPrefix : NEW EVAL? | EVAL | PEVAL;

caseArg : caseArgExprAs (':' expr)?;

caseArgExprAs : '\\elim' (ID | APPLY_HOLE)   # caseArgElim
              | expr (AS ID)? # caseArgExpr
              ;

appExpr : argumentAppExpr                             # appArgument
        | TRUNCATED_UNIVERSE maybeLevelAtom?          # truncatedUniverse
        | UNIVERSE (maybeLevelAtom maybeLevelAtom?)?  # universe
        | SET maybeLevelAtom?                         # setUniverse
        ;

argumentAppExpr : atomFieldsAcc onlyLevelAtom* argument*;

argument : atomFieldsAcc                            # argumentExplicit
         | appPrefix appExpr implementStatements?   # argumentNew
         | universeAtom                             # argumentUniverse
         | '{' tupleExpr (',' tupleExpr)* ','? '}'  # argumentImplicit
         ;

clauses : '{' clause? ('|' clause)* '}' # clausesWithBraces
        | ('|' clause)*                 # clausesWithoutBraces
        ;

coClauses : coClause*                         # coClausesWithoutBraces
          | '{' coClause* '}'                 # coClausesWithBraces
          ;

clause : pattern (',' pattern)* ('=>' expr)?;

coClause : '|' (longName coClauseBody | precedence longName tele* (COLON returnExpr)? coClauseDefBody);

coClauseBody : tele* '=>' expr          # coClauseImpl
             | '{' localCoClause* '}'   # coClauseRec
             ;

coClauseDefBody : '=>' expr                                 # coClauseExpr
                | '\\cowith' coClauses                      # coClauseCowith
                | elim? ('{' clause? ('|' clause)* '}')?    # coClauseWith
                ;

localCoClause : '|' longName tele* ('=>' expr | '{' localCoClause* '}');

letClause : (ID tele* typeAnnotation? | tuplePattern) '=>' expr;

tuplePattern : ID typeAnnotation?                       # tuplePatternID
             | '(' tuplePattern (',' tuplePattern)* ')' # tuplePatternList
             | '_'                                      # tuplePatternUnknown
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

tupleExpr : expr (':' expr)?;

atom  : literal                                     # atomLiteral
      | '(' (tupleExpr (',' tupleExpr)* ','?)? ')'  # tuple
      | NUMBER                                      # atomNumber
      | STRING                                      # atomString
      | APPLY_HOLE                                  # atomApplyHole
      | NEGATIVE_NUMBER                             # atomNegativeNumber
      | '\\this'                                    # atomThis
      ;

atomFieldsAcc : atom ('.' NUMBER)*;

implementStatements : '{' localCoClause* '}';

longName : ID ('.' ID)*;

literal : longName ('.' (INFIX | POSTFIX))? # name
        | '\\Prop'                          # prop
        | '_'                               # unknown
        | INFIX                             # infix
        | POSTFIX                           # postfix
        | '{?' ID? ('(' expr? ')')? '}'     # goal
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

typedExpr : STRICT? expr (':' expr)? ;

fieldTele : '(' CLASSIFYING? ID+ ':' expr ')'        # explicitFieldTele
          | '{' CLASSIFYING? ID+ ':' expr '}'        # implicitFieldTele
          ;

LET : '\\let';
LETS : '\\let!';
STRICT : '\\strict';
EVAL : '\\eval';
PEVAL : '\\peval';
CASE : '\\case';
SCASE : '\\scase';
COMMA : ',';
AS : '\\as';
USING : '\\using';
TRUNCATED : '\\truncated';
CLASSIFYING : '\\classifying';
NO_CLASSIFYING : '\\noclassifying';
NEW : '\\new';
NUMBER : [0-9]+;
NEGATIVE_NUMBER : '-' [0-9]+;
UNIVERSE : '\\Type' [0-9]*;
TRUNCATED_UNIVERSE : '\\' (NUMBER '-' | 'oo-' | 'h') 'Type' [0-9]*;
SET : '\\Set' [0-9]*;
STRING : INCOMPLETE_STRING '"';
INCOMPLETE_STRING : '"' (~["\\\r\n] | ESCAPE_SEQ | EOF)*;
fragment ESCAPE_SEQ : '\\' [btnfr"'\\] | OCT_ESCAPE | UNICODE_ESCAPE;
fragment OCT_ESCAPE : '\\' OCT_DIGIT OCT_DIGIT? | '\\' [0-3] OCT_DIGIT OCT_DIGIT;
fragment UNICODE_ESCAPE : '\\' 'u'+ HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
fragment HEX_DIGIT : [0-9a-fA-F];
fragment OCT_DIGIT : [0-8];
COLON : ':';
ARROW : '->';
APPLY_HOLE : '__';
UNDERSCORE : '_';
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' '-'* (~[~!@#$%^&*\-+=<>?/|:[\u005Da-zA-Z_0-9'\u2200-\u22FF\r\n] ~[\r\n]* | ) -> skip;
COMMENT : '{-' (COMMENT|.)*? '-}' -> skip;
fragment START_CHAR : [~!@#$%^&*\-+=<>?/|:[\u005Da-zA-Z_\u2200-\u22FF];
ID : START_CHAR (START_CHAR | [0-9'])*;
INFIX : '`' ID '`';
POSTFIX : '`' ID;
INVALID_KEYWORD : '\\' ID;
ERROR_CHAR : .;
