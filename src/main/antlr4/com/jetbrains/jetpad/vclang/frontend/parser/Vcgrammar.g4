grammar Vcgrammar;

statements : statement* EOF;

statement : definition                                                      # statDef
          | nsCmd atomFieldsAcc nsUsing? ('\\hiding' '(' id (',' id)* ')')? # statCmd
          ;

nsCmd : '\\open'                        # openCmd
      | '\\import'                      # importCmd
      ;

nsUsing : USING? '(' nsId? (',' nsId)* ')';

nsId : id ('\\as' precedence id)?;

classStat : '|' precedence id ':' expr  # classField
          | '|' id '=>' expr            # classImplement
          | definition                  # classDefinition
          ;

definition  : '\\function' precedence id tele* (':' expr)? functionBody where?                                              # defFunction
            | TRUNCATED? '\\data' precedence id tele* (':' expr)? dataBody where?                                           # defData
            | '\\class' precedence id tele* ('\\extends' atomFieldsAcc (',' atomFieldsAcc)*)? ('{' classStat* '}')? where?  # defClass
            | '\\view' precedence id '\\on' expr '\\by' id '{' classViewField* '}'                                          # defClassView
            | defaultInst '\\instance' id tele* '=>' expr                                                                   # defInstance
            ;

functionBody  : '=>' expr     # withoutElim
              | elim? clauses # withElim
              ;

dataBody : elim constructorClause*                      # dataClauses
         | ('=>' '|'? constructor)? ('|' constructor)*  # dataConstructors
         ;

constructorClause : '|' pattern (',' pattern)* '=>' (constructor | '{' '|'? constructor ('|' constructor)* '}');

elim : '\\with' | '=>' '\\elim' atomFieldsAcc (',' atomFieldsAcc)*;

defaultInst :             # noDefault
            | '\\default' # withDefault
            ;

classViewField : id ('=>' precedence id)? ;

where : '\\where' ('{' statement* '}' | statement);

pattern : atomPattern             # patternAtom
        | prefix atomPatternOrID* # patternConstructor
        ;

atomPattern : '(' pattern? ')'    # patternExplicit
            | '{' pattern '}'     # patternImplicit
            | '_'                 # patternAny
            ;

atomPatternOrID : atomPattern     # patternOrIDAtom
                | prefix          # patternID
                ;

constructor : precedence id tele* (elim? '{' clause? ('|' clause)* '}')?;

precedence :                            # noPrecedence
           | associativity NUMBER       # withPrecedence
           ;

associativity : '\\infix'               # nonAssoc
              | '\\infixl'              # leftAssoc
              | '\\infixr'              # rightAssoc
              ;

expr  : binOpLeft* binOpArg postfix*                                          # binOp
      | <assoc=right> expr '->' expr                                          # arr
      | '\\Pi' tele+ '->' expr                                                # pi
      | '\\Sigma' tele+                                                       # sigma
      | '\\lam' tele+ '=>' expr                                               # lam
      | '\\let' '|'? letClause ('|' letClause)* '\\in' expr                   # let
      | '\\case' expr (',' expr)* '\\with' '{' clause? ('|' clause)* '}'      # case
      ;

clauses : ('|' clause)*                 # clausesWithoutBraces
        | '{' clause? ('|' clause)* '}' # clausesWithBraces
        ;

letClause : id tele* typeAnnotation? '=>' expr;

typeAnnotation : ':' expr;

clause : pattern (',' pattern)* ('=>' expr)?;

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

binOpArg : NEW? atomFieldsAcc onlyLevelAtom* argument* implementStatements? # binOpArgument
         | TRUNCATED_UNIVERSE maybeLevelAtom?                                   # truncatedUniverse
         | UNIVERSE (maybeLevelAtom maybeLevelAtom?)?                           # universe
         | SET maybeLevelAtom?                                                  # setUniverse
         ;

binOpLeft : binOpArg postfix* infix;

fieldAcc : id                       # classFieldAcc
         | NUMBER                   # sigmaFieldAcc
         ;

atom  : literal                         # atomLiteral
      | '(' expr (',' expr)* ')'        # tuple
      | NUMBER                          # atomNumber
      ;

atomFieldsAcc : atom ('.' fieldAcc)*;

implementStatements : '{' implementStatement? ('|' implementStatement)* '}';

implementStatement : atomFieldsAcc '=>' expr;

argument : atomFieldsAcc                # argumentExplicit
         | universeAtom                 # argumentUniverse
         | '{' expr '}'                 # argumentImplicit
         ;

literal : prefix                        # name
        | '\\Prop'                      # prop
        | '_'                           # unknown
        | '{?' id? ('{' expr '}')? '}'  # goal
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
          | INFIX id* ':' expr          # typedVars
          | expr INFIX* ':' expr        # typed
          ;

id : PREFIX | INFIX;

prefix : PREFIX | PREFIX_INFIX;

infix : INFIX | INFIX_PREFIX;

postfix : POSTFIX_INFIX | POSTFIX_PREFIX;

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
fragment INFIX_CHAR : [~!@#$%^&*\-+=<>?/|:;[\]];
INFIX : INFIX_CHAR+;
PREFIX : (INFIX_CHAR | [a-zA-Z_]) (INFIX_CHAR | [a-zA-Z0-9_'])*;
PREFIX_INFIX : '`' INFIX;
INFIX_PREFIX : '`' PREFIX;
POSTFIX_INFIX : INFIX '`';
POSTFIX_PREFIX : PREFIX '`';
ERROR_CHAR : .;
