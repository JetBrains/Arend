package org.arend.term.concrete;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see org.arend.term.concrete.BaseConcreteExpressionVisitor
 */
public class SubstConcreteExpressionVisitor<P> implements ConcreteExpressionVisitor<P, Concrete.Expression> {
  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, P params) {
    // It is important that we process arguments first since setFunction modifies the list of arguments.
    var args = expr.getArguments().stream()
      .map(argument -> new Concrete.Argument(argument.expression.accept(this, params), argument.isExplicit()))
      .collect(Collectors.toList());
    return Concrete.AppExpression.make(
      expr.getData(),
      expr.getFunction().accept(this, params),
      args
    );
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, P params) {
    return new Concrete.ReferenceExpression(expr.getData(), expr.getReferent(), expr.getPLevel(), expr.getHLevel());
  }

  @Override
  public Concrete.Expression visitThis(Concrete.ThisExpression expr, P params) {
    return new Concrete.ThisExpression(expr.getData(), expr.getReferent());
  }

  @Override
  public Concrete.Expression visitInferenceReference(Concrete.InferenceReferenceExpression expr, P params) {
    return new Concrete.InferenceReferenceExpression(expr.getData(), expr.getVariable());
  }

  protected <T extends Concrete.Parameter> T visitParameter(T parameter, P params) {
    // TODO
    return parameter;
  }

  public <T extends Concrete.Parameter> List<T> visitParameters(List<T> parameters, P params) {
    return parameters.stream()
      .map(parameter -> visitParameter(parameter, params))
      .collect(Collectors.toList());
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, P params) {
    var newParams = visitParameters(expr.getParameters(), params);
    return new Concrete.LamExpression(expr.getData(), newParams, expr.body.accept(this, params));
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, P params) {
    var newParams = visitParameters(expr.getParameters(), params);
    return new Concrete.PiExpression(expr.getData(), newParams, expr.codomain.accept(this, params));
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, P params) {
    return new Concrete.UniverseExpression(expr.getData(), expr.getPLevel(), expr.getHLevel());
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, P params) {
    return new Concrete.HoleExpression(expr.getData());
  }

  @Override
  public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, P params) {
    return new Concrete.ApplyHoleExpression(expr.getData());
  }

  @Contract(pure = true, value = "null, _->null;!null, _->!null")
  protected @Nullable Concrete.Expression nullableMap(Concrete.Expression expr, P params) {
    if (expr != null) return expr.accept(this, params);
    else return null;
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, P params) {
    Concrete.Expression expression = null;
    if (expr.expression != null) {
      expression = expr.expression.accept(this, params);
    }
    return new Concrete.GoalExpression(
      expr.getData(),
      expr.getName(),
      expression,
      expr.goalSolver,
      expr.useGoalSolver,
      expr.errors
    );
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, P params) {
    return new Concrete.TupleExpression(
      expr.getData(),
      expr.getFields().stream()
        .map(field -> field.accept(this, params))
        .collect(Collectors.toList())
    );
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, P params) {
    var newParams = visitParameters(expr.getParameters(), params);
    return new Concrete.SigmaExpression(expr.getData(), newParams);
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, P params) {
    if (expr.getSequence().size() == 1) {
      return expr.getSequence().get(0).expression.accept(this, params);
    }

    return new Concrete.BinOpSequenceExpression(
      expr.getData(),
      expr.getSequence().stream()
        .map(elem -> new Concrete.BinOpSequenceElem(
          elem.expression.accept(this, params),
          elem.fixity,
          elem.isExplicit))
        .collect(Collectors.toList())
    );
  }

  protected Concrete.Pattern visitPattern(Concrete.Pattern pattern, P params) {
    if (Concrete.NamePattern.class.equals(pattern.getClass())) {
      var namePattern = (Concrete.NamePattern) pattern;
      return new Concrete.NamePattern(
        namePattern.getData(),
        namePattern.isExplicit(),
        namePattern.getReferable(),
        namePattern.type
      );
    } else if (Concrete.ConstructorPattern.class.equals(pattern.getClass())) {
      var conPattern = (Concrete.ConstructorPattern) pattern;
      return new Concrete.ConstructorPattern(
        conPattern.getData(),
        conPattern.isExplicit(),
        conPattern.getConstructor(),
        conPattern.getPatterns().stream()
          .map(pat -> visitPattern(pat, params))
          .collect(Collectors.toList()),
        conPattern.getAsReferables().stream()
          .map(ref -> new Concrete.TypedReferable(
            ref.getData(),
            ref.referable,
            ref.type.accept(this, params)))
          .collect(Collectors.toList())
      );
    } else if (Concrete.TuplePattern.class.equals(pattern.getClass())) {
      var tuplePattern = (Concrete.TuplePattern) pattern;
      return new Concrete.TuplePattern(
        tuplePattern.getData(),
        tuplePattern.isExplicit(),
        tuplePattern.getPatterns().stream()
          .map(pat -> visitPattern(pat, params))
          .collect(Collectors.toList()),
        tuplePattern.getAsReferables().stream()
          .map(ref -> new Concrete.TypedReferable(
            ref.getData(),
            ref.referable,
            ref.type.accept(this, params)))
          .collect(Collectors.toList())
      );
    } else {
      throw new IllegalArgumentException("Unhandled pattern: " + pattern.getClass());
    }
  }

  @SuppressWarnings("unchecked")
  protected <T extends Concrete.Clause> T visitClause(T clause, P params) {
    if (Concrete.ConstructorClause.class.equals(clause.getClass())) {
      var conClause = (Concrete.ConstructorClause) clause;
      return (T) new Concrete.ConstructorClause(
        clause.getData(),
        clause.getPatterns().stream()
          .map(pat -> visitPattern(pat, params))
          .collect(Collectors.toList()),
        new ArrayList<>(conClause.getConstructors())
      );
    } else if (Concrete.FunctionClause.class.equals(clause.getClass())) {
      var funcClause = (Concrete.FunctionClause) clause;
      return (T) new Concrete.FunctionClause(
        funcClause.getData(),
        funcClause.getPatterns().stream()
          .map(pat -> visitPattern(pat, params))
          .collect(Collectors.toList()),
        funcClause.expression.accept(this, params)
      );
    } else {
      throw new IllegalArgumentException("Unhandled clause: " + clause.getClass());
    }
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, P params) {
    return new Concrete.CaseExpression(
      expr.getData(),
      expr.isSCase(),
      expr.getArguments().stream()
        .map(arg -> new Concrete.CaseArgument(
          arg.expression.accept(this, params),
          arg.referable,
          nullableMap(arg.type, params)))
        .collect(Collectors.toList()),
      nullableMap(expr.getResultType(), params),
      nullableMap(expr.getResultTypeLevel(), params),
      expr.getClauses().stream()
        .map(clause -> visitClause(clause, params))
        .collect(Collectors.toList())
    );
  }

  @Override
  public Concrete.Expression visitEval(Concrete.EvalExpression expr, P params) {
    return new Concrete.EvalExpression(expr.getData(), expr.isPEval(), expr.getExpression().accept(this, params));
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, P params) {
    return new Concrete.ProjExpression(expr.getData(), expr.expression.accept(this, params), expr.getField());
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, P params) {
    return Concrete.ClassExtExpression.make(
      expr.getData(),
      expr.getBaseClassExpression().accept(this, params),
      expr.getStatements().stream()
        .map(statement -> visitClassElement(statement, params))
        .collect(Collectors.toList())
    );
  }

  @SuppressWarnings("unchecked")
  protected <T extends Concrete.ClassElement> T visitClassElement(T element, P params) {
    if (Concrete.ClassFieldImpl.class.equals(element.getClass())) {
      var field = (Concrete.ClassFieldImpl) element;
      return (T) new Concrete.ClassFieldImpl(
        element.getData(),
        field.getImplementedField(),
        field.implementation.accept(this, params),
        field.subClassFieldImpls.stream()
          .map(classField -> visitClassElement(classField, params))
          .collect(Collectors.toList()));
    } else if (Concrete.ClassField.class.equals(element.getClass())) {
      var field = (Concrete.ClassField) element;
      return (T) new Concrete.ClassField(
        field.getData(),
        field.getRelatedDefinition(),
        field.isExplicit(),
        field.getKind(),
        visitParameters(field.getParameters(), params),
        nullableMap(field.getResultType(), params),
        nullableMap(field.getResultTypeLevel(), params)
      );
    } else if (Concrete.OverriddenField.class.equals(element.getClass())) {
      var field = (Concrete.OverriddenField) element;
      return (T) new Concrete.OverriddenField(
        field.getData(),
        field.getOverriddenField(),
        visitParameters(field.getParameters(), params),
        field.getResultType().accept(this, params),
        nullableMap(field.getResultTypeLevel(), params)
      );
    } else if (Concrete.CoClauseFunctionReference.class.equals(element.getClass())) {
      var field = (Concrete.CoClauseFunctionReference) element;
      return (T) new Concrete.CoClauseFunctionReference(
        field.getData(),
        field.getImplementedField(),
        field.getFunctionReference()
      );
    } else {
      throw new IllegalArgumentException("Unhandled field: " + element.getClass());
    }
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, P params) {
    return new Concrete.NewExpression(expr.getData(), expr.expression.accept(this, params));
  }

  private Concrete.LetClause visitLetClause(Concrete.LetClause clause, P params) {
    return clause.copy(
      parameter -> visitParameter(parameter, params),
      expression -> nullableMap(expression, params));
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, P params) {
    return new Concrete.LetExpression(
      expr.getData(),
      expr.isStrict(),
      expr.getClauses().stream()
        .map(clause -> visitLetClause(clause, params))
        .collect(Collectors.toList()),
      expr.expression.accept(this, params)
    );
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, P params) {
    return new Concrete.NumericLiteral(expr.getData(), expr.getNumber());
  }

  @Override
  public Concrete.Expression visitStringLiteral(Concrete.StringLiteral expr, P params) {
    return new Concrete.StringLiteral(expr.getData(), expr.getUnescapedString());
  }

  @Override
  public Concrete.Expression visitTyped(Concrete.TypedExpression expr, P params) {
    return new Concrete.TypedExpression(
      expr.getData(),
      expr.expression.accept(this, params),
      expr.type.accept(this, params)
    );
  }
}
