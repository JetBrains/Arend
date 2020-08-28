package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.NormalizingFindBindingVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.ErrorReporter;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ConstructorExpressionPattern extends ConstructorPattern<Expression> implements ExpressionPattern {
  private ConstructorExpressionPattern(Expression expression, List<? extends ExpressionPattern> patterns) {
    super(expression, patterns);
  }

  public ConstructorExpressionPattern(ConCallExpression conCall, List<? extends ExpressionPattern> patterns) {
    super(conCall, patterns);
  }

  public ConstructorExpressionPattern(ClassCallExpression classCall, List<? extends ExpressionPattern> patterns) {
    super(classCall, patterns);
  }

  public ConstructorExpressionPattern(SigmaExpression sigma, List<? extends ExpressionPattern> patterns) {
    super(sigma, patterns);
  }

  public ConstructorExpressionPattern(FunCallExpression funCall, List<? extends ExpressionPattern> patterns) {
    super(funCall, patterns);
  }

  public ConstructorExpressionPattern(ConstructorExpressionPattern pattern, List<? extends ExpressionPattern> patterns) {
    super(pattern.data, patterns);
  }

  public Expression getDataExpression() {
    return data;
  }

  @Override
  public ConstructorExpressionPattern toExpressionPattern(Expression type) {
    return this;
  }

  @NotNull
  @Override
  public List<? extends ExpressionPattern> getSubPatterns() {
    //noinspection unchecked
    return (List<? extends ExpressionPattern>) super.getSubPatterns();
  }

  @Override
  public Concrete.Pattern toConcrete(Object data, boolean isExplicit, Map<DependentLink, Concrete.Pattern> subPatterns) {
    Definition definition = getConstructor();
    DependentLink param = definition != null ? definition.getParameters() : EmptyDependentLink.getInstance();

    List<Concrete.Pattern> patterns = new ArrayList<>();
    for (ExpressionPattern subPattern : getSubPatterns()) {
      patterns.add(subPattern.toConcrete(data, !param.hasNext() || param.isExplicit(), subPatterns));
      if (param.hasNext()) {
        param = param.getNext();
      }
    }

    if (definition != null) {
      return new Concrete.ConstructorPattern(data, isExplicit, definition.getRef(), patterns, Collections.emptyList());
    } else {
      return new Concrete.TuplePattern(data, isExplicit, patterns, Collections.emptyList());
    }
  }

  @Override
  public DependentLink replaceBindings(DependentLink link, List<Pattern> result) {
    List<ExpressionPattern> subPatterns = new ArrayList<>();
    result.add(new ConstructorExpressionPattern(data, subPatterns));
    for (ExpressionPattern pattern : getSubPatterns()) {
      //noinspection unchecked
      link = pattern.replaceBindings(link, (List<Pattern>) (List<?>) subPatterns);
    }
    return link;
  }

  @Override
  public Definition getDefinition() {
    return data instanceof DefCallExpression ? ((DefCallExpression) data).getDefinition() : null;
  }

  public List<? extends Expression> getDataTypeArguments() {
    return data instanceof ConCallExpression
      ? ((ConCallExpression) data).getDataTypeArguments()
      : data instanceof FunCallExpression
        ? ((FunCallExpression) data).getDefCallArguments()
        : null;
  }

  public Sort getSortArgument() {
    return data instanceof DefCallExpression ? ((DefCallExpression) data).getSortArgument() : null;
  }

  public DependentLink getParameters() {
    return data instanceof ClassCallExpression
      ? ((ClassCallExpression) data).getClassFieldParameters()
      : data instanceof FunCallExpression
        ? EmptyDependentLink.getInstance()
        : data instanceof DefCallExpression
          ? ((DefCallExpression) data).getDefinition().getParameters()
          : ((SigmaExpression) data).getParameters();
  }

  public int getLength() {
    return data instanceof ConCallExpression
      ? DependentLink.Helper.size(((ConCallExpression) data).getDefinition().getParameters())
      : data instanceof SigmaExpression
        ? DependentLink.Helper.size(((SigmaExpression) data).getParameters())
        : data instanceof ClassCallExpression
          ? ((ClassCallExpression) data).getDefinition().getNumberOfNotImplementedFields()
          : 0;
  }

  public Expression toExpression(List<Expression> arguments) {
    if (data instanceof SigmaExpression) {
      return new TupleExpression(arguments, (SigmaExpression) data);
    }

    if (data instanceof ConCallExpression) {
      ConCallExpression conCall = (ConCallExpression) data;
      return ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments);
    }

    if (data instanceof FunCallExpression) {
      return data;
    }

    ClassCallExpression classCall = (ClassCallExpression) data;
    Map<ClassField, Expression> implementations = new HashMap<>();
    ClassCallExpression resultClassCall = new ClassCallExpression(classCall.getDefinition(), classCall.getSortArgument(), implementations, Sort.PROP, UniverseKind.NO_UNIVERSES);
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
    List<Expression> arguments = new ArrayList<>();
    for (ExpressionPattern pattern : getSubPatterns()) {
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
    List<Expression> arguments = new ArrayList<>();
    for (ExpressionPattern pattern : getSubPatterns()) {
      Expression argument = pattern.toPatternExpression();
      if (argument == null) {
        return null;
      }
      arguments.add(argument);
    }
    return (data instanceof ClassCallExpression ? new ConstructorExpressionPattern(new SigmaExpression(Sort.PROP, getParameters()), getSubPatterns()) : this).toExpression(arguments);
  }

  @Override
  public DependentLink getFirstBinding() {
    return Pattern.getFirstBinding(getSubPatterns());
  }

  @Override
  public DependentLink getLastBinding() {
    return Pattern.getLastBinding(getSubPatterns());
  }

  public List<? extends Expression> getMatchingExpressionArguments(Expression expression, boolean normalize) {
    if (data instanceof SigmaExpression) {
      TupleExpression tuple = expression.cast(TupleExpression.class);
      return tuple == null ? null : tuple.getFields();
    }

    if (data instanceof FunCallExpression) {
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

    if (data instanceof ConCallExpression) {
      ConCallExpression conCall = expression.cast(ConCallExpression.class);
      Constructor myConstructor = ((ConCallExpression) data).getDefinition();
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
    for (ClassField field : ((ClassCallExpression) data).getDefinition().getFields()) {
      if (!((ClassCallExpression) data).isImplemented(field)) {
        arguments.add(newExpr.getImplementation(field));
      }
    }
    return arguments;
  }

  @Override
  public Decision match(Expression expression, List<Expression> result) {
    expression = expression.normalize(NormalizationMode.WHNF);
    List<? extends Expression> arguments = getMatchingExpressionArguments(expression, true);
    if (arguments != null) {
      return ExpressionPattern.match(getSubPatterns(), arguments, result);
    }

    if (data instanceof ConCallExpression) {
      ConCallExpression conCall = expression.cast(ConCallExpression.class);
      Constructor myConstructor = ((ConCallExpression) data).getDefinition();
      if (conCall != null && conCall.getDefinition() != myConstructor) {
        return Decision.NO;
      }
      if (conCall == null && (myConstructor == Prelude.ZERO || myConstructor == Prelude.SUC)) {
        IntegerExpression intExpr = expression.cast(IntegerExpression.class);
        if (intExpr != null && (myConstructor == Prelude.ZERO) != intExpr.isZero()) {
          return Decision.NO;
        }
      }
    }
    return Decision.MAYBE;
  }

  @Override
  public boolean unify(ExprSubstitution idpSubst, ExpressionPattern other, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    if (other instanceof BindingPattern) {
      if (substitution2 != null) {
        substitution2.add(((BindingPattern) other).getBinding(), toExpression());
      }
      return true;
    }

    if (other instanceof ConstructorExpressionPattern) {
      ConstructorExpressionPattern conPattern = (ConstructorExpressionPattern) other;
      return (data instanceof SigmaExpression && conPattern.data instanceof SigmaExpression ||
              data instanceof DefCallExpression && conPattern.data instanceof DefCallExpression &&
                ((DefCallExpression) data).getDefinition() == ((DefCallExpression) conPattern.data).getDefinition())
        && ExpressionPattern.unify(getSubPatterns(), conPattern.getSubPatterns(), idpSubst, substitution1, substitution2, errorReporter, sourceNode);
    }

    return false;
  }

  @Override
  public ExpressionPattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, ExpressionPattern> patternSubst) {
    List<ExpressionPattern> patterns = new ArrayList<>();
    for (ExpressionPattern pattern : getSubPatterns()) {
      patterns.add(pattern.subst(exprSubst, levelSubst, patternSubst));
    }
    Expression expr = data.subst(exprSubst, levelSubst);
    return new ConstructorExpressionPattern(expr instanceof SmallIntegerExpression && ((SmallIntegerExpression) expr).getInteger() == 0 ? new ConCallExpression(Prelude.ZERO, Sort.PROP, Collections.emptyList(), Collections.emptyList()) : expr, patterns);
  }

  @Override
  public Pattern removeExpressions() {
    return ConstructorPattern.make(getConstructor(), ExpressionPattern.removeExpressions(getSubPatterns()));
  }
}
