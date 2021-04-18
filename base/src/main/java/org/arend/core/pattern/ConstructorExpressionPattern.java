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
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConstructorExpressionPattern extends ConstructorPattern<Object> implements ExpressionPattern {
  private ConstructorExpressionPattern(Object expression, List<? extends ExpressionPattern> patterns) {
    super(expression, patterns);
  }

  public ConstructorExpressionPattern(ConCallExpression conCall, List<? extends ExpressionPattern> patterns) {
    super(conCall.getDefinition() == Prelude.ZERO ? new SmallIntegerExpression(0) : conCall, patterns);
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

  public ConstructorExpressionPattern(FunCallExpression funCall, Boolean isArrayEmpty, List<? extends ExpressionPattern> patterns) {
    super(isArrayEmpty == null ? funCall : new ArrayPair(funCall, isArrayEmpty), patterns);
  }

  public ConstructorExpressionPattern(FunCallExpression funCall, Expression arrayLength, List<? extends ExpressionPattern> patterns) {
    this(funCall, isEqualToZero(arrayLength), patterns);
  }

  private static class ArrayPair {
    final FunCallExpression funCall;
    final Boolean isEmpty;

    private ArrayPair(FunCallExpression funCall, Boolean isEmpty) {
      this.funCall = funCall;
      this.isEmpty = isEmpty;
    }
  }

  public static Boolean isEqualToZero(Expression length) {
    if (length == null) return null;
    length = length.normalize(NormalizationMode.WHNF);
    return length instanceof IntegerExpression ? (Boolean) ((IntegerExpression) length).isZero() : length instanceof ConCallExpression && ((ConCallExpression) length).getDefinition() == Prelude.SUC ? false : null;
  }

  public static Boolean isArrayEmpty(Expression type) {
    type = type.normalize(NormalizationMode.WHNF);
    return type instanceof ClassCallExpression && ((ClassCallExpression) type).getDefinition() == Prelude.ARRAY ? isEqualToZero(((ClassCallExpression) type).getAbsImplementationHere(Prelude.ARRAY_LENGTH)) : null;
  }

  public Expression getDataExpression() {
    return data instanceof ArrayPair ? ((ArrayPair) data).funCall : (Expression) data;
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
    return data instanceof DefCallExpression ? ((DefCallExpression) data).getDefinition() : data instanceof SmallIntegerExpression ? Prelude.ZERO : data instanceof ArrayPair ? ((ArrayPair) data).funCall.getDefinition() : null;
  }

  public List<? extends Expression> getDataTypeArguments() {
    return data instanceof ConCallExpression ? ((ConCallExpression) data).getDataTypeArguments() : Collections.emptyList();
  }

  public Expression getArrayElementsType() {
    Expression dataExpr = getDataExpression();
    return dataExpr instanceof FunCallExpression && ((FunCallExpression) dataExpr).getDefCallArguments().size() == 1 ? ((FunCallExpression) dataExpr).getDefCallArguments().get(0) : null;
  }

  public Boolean isArrayEmpty() {
    return data instanceof ArrayPair ? ((ArrayPair) data).isEmpty : null;
  }

  public Sort getSortArgument() {
    Expression dataExpr = getDataExpression();
    return dataExpr instanceof DefCallExpression ? ((DefCallExpression) dataExpr).getSortArgument() : dataExpr instanceof SmallIntegerExpression ? Sort.PROP : null;
  }

  public DependentLink getParameters() {
    Expression dataExpr = getDataExpression();
    return dataExpr instanceof ClassCallExpression
      ? ((ClassCallExpression) dataExpr).getClassFieldParameters()
      : dataExpr instanceof FunCallExpression && ((FunCallExpression) dataExpr).getDefinition() == Prelude.IDP || dataExpr instanceof SmallIntegerExpression
        ? EmptyDependentLink.getInstance()
        : dataExpr instanceof DefCallExpression
          ? ((DefCallExpression) dataExpr).getDefinition().getParameters()
          : ((SigmaExpression) dataExpr).getParameters();
  }

  public int getLength() {
    Expression dataExpr = getDataExpression();
    return dataExpr instanceof ClassCallExpression
      ? ((ClassCallExpression) dataExpr).getDefinition().getNumberOfNotImplementedFields()
      : dataExpr instanceof DefCallExpression && ((DefCallExpression) dataExpr).getDefinition() != Prelude.IDP
        ? DependentLink.Helper.size(((DefCallExpression) dataExpr).getDefinition().getParameters())
        : dataExpr instanceof SigmaExpression
          ? DependentLink.Helper.size(((SigmaExpression) dataExpr).getParameters())
          : 0;
  }

  public Expression toExpression(List<Expression> arguments) {
    Expression dataExpr = getDataExpression();
    if (dataExpr instanceof SigmaExpression) {
      return new TupleExpression(arguments, (SigmaExpression) dataExpr);
    }

    if (dataExpr instanceof ConCallExpression) {
      ConCallExpression conCall = (ConCallExpression) dataExpr;
      return ConCallExpression.make(conCall.getDefinition(), conCall.getSortArgument(), conCall.getDataTypeArguments(), arguments);
    }

    if (dataExpr instanceof FunCallExpression && ((FunCallExpression) dataExpr).getDefinition() == Prelude.IDP || dataExpr instanceof SmallIntegerExpression) {
      return dataExpr;
    }

    if (dataExpr instanceof FunCallExpression) {
      FunCallExpression funCall = (FunCallExpression) dataExpr;
      List<Expression> newArgs;
      if (!funCall.getDefCallArguments().isEmpty()) {
        newArgs = new ArrayList<>(funCall.getDefCallArguments().size() + arguments.size());
        newArgs.addAll(funCall.getDefCallArguments());
        newArgs.addAll(arguments);
      } else {
        newArgs = arguments;
      }
      return FunCallExpression.make(funCall.getDefinition(), funCall.getSortArgument(), newArgs);
    }

    ClassCallExpression classCall = (ClassCallExpression) dataExpr;
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
    Expression dataExpr = getDataExpression();
    if (dataExpr instanceof FunCallExpression && ((FunCallExpression) dataExpr).getDefinition() == Prelude.IDP || dataExpr instanceof SmallIntegerExpression) {
      return dataExpr;
    }

    List<Expression> arguments = new ArrayList<>();
    for (ExpressionPattern pattern : getSubPatterns()) {
      Expression argument = pattern.toPatternExpression();
      if (argument == null) {
        return null;
      }
      arguments.add(argument);
    }
    return (dataExpr instanceof ClassCallExpression ? new ConstructorExpressionPattern(new SigmaExpression(Sort.PROP, getParameters()), getSubPatterns()) : this).toExpression(arguments);
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
    Expression dataExpr = getDataExpression();
    if (dataExpr instanceof SigmaExpression) {
      TupleExpression tuple = expression.cast(TupleExpression.class);
      return tuple == null ? null : tuple.getFields();
    }

    if (dataExpr instanceof FunCallExpression) {
      FunctionDefinition function = ((FunCallExpression) dataExpr).getDefinition();
      expression = expression.getUnderlyingExpression();
      if (function == Prelude.EMPTY_ARRAY || function == Prelude.ARRAY_CONS) {
        if (!(expression instanceof ArrayExpression)) {
          return null;
        }
        ArrayExpression array = (ArrayExpression) expression;
        return array.getElements().isEmpty() == (function == Prelude.EMPTY_ARRAY) ? array.getConstructorArguments(((FunCallExpression) dataExpr).getDefCallArguments().isEmpty()) : null;
      }
      if (function != Prelude.IDP) {
        return null;
      }
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

    if (dataExpr instanceof ConCallExpression || dataExpr instanceof SmallIntegerExpression) {
      ConCallExpression conCall = expression.cast(ConCallExpression.class);
      Definition myConstructor = getDefinition();
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
    for (ClassField field : ((ClassCallExpression) dataExpr).getDefinition().getFields()) {
      if (!((ClassCallExpression) dataExpr).isImplemented(field)) {
        arguments.add(newExpr.getImplementation(field));
      }
    }
    return arguments;
  }

  @Override
  public Decision match(Expression expression, List<Expression> result) {
    Expression dataExpr = getDataExpression();
    expression = expression.normalize(NormalizationMode.WHNF);
    List<? extends Expression> arguments = getMatchingExpressionArguments(expression, true);
    if (arguments != null) {
      return ExpressionPattern.match(getSubPatterns(), arguments, result);
    }

    if (dataExpr instanceof ConCallExpression || dataExpr instanceof SmallIntegerExpression) {
      ConCallExpression conCall = expression.cast(ConCallExpression.class);
      Definition myConstructor = getDefinition();
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
    if (dataExpr instanceof FunCallExpression && ((FunCallExpression) dataExpr).getDefinition() != Prelude.IDP && expression instanceof ArrayExpression && ((ArrayExpression) expression).getElements().isEmpty() != (((FunCallExpression) dataExpr).getDefinition() == Prelude.EMPTY_ARRAY)) {
      return Decision.NO;
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
      Expression dataExpr = getDataExpression();
      Expression otherExpr = conPattern.getDataExpression();
      return (dataExpr instanceof SigmaExpression && otherExpr instanceof SigmaExpression ||
              dataExpr instanceof SmallIntegerExpression && otherExpr instanceof SmallIntegerExpression ||
              dataExpr instanceof DefCallExpression && otherExpr instanceof DefCallExpression &&
                ((DefCallExpression) dataExpr).getDefinition() == ((DefCallExpression) otherExpr).getDefinition())
        && ExpressionPattern.unify(getSubPatterns(), conPattern.getSubPatterns(), idpSubst, substitution1, substitution2, errorReporter, sourceNode);
    }

    return false;
  }

  @Override
  public @Nullable ExpressionPattern intersect(ExpressionPattern other) {
    if (other instanceof BindingPattern) {
      return this;
    }
    if (!(other instanceof ConstructorExpressionPattern)) {
      return null;
    }

    ConstructorExpressionPattern conPattern = (ConstructorExpressionPattern) other;
    Expression dataExpr = getDataExpression();
    Expression otherExpr = conPattern.getDataExpression();
    if (dataExpr instanceof SigmaExpression && otherExpr instanceof SigmaExpression ||
        dataExpr instanceof SmallIntegerExpression && otherExpr instanceof SmallIntegerExpression ||
        dataExpr instanceof DefCallExpression && otherExpr instanceof DefCallExpression &&
          ((DefCallExpression) dataExpr).getDefinition() == ((DefCallExpression) otherExpr).getDefinition()) {

      List<ExpressionPattern> resultSubPatterns = new ArrayList<>();
      List<? extends ExpressionPattern> subPatterns1 = getSubPatterns();
      List<? extends ExpressionPattern> subPatterns2 = conPattern.getSubPatterns();
      for (int i = 0; i < subPatterns1.size(); i++) {
        ExpressionPattern pattern = subPatterns1.get(i).intersect(subPatterns2.get(i));
        if (pattern == null) {
          return null;
        }
        resultSubPatterns.add(pattern);
      }
      return new ConstructorExpressionPattern(dataExpr, resultSubPatterns);
    } else {
      return null;
    }
  }

  @Override
  public ExpressionPattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, ExpressionPattern> patternSubst) {
    List<ExpressionPattern> patterns = new ArrayList<>();
    for (ExpressionPattern pattern : getSubPatterns()) {
      patterns.add(pattern.subst(exprSubst, levelSubst, patternSubst));
    }
    return new ConstructorExpressionPattern(getDataExpression().subst(exprSubst, levelSubst), patterns);
  }

  @Override
  public Pattern removeExpressions() {
    return ConstructorPattern.make(getConstructor(), ExpressionPattern.removeExpressions(getSubPatterns()));
  }
}
