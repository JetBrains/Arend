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
  public static class Result<T> {
    public final T expression;
    public final List<TypeArgument> args;

    private Result(T expression, List<TypeArgument> args) {
      this.expression = expression;
      this.args = args;
    }
  }

  public static Result<Expression> expandPattern(Pattern pattern, Expression type, List<Binding> context) {
    if (pattern instanceof NamePattern || pattern instanceof AnyConstructorPattern) {
      return new Result<Expression>(Index(0), Collections.singletonList(pattern instanceof NamePattern ? Tele(true, Collections.singletonList(((NamePattern) pattern).getName()), type) : TypeArg(true, type)));
    } else if (pattern instanceof ConstructorPattern) {
      ConstructorPattern constructorPattern = (ConstructorPattern) pattern;

      List<Expression> parameters = new ArrayList<>();
      type.normalize(NormalizeVisitor.Mode.WHNF, context).getFunction(parameters);
      Collections.reverse(parameters);
      List<Result<ArgumentExpression>> nestedResults = expandPatternArgs(constructorPattern.getArguments(), getTypes(getConstructorArguments(constructorPattern, parameters, context)), context);

      Expression resultExpression = ConCall(constructorPattern.getConstructor(), getMatchedParameters(constructorPattern, parameters));
      List<TypeArgument> resultArgs = new ArrayList<>();
      for (Result<ArgumentExpression> res : nestedResults) {
        resultExpression = Apps(resultExpression.liftIndex(0, res.args.size()), res.expression);
        resultArgs.addAll(res.args);
      }

      return new Result<>(resultExpression, resultArgs);
    } else {
      throw new IllegalStateException();
    }
  }

  public static List<Result<ArgumentExpression>> expandPatternArgs(List<PatternArgument> patternArgs, List<Expression> types, List<Binding> context) {
    List<Result<ArgumentExpression>> results = new ArrayList<>();
    for (int i = 0; i < patternArgs.size(); i++) {
      Expression argumentType = types.get(i);
      for (int j = 0; j < i; j++)
        argumentType = expandPatternSubstitute(patternArgs.get(j).getPattern(), i - j - 1, results.get(j).expression.getExpression(), argumentType);
      Result<ArgumentExpression> nestedResult = expandPatternArg(patternArgs.get(i), argumentType, context);
      results.add(nestedResult);
    }
    return results;
  }

  private static Result<ArgumentExpression> expandPatternArg(PatternArgument patternArg, Expression type, List<Binding> context) {
    Result<Expression> result = expandPattern(patternArg.getPattern(), type, context);
    return new Result<>(new ArgumentExpression(result.expression, patternArg.isExplicit(), patternArg.isHidden()), result.args);
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

