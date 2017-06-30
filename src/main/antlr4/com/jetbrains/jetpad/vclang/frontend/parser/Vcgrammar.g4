grammar Vcgrammar;

statements : statement*;

statement : definition                                                        # statDef
          | nsCmd nsCmdRoot fieldAcc* (hidingOpt '(' name (',' name)* ')')?   # statCmd
          ;

hidingOpt : '\\hiding'  # withHiding
          |             # withoutHiding
          ;

nsCmdRoot : modulePath | name;

definition  : '\\function' precedence name tele* (':' expr)? functionBody where?                          # defFunction
            | '\\field' precedence name ':' expr                                                          # defAbstract
            | '\\implement' name '=>' expr                                                                # defImplement
            | isTruncated '\\data' precedence name tele* (':' expr)? dataBody                             # defData
            | '\\class' ID tele* ('\\extends' expr (',' expr)*)? ('{' statements '}')? where?             # defClass
            | '\\view' ID '\\on' expr '\\by' name '{' classViewField* '}'                                 # defClassView
            | defaultInst '\\instance' ID tele* '=>' expr                                                 # defInstance
            ;

functionBody  : '=>' expr     # withoutElim
              | elim? clauses # withElim
              ;

dataBody : elim constructorClause*                      # dataClauses
         | ('=>' '|'? constructor)? ('|' constructor)*  # dataConstructors
         ;

constructorClause : '|' pattern (',' pattern)* '=>' (constructor | '{' '|'? constructor ('|' constructor)* '}');

elim : '\\with' | '=>' '\\elim' expr? (',' expr)*;

isTruncated : '\\truncated' # truncated
            |               # notTruncated
            ;

defaultInst :             # noDefault
            | '\\default' # withDefault
            ;

classViewField : name ('=>' precedence name)? ;

where : '\\where' ('{' statements '}' | statement);

nsCmd : '\\open'                        # openCmd
      | '\\export'                      # exportCmd
      ;

pattern : atomPattern             # patternAtom
        | name atomPatternOrID*   # patternConstructor
        ;

atomPattern : '(' pattern ')'     # patternExplicit
            | '{' pattern '}'     # patternImplicit
            | '()'                # patternEmpty
            | '_'                 # patternAny
            ;

atomPatternOrID : atomPattern     # patternOrIDAtom
                | ID              # patternID
                ;

constructor : precedence name tele* (elim clauses)?;

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

expr  : (binOpLeft+ | ) binOpArg                            # binOp
      | <assoc=right> expr '->' expr                        # arr
      | '\\Pi' tele+ '->' expr                              # pi
      | '\\Sigma' tele+                                     # sigma
      | '\\lam' tele+ '=>' expr                             # lam
      | '\\let' '|'? letClause ('|' letClause)* '\\in' expr # let
      | '\\case' expr (',' expr)* '\\with'? clauses         # case
      ;

clauses : clause*         # clausesWithoutBraces
        | '{' clause* '}' # clausesWithBraces
        ;

letClause : ID tele* typeAnnotation? '=>' expr;

typeAnnotation : ':' expr;

clause : '|' pattern (',' pattern)* ('=>' expr)?;

levelAtom : '\\lp'              # pLevel
          | '\\lh'              # hLevel
          | NUMBER              # numLevel
          | '(' levelExpr ')'   # exprLevel
          ;

levelExpr : levelAtom                     # atomLevelExpr
          | '\\suc' levelAtom             # sucLevelExpr
          | '\\max' levelAtom levelAtom   # maxLevelExpr
          ;

binOpArg : maybeNew atomFieldsAcc argument*       # binOpArgument
         | TRUNCATED_UNIVERSE levelAtom?          # truncatedUniverse
         | UNIVERSE levelAtom? levelAtom?         # universe
         | SET levelAtom?                         # setUniverse
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
         | universeAtom                 # argumentUniverse
         | '{' expr '}'                 # argumentImplicit
         ;

literal : name                          # id
        | '\\Prop'                      # prop
        | '_'                           # unknown
        | '{?}'                         # hole
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

NUMBER : [0-9]+;
UNIVERSE : '\\Type' [0-9]*;
TRUNCATED_UNIVERSE : '\\' (NUMBER | 'oo') '-Type' [0-9]*;
SET : '\\Set' [0-9]*;
COLON : ':';
ARROW : '->';
WS : [ \t\r\n]+ -> skip;
LINE_COMMENT : '--' ~[\r\n]* -> skip;
COMMENT : '{-' .*? '-}' -> skip;
fragment BIN_OP_CHAR : [~!@#$%^&*\-+=<>?/|.:];
BIN_OP : BIN_OP_CHAR+;
fragment ID_FRAGMENT : [a-zA-Z_] [a-zA-Z0-9_\']* | BIN_OP_CHAR+;
ID : ID_FRAGMENT ('-' ID_FRAGMENT)*;
ERROR_CHAR : .;
