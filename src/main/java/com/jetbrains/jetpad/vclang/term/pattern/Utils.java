package com.jetbrains.jetpad.vclang.term.pattern;


import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Apps;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.DefCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Index;
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

  public static class ProcessImplcitResult {
    public final List<Abstract.Pattern> patterns;
    public final int wrongImplicitPosition;
    public final int numExplicit;

    public ProcessImplcitResult(List<Abstract.Pattern> patterns, int numExplicit) {
      this.patterns = patterns;
      this.wrongImplicitPosition = -1;
      this.numExplicit = numExplicit;
    }

    public ProcessImplcitResult(int wrongImplicitPosition, int numExplicit) {
      this.patterns = null;
      this.wrongImplicitPosition = wrongImplicitPosition;
      this.numExplicit = numExplicit;
    }
  }

  public static ProcessImplcitResult processImplicit(List<? extends Abstract.Pattern> patterns, List<? extends Abstract.TypeArgument> arguments) {
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
        return new ProcessImplcitResult(indexI, numExplicit);
      }
      result.add(curPattern);
    }
    return new ProcessImplcitResult(result, numExplicit);
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

  public static class PatternToArgumentConverter {
    private int myIndex;

    public PatternToArgumentConverter(int startIndex) {
      myIndex = startIndex;
    }

    public ArgumentExpression patternToArgument(Pattern pattern, Expression type) {
      if (pattern instanceof NamePattern) {
        return new ArgumentExpression(Index(myIndex--), pattern.getExplicit(), !pattern.getExplicit());
      } else if (pattern instanceof ConstructorPattern) {
        ConstructorPattern constructorPattern = (ConstructorPattern) pattern;
        List<Expression> parameters = new ArrayList<>();
        type.getFunction(parameters);
        Collections.reverse(parameters);
        List<TypeArgument> constructorArguments = new ArrayList<>();
        splitArguments(constructorPattern.getConstructor().getType(), constructorArguments);
        List<Expression> substituteExpressions = new ArrayList<>();
        Expression result = DefCall(constructorPattern.getConstructor());
        for (int i = 0; i < constructorPattern.getArguments().size(); i++) {
          Expression nestedType = constructorArguments.get(i).getType().subst(parameters, i).subst(substituteExpressions, 0);
          ArgumentExpression nestedResult = patternToArgument(constructorPattern.getArguments().get(i), nestedType);
          result = Apps(result, nestedResult.getExpression());
          substituteExpressions.add(nestedResult.getExpression());
        }
        return new ArgumentExpression(result, pattern.getExplicit(), !pattern.getExplicit());
      } else {
        throw new IllegalStateException();
      }
    }
  }
}
