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
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.splitArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.expandPatternSubstitute;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.getNumArguments;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.patternMatchAll;

class PatternExpansion {
  static class Result {
    final ArgumentExpression expression;
    final List<TypeArgument> args;

    private Result(ArgumentExpression expression, List<TypeArgument> args) {
      this.expression = expression;
      this.args = args;
    }
  }

  static ArgumentExpression expandPattern(Pattern pattern) {
    if (pattern instanceof NamePattern)  {
      return new ArgumentExpression(Index(0), pattern.getExplicit(), !pattern.getExplicit());
    } else if (pattern instanceof ConstructorPattern) {
      Expression resultExpression = DefCall(((ConstructorPattern) pattern).getConstructor());
      for (Pattern nestedPattern : ((ConstructorPattern) pattern).getArguments())
        resultExpression = Apps(resultExpression.liftIndex(0, getNumArguments(nestedPattern)), expandPattern(nestedPattern));

      return new ArgumentExpression(resultExpression, pattern.getExplicit(), !pattern.getExplicit());
    } else {
      throw new IllegalStateException();
    }
  }

  static Result expandPattern(Pattern pattern, Expression type) {
    if (pattern instanceof NamePattern) {
      return new Result(new ArgumentExpression(Index(0), pattern.getExplicit(), !pattern.getExplicit()),
          Collections.singletonList(TypeArg(pattern.getExplicit(), type)));
    } else if (pattern instanceof ConstructorPattern) {
      ConstructorPattern constructorPattern = (ConstructorPattern) pattern;

      List<Expression> parameters = new ArrayList<>();
      type.normalize(NormalizeVisitor.Mode.WHNF).getFunction(parameters);
      Collections.reverse(parameters);
      List<Result> nestedResults = expandPatterns(constructorPattern.getArguments(), getConstructorArguments(constructorPattern, parameters));

      Expression resultExpression = DefCall(null, constructorPattern.getConstructor(), parameters);
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

  static List<Result> expandPatterns(List<Pattern> patterns, List<TypeArgument> args) {
    List<TypeArgument> argsSplitted = new ArrayList<>();
    splitArguments(args, argsSplitted);

    List<Result> results = new ArrayList<>();
    for (int i = 0; i < patterns.size(); i++) {
      Expression argumentType = argsSplitted.get(i).getType();
      for (int j = 0; j < i; j++)
        argumentType = expandPatternSubstitute(patterns.get(j), i - j - 1, results.get(j).expression.getExpression(), argumentType);
      Result nestedResult = expandPattern(patterns.get(i), argumentType);
      results.add(nestedResult);
    }
    return results;
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
}

