package com.jetbrains.jetpad.vclang.term.pattern;


import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utils {
  public static int getNumArguments(Abstract.Pattern pattern) {
    if (pattern instanceof Abstract.NamePattern) {
      return 1;
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      int result = 0;
      for (Abstract.Pattern nestedPattern : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        result += getNumArguments(nestedPattern);
      }
      return result;
    } else {
      throw new IllegalStateException();
    }
  }

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
    prettyPrintPattern(pattern, builder, names, false);
  }
  public static void prettyPrintPattern(Abstract.Pattern pattern, StringBuilder builder, List<String> names, boolean topLevel) {
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
      if (!topLevel)
        builder.append('(');
      builder.append(((Abstract.ConstructorPattern) pattern).getConstructorName());
      for (Abstract.Pattern p : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        builder.append(' ');
        prettyPrintPattern(p, builder, names, false);
      }
      if (!topLevel)
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


  public static List<TypeArgument> expandConstructorParameters(Constructor constructor) {
    List<PatternExpander.Result> results = new PatternExpander(0).expandPatterns(constructor.getPatterns(), constructor.getDataType().getParameters());

    List<TypeArgument> result = new ArrayList<>();
    for (PatternExpander.Result nestedResult : results) {
      result.addAll(nestedResult.args);
    }
    return result;
  }

  public static List<ArgumentExpression> patternsToExpressions(List<Pattern> patterns, List<TypeArgument> args, int startIndex) {
    int numberOfArguments = 0;
    for (Pattern pattern : patterns) {
      numberOfArguments += getNumArguments(pattern);
    }

    List<PatternExpander.Result> results = new PatternExpander(startIndex + numberOfArguments - 1).expandPatterns(patterns, args);

    List<ArgumentExpression> result = new ArrayList<>();
    for (PatternExpander.Result nestedResult : results) {
      result.add(nestedResult.expression);
    }

    return result;
  }

  public static ArgumentExpression patternToExpression(Pattern pattern) {
    return new PatternExpander(getNumArguments(pattern) - 1).expandPattern(pattern);
  }

  public static Expression expandPatternSubstitute(Pattern pattern, int varIndex, Expression what, Expression where) {
    int numArguments = getNumArguments(pattern);
    Expression expression = what.liftIndex(0, varIndex);
    if (numArguments > 0) {
      where = where.liftIndex(varIndex + 1, numArguments).subst(expression, varIndex);
    } else {
      where = where.subst(expression, varIndex);
    }
    return where;
  }
}
