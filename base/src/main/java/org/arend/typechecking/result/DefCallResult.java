package org.arend.typechecking.result;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.LevelSubstitution;
import org.arend.core.subst.SubstVisitor;
import org.arend.ext.error.ErrorReporter;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefCallResult implements TResult {
  private final Concrete.ReferenceExpression myDefCall;
  private final Definition myDefinition;
  private final LevelPair myLevels;
  private final List<Expression> myArguments;
  private List<DependentLink> myParameters;
  private Expression myResultType;

  private DefCallResult(Concrete.ReferenceExpression defCall, Definition definition, LevelPair levels, List<Expression> arguments, List<DependentLink> parameters, Expression resultType) {
    myDefCall = defCall;
    myDefinition = definition;
    myLevels = levels;
    myArguments = arguments;
    myParameters = parameters;
    myResultType = resultType;
  }

  public static TResult makeTResult(Concrete.ReferenceExpression defCall, Definition definition, LevelPair levels) {
    List<DependentLink> parameters = new ArrayList<>();
    Expression resultType = definition.getTypeWithParams(parameters, levels);

    if (parameters.isEmpty()) {
      return new TypecheckingResult(definition.getDefCall(levels, Collections.emptyList()), resultType);
    } else {
      return new DefCallResult(defCall, definition, levels, new ArrayList<>(), parameters, resultType);
    }
  }

  public static TResult makePathType(Concrete.ReferenceExpression defCall, boolean isInfix, LevelPair levels, Sort resultSort) {
    Definition definition = isInfix ? Prelude.PATH_INFIX : Prelude.PATH;
    List<DependentLink> parameters = new ArrayList<>();
    definition.getTypeWithParams(parameters, levels);
    return new DefCallResult(defCall, definition, levels, new ArrayList<>(), parameters, new UniverseExpression(resultSort));
  }

  @Override
  public TypecheckingResult toResult(CheckTypeVisitor typechecker) {
    if (myParameters.isEmpty()) {
      return new TypecheckingResult(myDefinition.getDefCall(myLevels, myArguments), myResultType);
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

    Expression expression = myDefinition.getDefCall(myLevels, myArguments);
    Expression type = myResultType.subst(substitution, LevelSubstitution.EMPTY);
    Sort codSort = typechecker.getSortOfType(type, myDefCall);
    for (int i = parameters.size() - 1; i >= 0; i--) {
      codSort = PiExpression.generateUpperBound(parameters.get(i).getType().getSortOfType(), codSort, typechecker.getEquations(), myDefCall);
      expression = new LamExpression(codSort, parameters.get(i), expression);
      type = new PiExpression(codSort, parameters.get(i), type);
    }
    return new TypecheckingResult(expression, type);
  }

  @Override
  public DependentLink getParameter() {
    return myParameters.get(0);
  }

  @Override
  public TResult applyExpression(Expression expression, boolean isExplicit, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    int size = myParameters.size();
    myArguments.add(expression);
    ExprSubstitution subst = new ExprSubstitution();
    subst.add(myParameters.get(0), expression);
    myParameters = DependentLink.Helper.subst(myParameters.subList(1, size), subst, LevelSubstitution.EMPTY);
    myResultType = myResultType.subst(subst, LevelSubstitution.EMPTY);
    return size > 1 ? this : new TypecheckingResult(myDefinition.getDefCall(myLevels, myArguments), myResultType);
  }

  public TResult applyExpressions(List<? extends Expression> expressions) {
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
    return expressions.size() < size ? this : new TypecheckingResult(myDefinition.getDefCall(myLevels, myArguments), myResultType);
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

  public LevelPair getLevels() {
    return myLevels;
  }
}
