package org.arend.term.concrete;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @see org.arend.term.concrete.BaseConcreteExpressionVisitor
 */
public class SubstConcreteExpressionVisitor implements ConcreteExpressionVisitor<Void, Concrete.Expression> {
  private final Map<Referable, Concrete.Expression> mySubstitution;

  public SubstConcreteExpressionVisitor(@NotNull Map<Referable, Concrete.Expression> substitution) {
    mySubstitution = substitution;
  }

  public SubstConcreteExpressionVisitor() {
    this(new HashMap<>());
  }

  public void bind(@NotNull Referable referable, @Nullable Concrete.Expression expression) {
    mySubstitution.put(referable, expression);
  }

  public void unbind(@NotNull Referable referable) {
    mySubstitution.remove(referable);
  }

  public int size() {
    return mySubstitution.size();
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void ignored) {
    // It is important that we process arguments first since setFunction modifies the list of arguments.
    var args = expr.getArguments().stream()
      .map(argument -> new Concrete.Argument(argument.expression.accept(this, null), argument.isExplicit()))
      .collect(Collectors.toList());
    return Concrete.AppExpression.make(
      expr.getData(),
      expr.getFunction().accept(this, null),
      args
    );
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void ignored) {
    var subst = mySubstitution.get(expr.getReferent());
    return subst != null ? subst : expr;
  }

  @Override
  public Concrete.Expression visitThis(Concrete.ThisExpression expr, Void ignored) {
    return expr;
  }

  @SuppressWarnings("unchecked")
  protected <T extends Concrete.Parameter> T visitParameter(T parameter) {
    if (Concrete.NameParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.NameParameter(parameter.getData(), parameter.isExplicit(), ((Concrete.NameParameter) parameter).getReferable());
    } else if (Concrete.TypeParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.TypeParameter(parameter.getData(), parameter.isExplicit(), nullableMap(parameter.getType()));
    } else if (Concrete.DefinitionTypeParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.DefinitionTypeParameter(parameter.getData(), parameter.isExplicit(), parameter.isStrict(), nullableMap(parameter.getType()));
    } else if (Concrete.TelescopeParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.TelescopeParameter(parameter.getData(), parameter.isExplicit(), new ArrayList<>(parameter.getReferableList()), nullableMap(parameter.getType()));
    } else if (Concrete.DefinitionTelescopeParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.DefinitionTelescopeParameter(parameter.getData(), parameter.isExplicit(), parameter.isStrict(), new ArrayList<>(parameter.getReferableList()), nullableMap(parameter.getType()));
    } else {
      throw new IllegalArgumentException("Unhandled parameter: " + parameter.getClass());
    }
  }

  public <T extends Concrete.Parameter> List<T> visitParameters(List<T> parameters) {
    return parameters.stream().map(this::visitParameter).collect(Collectors.toList());
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void ignored) {
    return new Concrete.LamExpression(expr.getData(), visitParameters(expr.getParameters()), expr.body.accept(this, null));
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void ignored) {
    return new Concrete.PiExpression(expr.getData(), visitParameters(expr.getParameters()), expr.codomain.accept(this, null));
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Void ignored) {
    return expr;
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, Void ignored) {
    return expr;
  }

  @Override
  public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, Void ignored) {
    return expr;
  }

  @Contract(value = "null -> null; !null -> !null", pure = true)
  protected Concrete.@Nullable Expression nullableMap(Concrete.Expression expr) {
    if (expr != null) return expr.accept(this, null);
    else return null;
  }

  protected List<Concrete.Expression> listMap(List<? extends Concrete.Expression> exprs) {
    return exprs.stream().map(e -> e.accept(this, null)).collect(Collectors.toList());
  }

  @Override
  public Concrete.Expression visitGoal(Concrete.GoalExpression expr, Void ignored) {
    if (expr instanceof Concrete.IncompleteExpression) {
      return new Concrete.IncompleteExpression(expr.getData());
    }
    return new Concrete.GoalExpression(expr.getData(), expr.getName(), nullableMap(expr.expression), expr.goalSolver, expr.useGoalSolver, expr.errors);
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void ignored) {
    return new Concrete.TupleExpression(expr.getData(), listMap(expr.getFields()));
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void ignored) {
    return new Concrete.SigmaExpression(expr.getData(), visitParameters(expr.getParameters()));
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void ignored) {
    if (expr.getSequence().size() == 1) {
      return expr.getSequence().get(0).expression.accept(this, null);
    }
    var clauses = expr.getClauses();

    var functionClauses = clauses == null ? null : new Concrete.FunctionClauses(
      clauses.getData(),
      clauses.getClauseList().stream().map(this::visitClause).collect(Collectors.toList()));
    return new Concrete.BinOpSequenceExpression(
      expr.getData(),
      expr.getSequence().stream()
        .map(elem -> new Concrete.BinOpSequenceElem(elem.expression.accept(this, null), elem.fixity, elem.isExplicit))
        .collect(Collectors.toList()),
      functionClauses
    );
  }

  protected Concrete.Pattern visitPattern(Concrete.Pattern pattern) {
    if (Concrete.NamePattern.class.equals(pattern.getClass())) {
      var namePattern = (Concrete.NamePattern) pattern;
      return new Concrete.NamePattern(namePattern.getData(), namePattern.isExplicit(), namePattern.getReferable(), namePattern.type);
    } else if (Concrete.ConstructorPattern.class.equals(pattern.getClass())) {
      var conPattern = (Concrete.ConstructorPattern) pattern;
      return new Concrete.ConstructorPattern(conPattern.getData(), conPattern.isExplicit(), conPattern.getConstructor(), visitPatterns(conPattern.getPatterns()), visitTypedReferables(conPattern.getAsReferables()));
    } else if (Concrete.TuplePattern.class.equals(pattern.getClass())) {
      var tuplePattern = (Concrete.TuplePattern) pattern;
      return new Concrete.TuplePattern(tuplePattern.getData(), tuplePattern.isExplicit(), visitPatterns(tuplePattern.getPatterns()), visitTypedReferables(tuplePattern.getAsReferables()));
    } else {
      throw new IllegalArgumentException("Unhandled pattern: " + pattern.getClass());
    }
  }

  private List<Concrete.TypedReferable> visitTypedReferables(List<Concrete.TypedReferable> asReferables) {
    return asReferables.stream()
      .map(ref -> new Concrete.TypedReferable(ref.getData(), ref.referable, ref.type.accept(this, null)))
      .collect(Collectors.toList());
  }

  private @NotNull List<Concrete.Pattern> visitPatterns(List<Concrete.Pattern> patterns) {
    return patterns.stream().map(this::visitPattern).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  protected <T extends Concrete.Clause> T visitClause(T clause) {
    if (Concrete.ConstructorClause.class.equals(clause.getClass())) {
      var conClause = (Concrete.ConstructorClause) clause;
      return (T) new Concrete.ConstructorClause(clause.getData(), visitPatterns(clause.getPatterns()), new ArrayList<>(conClause.getConstructors()));
    } else if (Concrete.FunctionClause.class.equals(clause.getClass())) {
      var funcClause = (Concrete.FunctionClause) clause;
      return (T) new Concrete.FunctionClause(funcClause.getData(), visitPatterns(funcClause.getPatterns()), funcClause.expression.accept(this, null));
    } else {
      throw new IllegalArgumentException("Unhandled clause: " + clause.getClass());
    }
  }

  @Override
  public Concrete.Expression visitCase(Concrete.CaseExpression expr, Void ignored) {
    var clauses = expr.getClauses().stream().map(this::visitClause).collect(Collectors.toList());
    var arguments = expr.getArguments().stream()
      .map(arg -> new Concrete.CaseArgument(arg.expression.accept(this, null), arg.referable, nullableMap(arg.type)))
      .collect(Collectors.toList());
    return new Concrete.CaseExpression(expr.getData(), expr.isSCase(), arguments, nullableMap(expr.getResultType()), nullableMap(expr.getResultTypeLevel()), clauses);
  }

  @Override
  public Concrete.Expression visitEval(Concrete.EvalExpression expr, Void ignored) {
    return new Concrete.EvalExpression(expr.getData(), expr.isPEval(), expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void ignored) {
    return new Concrete.ProjExpression(expr.getData(), expr.expression.accept(this, null), expr.getField());
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void ignored) {
    var coclauses = expr.getCoclauses();
    var statements = coclauses.getCoclauseList().stream().map(this::visitClassElement).collect(Collectors.toList());
    var newCoclauses = new Concrete.Coclauses(coclauses.getData(), statements);
    return Concrete.ClassExtExpression.make(expr.getData(), expr.getBaseClassExpression().accept(this, null), newCoclauses);
  }

  @SuppressWarnings("unchecked")
  protected <T extends Concrete.ClassElement> T visitClassElement(T element) {
    if (Concrete.ClassFieldImpl.class.equals(element.getClass())) {
      var field = (Concrete.ClassFieldImpl) element;
      var subCoclauses = field.getSubCoclauses();
      var subClassFieldImpls = subCoclauses == null ? null : new Concrete.Coclauses(
        subCoclauses.getData(),
        subCoclauses.getCoclauseList().stream().map(this::visitClassElement).collect(Collectors.toList()));
      return (T) new Concrete.ClassFieldImpl(element.getData(), field.getImplementedField(), field.implementation.accept(this, null), subClassFieldImpls);
    } else if (Concrete.ClassField.class.equals(element.getClass())) {
      var field = (Concrete.ClassField) element;
      return (T) new Concrete.ClassField(field.getData(), field.getRelatedDefinition(), field.isExplicit(), field.getKind(), visitParameters(field.getParameters()), nullableMap(field.getResultType()), nullableMap(field.getResultTypeLevel()));
    } else if (Concrete.OverriddenField.class.equals(element.getClass())) {
      var field = (Concrete.OverriddenField) element;
      return (T) new Concrete.OverriddenField(field.getData(), field.getOverriddenField(), visitParameters(field.getParameters()), field.getResultType().accept(this, null), nullableMap(field.getResultTypeLevel()));
    } else if (Concrete.CoClauseFunctionReference.class.equals(element.getClass())) {
      var field = (Concrete.CoClauseFunctionReference) element;
      return (T) new Concrete.CoClauseFunctionReference(field.getData(), field.getImplementedField(), field.getFunctionReference());
    } else {
      throw new IllegalArgumentException("Unhandled field: " + element.getClass());
    }
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void ignored) {
    return new Concrete.NewExpression(expr.getData(), expr.expression.accept(this, null));
  }

  private Concrete.LetClause visitLetClause(Concrete.LetClause clause) {
    return clause.copy(this::visitParameter, this::nullableMap);
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void ignored) {
    var clauses = expr.getClauses().stream().map(this::visitLetClause).collect(Collectors.toList());
    return new Concrete.LetExpression(expr.getData(), expr.isStrict(), clauses, expr.expression.accept(this, null));
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, Void ignored) {
    return expr;
  }

  @Override
  public Concrete.Expression visitStringLiteral(Concrete.StringLiteral expr, Void ignored) {
    return expr;
  }

  @Override
  public Concrete.Expression visitTyped(Concrete.TypedExpression expr, Void ignored) {
    return new Concrete.TypedExpression(expr.getData(), expr.expression.accept(this, null), expr.type.accept(this, null));
  }
}
