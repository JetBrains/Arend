package org.arend.typechecking.result;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.Levels;
import org.arend.ext.core.level.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.TypecheckingError;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.PathEndpointMismatchError;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefCallResult implements TResult {
  private final Concrete.ReferenceExpression myDefCall;
  private final Definition myDefinition;
  private final Levels myLevels;
  private final List<Expression> myArguments;
  private List<DependentLink> myParameters;
  private Expression myResultType;

  private DefCallResult(Concrete.ReferenceExpression defCall, Definition definition, Levels levels, List<Expression> arguments, List<DependentLink> parameters, Expression resultType) {
    myDefCall = defCall;
    myDefinition = definition;
    myLevels = levels;
    myArguments = arguments;
    myParameters = parameters;
    myResultType = resultType;
  }

  public static TResult makeTResult(Concrete.ReferenceExpression defCall, Definition definition, Levels levels) {
    List<DependentLink> parameters = new ArrayList<>();
    Expression resultType = definition.getTypeWithParams(parameters, levels);

    if (parameters.isEmpty()) {
      return new TypecheckingResult(definition.getDefCall(levels, Collections.emptyList()), resultType);
    } else {
      return new DefCallResult(defCall, definition, levels, new ArrayList<>(), parameters, resultType);
    }
  }

  public static TResult makePathType(Concrete.ReferenceExpression defCall, boolean isInfix, Levels levels, Sort resultSort) {
    Definition definition = isInfix ? Prelude.PATH_INFIX : Prelude.PATH;
    List<DependentLink> parameters = new ArrayList<>();
    definition.getTypeWithParams(parameters, levels);
    return new DefCallResult(defCall, definition, levels, new ArrayList<>(), parameters, new UniverseExpression(resultSort));
  }

  private Expression getCoreDefCall() {
    return myDefinition == Prelude.PATH_CON
      ? new PathExpression(myLevels.toLevelPair(), myArguments.get(0).removeConstLam() == null ? myArguments.get(0) : null, myArguments.get(1))
      : myDefinition == Prelude.AT
        ? AtExpression.make(myLevels.toLevelPair(), myArguments.get(3), myArguments.get(4), true)
        : myDefinition.getDefCall(myLevels, myArguments);
  }

  public boolean checkField(CheckTypeVisitor typechecker, Expression expr, Expression type) {
    if (!(type instanceof ClassCallExpression)) {
      // typechecker.getErrorReporter().report(new TypeComputationError(null, expr, myDefCall));
      return true;
    }
    if (!myLevels.compare(((ClassCallExpression) type).getLevels(((ClassField) myDefinition).getParentClass()), CMP.LE, typechecker.getEquations(), myDefCall)) {
      typechecker.getErrorReporter().report(new TypecheckingError("Cannot compare the type of the argument", myDefCall));
      return false;
    } else {
      return true;
    }
  }

  private boolean checkField(CheckTypeVisitor typechecker) {
    if (!(myDefinition instanceof ClassField)) return true;
    Expression type = myArguments.get(0).getType();
    if (type != null) {
      type = type.normalize(NormalizationMode.WHNF);
    }
    return checkField(typechecker, myArguments.get(0), type);
  }

  @Override
  public TypecheckingResult toResult(CheckTypeVisitor typechecker) {
    if (myParameters.isEmpty()) {
      checkField(typechecker);
      return new TypecheckingResult(getCoreDefCall(), myResultType);
    }

    List<SingleDependentLink> parameters = new ArrayList<>();
    ExprSubstitution substitution = new ExprSubstitution();
    List<String> names = new ArrayList<>();
    DependentLink link0 = null;
    for (DependentLink link : myParameters) {
      if (link0 == null) {
        link0 = link;
      }

      names.add(link.getName());
      if (link instanceof TypedDependentLink) {
        SingleDependentLink parameter = ExpressionFactory.singleParams(link.isExplicit(), names, link.getType().subst(new SubstVisitor(substitution, LevelSubstitution.EMPTY)));
        parameters.add(parameter);
        names.clear();

        for (; parameter.hasNext(); parameter = parameter.getNext(), link0 = link0.getNext()) {
          substitution.add(link0, new ReferenceExpression(parameter));
          myArguments.add(new ReferenceExpression(parameter));
        }

        link0 = null;
      }
    }

    Expression expression = getCoreDefCall();
    Expression type = myResultType.subst(substitution, LevelSubstitution.EMPTY);
    Sort codSort = typechecker.getSortOfType(type, myDefCall);
    for (int i = parameters.size() - 1; i >= 0; i--) {
      codSort = PiExpression.generateUpperBound(parameters.get(i).getType().getSortOfType(), codSort, typechecker.getEquations(), myDefCall);
      expression = new LamExpression(codSort, parameters.get(i), expression);
      type = new PiExpression(codSort, parameters.get(i), type);
    }
    checkField(typechecker);
    return new TypecheckingResult(expression, type);
  }

  @Override
  public DependentLink getParameter() {
    return myParameters.get(0);
  }

  @Override
  public TResult applyExpression(Expression expression, boolean isExplicit, CheckTypeVisitor typechecker, Concrete.SourceNode sourceNode) {
    int size = myParameters.size();
    myArguments.add(expression);
    ExprSubstitution subst = new ExprSubstitution();
    subst.add(myParameters.get(0), expression);
    myParameters = DependentLink.Helper.subst(myParameters.subList(1, size), subst, LevelSubstitution.EMPTY);
    myResultType = myResultType.subst(subst, LevelSubstitution.EMPTY);
    if (size > 1) return this;
    checkField(typechecker);
    return new TypecheckingResult(getCoreDefCall(), myResultType);
  }

  public TResult applyExpressions(List<? extends Expression> expressions, CheckTypeVisitor typechecker) {
    int size = myParameters.size();
    List<? extends Expression> args = expressions.size() <= size ? expressions : expressions.subList(0, size);
    myArguments.addAll(args);
    ExprSubstitution subst = new ExprSubstitution();
    for (int i = 0; i < args.size(); i++) {
      subst.add(myParameters.get(i), args.get(i));
    }
    myParameters = DependentLink.Helper.subst(myParameters.subList(args.size(), size), subst, LevelSubstitution.EMPTY);
    myResultType = myResultType.subst(subst, LevelSubstitution.EMPTY);

    assert expressions.size() <= size;
    if (expressions.size() < size) return this;
    checkField(typechecker);
    return new TypecheckingResult(getCoreDefCall(), myResultType);
  }

  public TResult applyPathArgument(Expression argument, CheckTypeVisitor visitor, Concrete.SourceNode sourceNode) {
    assert myDefinition == Prelude.PATH_CON && !myArguments.isEmpty();
    Expression leftExpr = AppExpression.make(argument, ExpressionFactory.Left(), true);
    Expression rightExpr = AppExpression.make(argument, ExpressionFactory.Right(), true);
    ExprSubstitution subst = new ExprSubstitution();
    if (myArguments.size() >= 2) {
      if (!CompareVisitor.compare(visitor.getEquations(), CMP.EQ, leftExpr, myArguments.get(1), AppExpression.make(myArguments.get(0), ExpressionFactory.Left(), true), sourceNode)) {
        visitor.getErrorReporter().report(new PathEndpointMismatchError(true, myArguments.get(1), leftExpr, sourceNode));
      }
    } else {
      subst.add(myParameters.get(0), leftExpr);
    }
    if (myArguments.size() >= 3) {
      if (!CompareVisitor.compare(visitor.getEquations(), CMP.EQ, rightExpr, myArguments.get(2), AppExpression.make(myArguments.get(0), ExpressionFactory.Right(), true), sourceNode)) {
        visitor.getErrorReporter().report(new PathEndpointMismatchError(false, myArguments.get(2), rightExpr, sourceNode));
      }
    } else {
      subst.add(myParameters.get(myParameters.size() - 2), rightExpr);
    }
    if (myArguments.size() > 1) {
      myArguments.subList(1, myArguments.size()).clear();
    }
    myArguments.add(argument);

    myParameters = Collections.emptyList();
    myResultType = myResultType.subst(subst, LevelSubstitution.EMPTY);
    return new TypecheckingResult(getCoreDefCall(), myResultType);
  }

  @Override
  public List<DependentLink> getImplicitParameters() {
    List<DependentLink> params = new ArrayList<>(myParameters.size());
    for (DependentLink param : myParameters) {
      if (param.isExplicit()) {
        return params;
      }
      params.add(param);
    }
    myResultType.getPiParameters(params, true);
    return params;
  }

  @Override
  public Expression getType() {
    return myResultType;
  }

  public Concrete.ReferenceExpression getDefCall() {
    return myDefCall;
  }

  public Definition getDefinition() {
    return myDefinition;
  }

  public List<? extends Expression> getArguments() {
    return myArguments;
  }

  public Levels getLevels() {
    return myLevels;
  }
}
