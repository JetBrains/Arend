package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.getTypes;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.patternMatchAll;

public class PatternExpansion {
  public static class Result {
    public final ArgumentExpression expression;
    public final List<TypeArgument> args;

    private Result(ArgumentExpression expression, List<TypeArgument> args) {
      this.expression = expression;
      this.args = args;
    }
  }

  public static Result expandPattern(Pattern pattern, Expression type, List<Binding> context) {
    if (pattern instanceof NamePattern || pattern instanceof AnyConstructorPattern) {
      return new Result(new ArgumentExpression(Index(0), pattern.getExplicit(), !pattern.getExplicit()),
          Collections.singletonList(pattern instanceof NamePattern ? Tele(pattern.getExplicit(), Collections.singletonList(((NamePattern) pattern).getName()), type) : TypeArg(pattern.getExplicit(), type)));
    } else if (pattern instanceof ConstructorPattern) {
      ConstructorPattern constructorPattern = (ConstructorPattern) pattern;

      List<Expression> parameters = new ArrayList<>();
      type.normalize(NormalizeVisitor.Mode.WHNF, context).getFunction(parameters);
      Collections.reverse(parameters);
      List<Result> nestedResults = expandPatterns(constructorPattern.getPatterns(), getTypes(getConstructorArguments(constructorPattern, parameters, context)), context);

      Expression resultExpression = ConCall(constructorPattern.getConstructor(), getMatchedParameters(constructorPattern, parameters));
      List<TypeArgument> resultArgs = new ArrayList<>();
      for (Result res : nestedResults) {
        resultExpression = Apps(resultExpression.liftIndex(0, res.args.size()), res.expression);
        resultArgs.addAll(res.args);
      }

      return new Result(new ArgumentExpression(resultExpression, pattern.getExplicit(), !pattern.getExplicit()), resultArgs);
    } else {
      throw new IllegalStateException();
    }
  }

  public static List<Result> expandPatterns(List<Pattern> patterns, List<Expression> types, List<Binding> context) {
    List<Result> results = new ArrayList<>();
    for (int i = 0; i < patterns.size(); i++) {
      Expression argumentType = types.get(i);
      for (int j = 0; j < i; j++)
        argumentType = expandPatternSubstitute(patterns.get(j), i - j - 1, results.get(j).expression.getExpression(), argumentType);
      Result nestedResult = expandPattern(patterns.get(i), argumentType, context);
      results.add(nestedResult);
    }
    return results;
  }

  private static List<Expression> getMatchedParameters(ConstructorPattern constructorPattern, List<Expression> dataTypeParameters) {
    if (constructorPattern.getConstructor().getPatterns() != null) {
      return ((Utils.PatternMatchOKResult) patternMatchAll(constructorPattern.getConstructor().getPatterns(), dataTypeParameters, new ArrayList<Binding>())).expressions;
    } else {
      return new ArrayList<>(dataTypeParameters);
    }
  }

  private static List<TypeArgument> getConstructorArguments(ConstructorPattern constructorPattern, List<Expression> dataTypeParameters, List<Binding> context) {
    List<Expression> matchedParameters = getMatchedParameters(constructorPattern, dataTypeParameters);
    Collections.reverse(matchedParameters);

    List<TypeArgument> constructorArguments = new ArrayList<>();
    splitArguments(constructorPattern.getConstructor().getType().subst(matchedParameters, 0), constructorArguments, context);
    return constructorArguments;
  }
}

