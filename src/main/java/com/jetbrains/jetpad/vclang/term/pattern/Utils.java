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

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.getTypes;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;

public class Utils {
  public static int getNumArguments(Pattern pattern) {
    if (pattern instanceof NamePattern || pattern instanceof AnyConstructorPattern) {
      return 1;
    } else if (pattern instanceof ConstructorPattern) {
      return getNumArguments(((ConstructorPattern) pattern).getArguments());
    } else {
      throw new IllegalStateException();
    }
  }

  public static int getNumArguments(List<? extends PatternArgument> patternArgs) {
    int result = 0;
    for (PatternArgument patternArg : patternArgs)
      result += getNumArguments(patternArg.getPattern());
    return result;
  }

  public static void prettyPrintPatternArg(Abstract.PatternArgument patternArg, StringBuilder builder, List<String> names) {
    builder.append(patternArg.isExplicit() ? "(" : "{");
    prettyPrintPattern(patternArg.getPattern(), builder, names);
    builder.append(patternArg.isExplicit() ? ")" : "}");
  }

  public static void prettyPrintPattern(Abstract.Pattern pattern, StringBuilder builder, List<String> names) {
    if (pattern instanceof Abstract.NamePattern) {
      if (((Abstract.NamePattern) pattern).getName() == null) {
        builder.append('_');
      } else {
        builder.append(((Abstract.NamePattern) pattern).getName());
      }
      names.add(((Abstract.NamePattern) pattern).getName());
    } else if (pattern instanceof Abstract.AnyConstructorPattern) {
      builder.append("_!");
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      builder.append(((Abstract.ConstructorPattern) pattern).getConstructorName());
      for (Abstract.PatternArgument patternArg : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        if (!patternArg.isHidden()) {
          builder.append(' ');
          prettyPrintPatternArg(patternArg, builder, names);
        }
      }
    }
  }

  public static class ProcessImplicitResult {
    public final List<Abstract.PatternArgument> patterns;
    public final int wrongImplicitPosition;
    public final int numExplicit;
    public final int numExcessive;

    public ProcessImplicitResult(List<Abstract.PatternArgument> patterns, int numExplicit) {
      this.patterns = patterns;
      this.wrongImplicitPosition = -1;
      this.numExplicit = numExplicit;
      this.numExcessive = 0;
    }

    public ProcessImplicitResult(int wrongImplicitPosition, int numExplicit) {
      this.patterns = null;
      this.wrongImplicitPosition = wrongImplicitPosition;
      this.numExplicit = numExplicit;
      this.numExcessive = 0;
    }

    public ProcessImplicitResult(int numExcessive) {
      this.patterns = null;
      this.numExcessive = numExcessive;
      this.numExplicit = -1;
      this.wrongImplicitPosition = -1;
    }
  }

  public static ProcessImplicitResult processImplicit(List<? extends Abstract.PatternArgument> patterns, List<? extends TypeArgument> arguments) {
    class Arg {
      String name;
      boolean isExplicit;

      public Arg(String name, boolean isExplicit) {
        this.name = name;
        this.isExplicit = isExplicit;
      }
    }

    ArrayList<Arg> args = new ArrayList<>();
    for (TypeArgument arg : arguments) {
      if (arg instanceof Abstract.TelescopeArgument) {
        for (String name : ((Abstract.TelescopeArgument) arg).getNames()) {
          args.add(new Arg(name, arg.getExplicit()));
        }
      } else {
        args.add(new Arg(null, arg.getExplicit()));
      }
    }

    int numExplicit = 0;
    for (Arg arg : args) {
      if (arg.isExplicit)
        numExplicit++;
    }

    List<Abstract.PatternArgument> result = new ArrayList<>();
    int indexI = 0;
    for (Arg arg : args) {
      Abstract.PatternArgument curPattern = indexI < patterns.size() ? patterns.get(indexI) : new PatternArgument(new NamePattern(arg.name), false, true);
      if (curPattern.isExplicit() && !arg.isExplicit) {
        curPattern = new PatternArgument(new NamePattern(arg.name), false, true);
      } else {
        indexI++;
      }
      if (curPattern.isExplicit() != arg.isExplicit) {
        return new ProcessImplicitResult(indexI, numExplicit);
      }
      result.add(curPattern);
    }
    if (indexI < patterns.size()) {
      return new ProcessImplicitResult(patterns.size() - indexI);
    }
    return new ProcessImplicitResult(result, numExplicit);
  }

  public static class PatternMatchResult {}

  public static class PatternMatchOKResult extends PatternMatchResult {
    public final List<Expression> expressions;

    PatternMatchOKResult(List<Expression> expressions) {
      this.expressions = expressions;
    }
  }

  public static class PatternMatchFailedResult extends PatternMatchResult {
    public final ConstructorPattern failedPattern;
    public final Expression actualExpression;

    PatternMatchFailedResult(ConstructorPattern failedPattern, Expression actualExpression) {
      this.failedPattern = failedPattern;
      this.actualExpression = actualExpression;
    }
  }

  public static class PatternMatchMaybeResult extends PatternMatchResult {
    public final Pattern maybePattern;
    public final Expression actualExpression;

    PatternMatchMaybeResult(Pattern maybePattern, Expression actualExpression) {
      this.maybePattern = maybePattern;
      this.actualExpression = actualExpression;
    }
  }

  public static List<Pattern> toPatterns(List<PatternArgument> patternArgs)  {
    List<Pattern> result = new ArrayList<>(patternArgs.size());
    for (PatternArgument patternArg : patternArgs) {
      result.add(patternArg.getPattern());
    }
    return result;
  }

  public static PatternMatchResult patternMatchAll(List<PatternArgument> patterns, List<Expression> exprs, List<Binding> context) {
    List<Expression> result = new ArrayList<>();
    assert patterns.size() == exprs.size();

    PatternMatchMaybeResult maybe = null;
    for (int i = 0; i < patterns.size(); i++) {
      PatternMatchResult subMatch = patterns.get(i).getPattern().match(exprs.get(i), context);
      if (subMatch instanceof PatternMatchFailedResult) {
        return subMatch;
      } else if (subMatch instanceof PatternMatchMaybeResult) {
        if (maybe == null)
          maybe = (PatternMatchMaybeResult) subMatch;
      } else if (subMatch instanceof PatternMatchOKResult) {
        result.addAll(((PatternMatchOKResult) subMatch).expressions);
      }
    }

    return maybe != null ? maybe : new PatternMatchOKResult(result);
  }

  public static List<TypeArgument> expandConstructorParameters(Constructor constructor, List<Binding> context) {
    List<PatternToExpression.Result<ArgumentExpression>> results = PatternToExpression.expandPatternArgs(constructor.getPatterns(), getTypes(constructor.getDataType().getParameters()), context);

    List<TypeArgument> result = new ArrayList<>();
    for (PatternToExpression.Result<ArgumentExpression> nestedResult : results) {
      for (TypeArgument arg : nestedResult.args) {
        result.add(arg.toExplicit(false));
      }
    }
    return result;
  }

  public static List<ArgumentExpression> constructorPatternsToExpressions(Constructor constructor) {
    List<PatternToExpression.Result<ArgumentExpression>> results = PatternToExpression.expandPatternArgs(constructor.getPatterns(), getTypes(constructor.getDataType().getParameters()), new ArrayList<Binding>());

    List<ArgumentExpression> result = new ArrayList<>();
    int shift = numberOfVariables(constructor.getArguments());
    int numArguments = 0;
    for (int i = results.size() - 1; i >= 0; i--) {
      result.add(new ArgumentExpression(results.get(i).expression.getExpression().liftIndex(0, numArguments + shift),
          results.get(i).expression.isExplicit(), results.get(i).expression.isHidden()));
      numArguments += results.get(i).args.size();
    }
    Collections.reverse(result);

    return result;
  }

  public static Expression expandPatternSubstitute(Pattern pattern, int varIndex, Expression what, Expression where) {
    return expandPatternSubstitute(getNumArguments(pattern), varIndex, what, where);
  }

  public static Expression expandPatternSubstitute(int numArguments, int varIndex, Expression what, Expression where) {
    return where.liftIndex(varIndex + 1, numArguments).subst(what.liftIndex(0, varIndex), varIndex);
  }
}
