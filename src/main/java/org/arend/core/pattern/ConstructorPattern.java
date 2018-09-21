package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.prelude.Prelude;

import java.util.*;

public class ConstructorPattern implements Pattern {
  private final Expression myExpression; // Either conCall, classCall, or Sigma.
  private final Patterns myPatterns;

  public ConstructorPattern(ConCallExpression conCall, Patterns patterns) {
    myExpression = conCall;
    myPatterns = patterns;
  }

  public ConstructorPattern(ClassCallExpression classCall, Patterns patterns) {
    myExpression = classCall;
    myPatterns = patterns;
  }

  public ConstructorPattern(SigmaExpression sigma, Patterns patterns) {
    myExpression = sigma;
    myPatterns = patterns;
  }

  public ConstructorPattern(ConstructorPattern pattern, Patterns patterns) {
    myExpression = pattern.myExpression;
    myPatterns = patterns;
  }

  public Patterns getPatterns() {
    return myPatterns;
  }

  public Expression getDataExpression() {
    return myExpression;
  }

  public Definition getDefinition() {
    return myExpression instanceof DefCallExpression ? ((DefCallExpression) myExpression).getDefinition() : null;
  }

  public List<Expression> getDataTypeArguments() {
    return myExpression instanceof ConCallExpression ? ((ConCallExpression) myExpression).getDataTypeArguments() : null;
  }

  public List<Pattern> getArguments() {
    return myPatterns.getPatternList();
  }

  public Sort getSortArgument() {
    return myExpression instanceof DefCallExpression ? ((DefCallExpression) myExpression).getSortArgument() : null;
  }

  public DependentLink getParameters() {
    return myExpression instanceof ConCallExpression
      ? ((ConCallExpression) myExpression).getDefinition().getParameters()
      : myExpression instanceof SigmaExpression
        ? ((SigmaExpression) myExpression).getParameters()
        : ((ClassCallExpression) myExpression).getClassFieldParameters();
  }

  public Expression toExpression(List<Expression> arguments) {
    if (myExpression instanceof SigmaExpression) {
      return new TupleExpression(arguments, (SigmaExpression) myExpression);
    }

    if (myExpression instanceof ConCallExpression) {
      ConCallExpression conCall = (ConCallExpression) myExpression;
      return ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments);
    }

    ClassCallExpression classCall = (ClassCallExpression) myExpression;
    Map<ClassField, Expression> implementations = new HashMap<>();
    int i = 0;
    for (ClassField field : classCall.getDefinition().getFields()) {
      implementations.put(field, arguments.get(i++));
    }
    return new NewExpression(new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), implementations, Sort.PROP));
  }

  @Override
  public Expression toExpression() {
    List<Expression> arguments = new ArrayList<>(myPatterns.getPatternList().size());
    for (Pattern pattern : myPatterns.getPatternList()) {
      Expression argument = pattern.toExpression();
      if (argument == null) {
        return null;
      }
      arguments.add(argument);
    }
    return toExpression(arguments);
  }

  @Override
  public DependentLink getFirstBinding() {
    return myPatterns.getFirstBinding();
  }

  @Override
  public DependentLink getLastBinding() {
    return myPatterns.getLastBinding();
  }

  public List<? extends Expression> getMatchingExpressionArguments(Expression expression) {
    if (myExpression instanceof SigmaExpression) {
      TupleExpression tuple = expression.checkedCast(TupleExpression.class);
      return tuple == null ? null : tuple.getFields();
    }

    if (myExpression instanceof ConCallExpression) {
      ConCallExpression conCall = expression.checkedCast(ConCallExpression.class);
      Constructor myConstructor = ((ConCallExpression) myExpression).getDefinition();
      if (conCall == null && (myConstructor == Prelude.ZERO || myConstructor == Prelude.SUC)) {
        IntegerExpression intExpr = expression.checkedCast(IntegerExpression.class);
        if (intExpr != null) {
          return myConstructor == Prelude.ZERO && intExpr.isZero()
            ? Collections.emptyList()
            : myConstructor == Prelude.SUC && !intExpr.isZero()
              ? Collections.singletonList(intExpr.pred())
              : null;
        }
      }
      if (conCall == null || conCall.getDefinition() != myConstructor) {
        return null;
      }
      return conCall.getDefCallArguments();
    }

    NewExpression newExpr = expression.checkedCast(NewExpression.class);
    if (newExpr == null) {
      return null;
    }
    List<Expression> arguments = new ArrayList<>();
    for (ClassField field : ((ClassCallExpression) myExpression).getDefinition().getFields()) {
      arguments.add(newExpr.getExpression().getImplementation(field, newExpr));
    }
    return arguments;
  }

  @Override
  public MatchResult match(Expression expression, List<Expression> result) {
    List<? extends Expression> arguments = getMatchingExpressionArguments(expression.normalize(NormalizeVisitor.Mode.WHNF));
    if (arguments != null) {
      return myPatterns.match(arguments, result);
    }

    if (myExpression instanceof ConCallExpression) {
      ConCallExpression conCall = expression.checkedCast(ConCallExpression.class);
      Constructor myConstructor = ((ConCallExpression) myExpression).getDefinition();
      if (conCall != null && conCall.getDefinition() != myConstructor) {
        return MatchResult.FAIL;
      }
      if (conCall == null && (myConstructor == Prelude.ZERO || myConstructor == Prelude.SUC)) {
        IntegerExpression intExpr = expression.checkedCast(IntegerExpression.class);
        if (intExpr != null && (myConstructor == Prelude.ZERO) != intExpr.isZero()) {
          return MatchResult.FAIL;
        }
      }
    }
    return MatchResult.MAYBE;
  }

  @Override
  public boolean unify(Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2) {
    if (other instanceof BindingPattern) {
      substitution2.add(((BindingPattern) other).getBinding(), toExpression());
      return true;
    }

    if (other instanceof ConstructorPattern) {
      ConstructorPattern conPattern = (ConstructorPattern) other;
      return (myExpression instanceof SigmaExpression && conPattern.myExpression instanceof SigmaExpression ||
              myExpression instanceof DefCallExpression && conPattern.myExpression instanceof DefCallExpression &&
                ((DefCallExpression) myExpression).getDefinition() == ((DefCallExpression) conPattern.myExpression).getDefinition())
        && myPatterns.unify(conPattern.myPatterns, substitution1, substitution2);
    }

    return false;
  }
}
