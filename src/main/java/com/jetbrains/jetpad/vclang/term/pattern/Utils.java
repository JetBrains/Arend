package com.jetbrains.jetpad.vclang.term.pattern;


import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;

public class Utils {
  public static void collectPatternNames(Abstract.Pattern pattern, List<String> names) {
    if (pattern instanceof Abstract.NamePattern) {
      names.add(((Abstract.NamePattern) pattern).getName());
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      for (Abstract.Pattern nestedPattern : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        collectPatternNames(nestedPattern, names);
      }
    }
  }

  public static void prettyPrintPattern(Abstract.Pattern pattern, StringBuilder builder, List<String> names) {
    if (!pattern.getExplicit())
      builder.append('{');
    if (pattern instanceof Abstract.NamePattern) {
      if (((Abstract.NamePattern) pattern).getName() == null) {
        builder.append('_');
      } else {
        builder.append(((Abstract.NamePattern) pattern).getName());
      }
      names.add(((Abstract.NamePattern) pattern).getName());
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      builder.append('(');
      builder.append(((Abstract.ConstructorPattern) pattern).getConstructorName());
      builder.append(' ');
      for (Abstract.Pattern p : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        prettyPrintPattern(p, builder, names);
      }
      builder.append(')');
    }
    if (!pattern.getExplicit())
      builder.append('}');
  }

  public static class ProcessImplicitResult {
    public final List<Abstract.Pattern> patterns;
    public final int wrongImplicitPosition;
    public final int numExplicit;

    public ProcessImplicitResult(List<Abstract.Pattern> patterns, int numExplicit) {
      this.patterns = patterns;
      this.wrongImplicitPosition = -1;
      this.numExplicit = numExplicit;
    }

    public ProcessImplicitResult(int wrongImplicitPosition, int numExplicit) {
      this.patterns = null;
      this.wrongImplicitPosition = wrongImplicitPosition;
      this.numExplicit = numExplicit;
    }
  }

  public static ProcessImplicitResult processImplicit(List<? extends Abstract.Pattern> patterns, List<? extends Abstract.TypeArgument> arguments) {
    ArrayList<Boolean> argIsExplicit = new ArrayList<>();
    for (Abstract.TypeArgument arg : arguments) {
      if (arg instanceof Abstract.TelescopeArgument) {
        argIsExplicit.addAll(Collections.nCopies(((Abstract.TelescopeArgument) arg).getNames().size(), arg.getExplicit()));
      } else {
        argIsExplicit.add(arg.getExplicit());
      }
    }

    int numExplicit = 0;
    for (boolean b : argIsExplicit) {
      if (b)
        numExplicit++;
    }

    List<Abstract.Pattern> result = new ArrayList<>();
    for (int indexI = 0, indexJ = 0; indexJ < argIsExplicit.size(); ++indexJ) {
      Abstract.Pattern curPattern = indexI < patterns.size() ? patterns.get(indexI) : new NamePattern(null, false);
      if (curPattern.getExplicit() && !argIsExplicit.get(indexJ)) {
        curPattern = new NamePattern(null, false);
      } else {
        indexI++;
      }
      if (curPattern.getExplicit() != argIsExplicit.get(indexJ)) {
        return new ProcessImplicitResult(indexI, numExplicit);
      }
      result.add(curPattern);
    }
    return new ProcessImplicitResult(result, numExplicit);
  }

  public static class PatternMatchResult {
    public final List<Expression> expressions;

    public final ConstructorPattern failedPattern;
    public final Expression actualExpression;

    PatternMatchResult(List<Expression> expressions) {
      this.expressions = expressions;
      this.failedPattern = null;
      this.actualExpression = null;
    }

    PatternMatchResult(ConstructorPattern failedPattern, Expression actualExpression) {
      this.expressions = null;
      this.failedPattern = failedPattern;
      this.actualExpression = actualExpression;
    }
  }

  public static PatternMatchResult patternMatchAll(List<Pattern> patterns, List<Expression> exprs, List<Binding> context) {
    List<Expression> result = new ArrayList<>();
    assert patterns.size() == exprs.size();
    for (int i = 0; i < patterns.size(); i++) {
      PatternMatchResult subMatch = patterns.get(i).match(exprs.get(i), context);
      if (subMatch.expressions == null) {
        return subMatch;
      }
      result.addAll(subMatch.expressions);
    }
    return new PatternMatchResult(result);
  }

 private static class PatternExpander {
   private int myIndex;

   private static class Result {
     private final ArgumentExpression expression;
     private final List<TypeArgument> args;

     private Result(ArgumentExpression expression, List<TypeArgument> args) {
       this.expression = expression;
       this.args = args;
     }
   }

   private PatternExpander(int startIndex) {
     myIndex = startIndex;
   }

   private Result expandPattern(Pattern pattern, Expression type) {
     if (pattern instanceof NamePattern) {
       return new Result(new ArgumentExpression(Index(myIndex--), pattern.getExplicit(), !pattern.getExplicit()),
           Collections.singletonList(TypeArg(pattern.getExplicit(), type)));
     } else if (pattern instanceof ConstructorPattern) {
       ConstructorPattern constructorPattern = (ConstructorPattern) pattern;
       List<Expression> parameters = new ArrayList<>();
       type.getFunction(parameters);
       Collections.reverse(parameters);

       List<Result> nestedResults = expandPatterns(constructorPattern.getArguments(), getConstructorArguments(constructorPattern, parameters));

       List<TypeArgument> resultArgs = new ArrayList<>();
       Expression resultExpression = DefCall(null, constructorPattern.getConstructor(), parameters);
       for (Result res : nestedResults) {
         resultExpression = Apps(resultExpression, res.expression);
         resultArgs.addAll(res.args);
       }

       return new Result(new ArgumentExpression(resultExpression, pattern.getExplicit(), !pattern.getExplicit()), resultArgs);
     } else {
       throw new IllegalStateException();
     }
   }

   private List<Result> expandPatterns(List<Pattern> patterns, List<TypeArgument> args) {
     List<Result> results = new ArrayList<>();

     List<Expression> substituteExpressions = new ArrayList<>();
     for (int i = 0; i < patterns.size(); i++) {
       Result nestedResult = expandPattern(patterns.get(i), args.get(i).getType().subst(substituteExpressions, 0));
       results.add(nestedResult);
       substituteExpressions.add(nestedResult.expression.getExpression());
     }
     return results;
   }
  }

  private static List<TypeArgument> getConstructorArguments(ConstructorPattern constructorPattern, List<Expression> dataTypeParameters) {
    List<Expression> matchedParameters;
    if (constructorPattern.getConstructor().getPatterns() != null) {
      matchedParameters = patternMatchAll(constructorPattern.getConstructor().getPatterns(), dataTypeParameters, new ArrayList<Binding>()).expressions;
    } else {
      matchedParameters = dataTypeParameters;
    }
    Collections.reverse(matchedParameters);

    List<TypeArgument> constructorArguments = new ArrayList<>();
    splitArguments(constructorPattern.getConstructor().getType().subst(matchedParameters, 0), constructorArguments);
    return constructorArguments;
  }

  public static List<TypeArgument> expandArgs(List<Pattern> pattenrs, List<TypeArgument> args) {
    List<TypeArgument> argsSplitted = new ArrayList<>();
    splitArguments(args, argsSplitted);

    List<PatternExpander.Result> results = new PatternExpander(0).expandPatterns(pattenrs, argsSplitted);

    List<TypeArgument> result = new ArrayList<>();
    for (PatternExpander.Result nestedResult : results) {
      result.addAll(nestedResult.args);
    }
    return result;
  }

  public static List<ArgumentExpression> patternsToExpessions(List<Pattern> patterns, List<TypeArgument> args, int startIndex) {
    List<TypeArgument> argsSplitted = new ArrayList<>();
    splitArguments(args, argsSplitted);

    List<String> names = new ArrayList<>();
    for (Pattern pattern : patterns) {
      collectPatternNames(pattern, names);
    }

    List<PatternExpander.Result> results = new PatternExpander(startIndex + names.size() - 1).expandPatterns(patterns, argsSplitted);

    List<ArgumentExpression> result = new ArrayList<>();
    for (PatternExpander.Result nestedResult : results) {
      result.add(nestedResult.expression);
    }

    return result;
  }
}
