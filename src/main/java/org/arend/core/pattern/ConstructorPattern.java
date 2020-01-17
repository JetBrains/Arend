package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizingFindBindingVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;

import java.util.*;

public class ConstructorPattern implements Pattern {
  private final Expression myExpression; // Either conCall, classCall, Sigma, or FunCall(idp).
  private final Patterns myPatterns;

  private ConstructorPattern(Expression expression, Patterns patterns) {
    myExpression = expression;
    myPatterns = patterns;
  }

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

  public ConstructorPattern(FunCallExpression funCall, Patterns patterns) {
    myExpression = funCall;
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

  public List<? extends Expression> getDataTypeArguments() {
    return myExpression instanceof ConCallExpression
      ? ((ConCallExpression) myExpression).getDataTypeArguments()
      : myExpression instanceof FunCallExpression
        ? ((FunCallExpression) myExpression).getDefCallArguments()
        : null;
  }

  public List<Pattern> getArguments() {
    return myPatterns.getPatternList();
  }

  public Sort getSortArgument() {
    return myExpression instanceof DefCallExpression ? ((DefCallExpression) myExpression).getSortArgument() : null;
  }

  public DependentLink getParameters() {
    return myExpression instanceof ClassCallExpression
      ? ((ClassCallExpression) myExpression).getClassFieldParameters()
      : myExpression instanceof FunCallExpression
        ? EmptyDependentLink.getInstance()
        : myExpression instanceof DefCallExpression
          ? ((DefCallExpression) myExpression).getDefinition().getParameters()
          : ((SigmaExpression) myExpression).getParameters();
  }

  public int getLength() {
    return myExpression instanceof ConCallExpression
      ? DependentLink.Helper.size(((ConCallExpression) myExpression).getDefinition().getParameters())
      : myExpression instanceof SigmaExpression
        ? DependentLink.Helper.size(((SigmaExpression) myExpression).getParameters())
        : myExpression instanceof ClassCallExpression
          ? ((ClassCallExpression) myExpression).getDefinition().getNumberOfNotImplementedFields()
          : 0;
  }

  public Expression toExpression(List<Expression> arguments) {
    if (myExpression instanceof SigmaExpression) {
      return new TupleExpression(arguments, (SigmaExpression) myExpression);
    }

    if (myExpression instanceof ConCallExpression) {
      ConCallExpression conCall = (ConCallExpression) myExpression;
      return ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments);
    }

    if (myExpression instanceof FunCallExpression) {
      return myExpression;
    }

    ClassCallExpression classCall = (ClassCallExpression) myExpression;
    Map<ClassField, Expression> implementations = new HashMap<>();
    ClassCallExpression resultClassCall = new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), implementations, Sort.PROP, false);
    resultClassCall.copyImplementationsFrom(classCall);
    int i = 0;
    for (ClassField field : classCall.getDefinition().getFields()) {
      if (!classCall.isImplemented(field)) {
        implementations.put(field, arguments.get(i++));
      }
    }
    return new NewExpression(null, resultClassCall);
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
  public Expression toPatternExpression() {
    List<Expression> arguments = new ArrayList<>(myPatterns.getPatternList().size());
    for (Pattern pattern : myPatterns.getPatternList()) {
      Expression argument = pattern.toPatternExpression();
      if (argument == null) {
        return null;
      }
      arguments.add(argument);
    }
    return (myExpression instanceof ClassCallExpression ? new ConstructorPattern(new SigmaExpression(Sort.PROP, getParameters()), myPatterns) : this).toExpression(arguments);
  }

  @Override
  public DependentLink getFirstBinding() {
    return myPatterns.getFirstBinding();
  }

  @Override
  public DependentLink getLastBinding() {
    return myPatterns.getLastBinding();
  }

  public List<? extends Expression> getMatchingExpressionArguments(Expression expression, boolean normalize) {
    if (myExpression instanceof SigmaExpression) {
      TupleExpression tuple = expression.cast(TupleExpression.class);
      return tuple == null ? null : tuple.getFields();
    }

    if (myExpression instanceof FunCallExpression) {
      expression = expression.getUnderlyingExpression();
      if (expression instanceof FunCallExpression && ((FunCallExpression) expression).getDefinition() == Prelude.IDP) {
        return Collections.emptyList();
      }
      if (!(expression instanceof ConCallExpression && ((ConCallExpression) expression).getDefinition() == Prelude.PATH_CON)) {
        return null;
      }
      Expression arg = ((ConCallExpression) expression).getDefCallArguments().get(0);
      if (normalize) {
        arg = arg.normalize(NormalizationMode.WHNF);
      }
      LamExpression lamExpr = arg.cast(LamExpression.class);
      if (lamExpr == null) {
        return null;
      }
      Expression body = lamExpr.getParameters().getNext().hasNext() ? new LamExpression(lamExpr.getResultSort(), lamExpr.getParameters().getNext(), lamExpr.getBody()) : lamExpr.getBody();
      return NormalizingFindBindingVisitor.findBinding(body, lamExpr.getParameters()) ? null : Collections.emptyList();
    }

    if (myExpression instanceof ConCallExpression) {
      ConCallExpression conCall = expression.cast(ConCallExpression.class);
      Constructor myConstructor = ((ConCallExpression) myExpression).getDefinition();
      if (conCall == null && (myConstructor == Prelude.ZERO || myConstructor == Prelude.SUC)) {
        IntegerExpression intExpr = expression.cast(IntegerExpression.class);
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

    NewExpression newExpr = expression.cast(NewExpression.class);
    if (newExpr == null) {
      return null;
    }
    List<Expression> arguments = new ArrayList<>();
    for (ClassField field : ((ClassCallExpression) myExpression).getDefinition().getFields()) {
      if (!((ClassCallExpression) myExpression).getDefinition().isImplemented(field)) {
        arguments.add(newExpr.getImplementation(field));
      }
    }
    return arguments;
  }

  @Override
  public MatchResult match(Expression expression, List<Expression> result) {
    expression = expression.normalize(NormalizationMode.WHNF); // TODO[idp]: Implement IDP_WHNF
    List<? extends Expression> arguments = getMatchingExpressionArguments(expression, true);
    if (arguments != null) {
      return myPatterns.match(arguments, result);
    }

    if (myExpression instanceof ConCallExpression) {
      ConCallExpression conCall = expression.cast(ConCallExpression.class);
      Constructor myConstructor = ((ConCallExpression) myExpression).getDefinition();
      if (conCall != null && conCall.getDefinition() != myConstructor) {
        return MatchResult.FAIL;
      }
      if (conCall == null && (myConstructor == Prelude.ZERO || myConstructor == Prelude.SUC)) {
        IntegerExpression intExpr = expression.cast(IntegerExpression.class);
        if (intExpr != null && (myConstructor == Prelude.ZERO) != intExpr.isZero()) {
          return MatchResult.FAIL;
        }
      }
    }
    return MatchResult.MAYBE;
  }

  @Override
  public boolean unify(ExprSubstitution idpSubst, Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    if (other instanceof BindingPattern) {
      if (substitution2 != null) {
        substitution2.add(((BindingPattern) other).getBinding(), toExpression());
      }
      return true;
    }

    if (other instanceof ConstructorPattern) {
      ConstructorPattern conPattern = (ConstructorPattern) other;
      return (myExpression instanceof SigmaExpression && conPattern.myExpression instanceof SigmaExpression ||
              myExpression instanceof DefCallExpression && conPattern.myExpression instanceof DefCallExpression &&
                ((DefCallExpression) myExpression).getDefinition() == ((DefCallExpression) conPattern.myExpression).getDefinition())
        && myPatterns.unify(idpSubst, conPattern.myPatterns, substitution1, substitution2, errorReporter, sourceNode);
    }

    return false;
  }

  @Override
  public Pattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, Pattern> patternSubst) {
    List<Pattern> patterns = new ArrayList<>(myPatterns.getPatternList().size());
    for (Pattern pattern : myPatterns.getPatternList()) {
      patterns.add(pattern.subst(exprSubst, levelSubst, patternSubst));
    }
    return new ConstructorPattern(myExpression.subst(exprSubst, levelSubst), new Patterns(patterns));
  }
}
