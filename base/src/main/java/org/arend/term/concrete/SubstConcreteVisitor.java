package org.arend.term.concrete;

import org.arend.ext.reference.DataContainer;
import org.arend.naming.reference.Referable;
import org.arend.typechecking.instance.pool.RecursiveInstanceHoleExpression;
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
public class SubstConcreteVisitor extends BaseConcreteExpressionVisitor<Void> implements DataContainer, ConcreteLevelExpressionVisitor<Void, Concrete.LevelExpression> {
  private final Map<Referable, Concrete.Expression> mySubstitution;
  private final Map<Referable, Concrete.LevelExpression> myLevelSubstitution = new HashMap<>();
  private final Map<Referable, Referable> myRefMap = new HashMap<>();
  private final Object myData;

  public SubstConcreteVisitor(@NotNull Map<Referable, Concrete.Expression> substitution, Object data) {
    mySubstitution = substitution;
    myData = data;
  }

  public SubstConcreteVisitor(@Nullable Object data) {
    this(new HashMap<>(), data);
  }

  @Override
  public @Nullable Object getData() {
    return myData;
  }

  public void bind(@NotNull Referable referable, @NotNull Concrete.Expression expression) {
    mySubstitution.put(referable, expression);
  }

  public void bind(@NotNull Referable referable, @NotNull Referable newReferable) {
    myRefMap.put(referable, newReferable);
  }

  public void bind(@NotNull Referable referable, @NotNull Concrete.LevelExpression expression) {
    myLevelSubstitution.put(referable, expression);
  }

  public void unbind(@NotNull Referable referable) {
    mySubstitution.remove(referable);
  }

  public int size() {
    return mySubstitution.size() + myRefMap.size();
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void ignored) {
    // It is important that we process arguments first since setFunction modifies the list of arguments.
    var args = expr.getArguments().stream()
      .map(argument -> new Concrete.Argument(argument.expression.accept(this, null), argument.isExplicit()))
      .collect(Collectors.toList());
    return Concrete.AppExpression.make(
      myData != null ? myData : expr.getData(),
      expr.getFunction().accept(this, null),
      args
    );
  }

  private List<Concrete.LevelExpression> visitLevels(List<Concrete.LevelExpression> levels) {
    if (levels == null) return null;
    List<Concrete.LevelExpression> result = new ArrayList<>(levels.size());
    for (Concrete.LevelExpression level : levels) {
      result.add(level == null ? null : level.accept(this, null));
    }
    return result;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void ignored) {
    var subst = mySubstitution.get(expr.getReferent());
    if (subst != null) return subst;
    var newRef = myRefMap.get(expr.getReferent());
    if (newRef == null) newRef = expr.getReferent();
    var data = myData != null ? myData : expr.getData();
    if (Concrete.LongReferenceExpression.class.equals(expr.getClass())) {
      var longRef = (Concrete.LongReferenceExpression) expr;
      return new Concrete.LongReferenceExpression(data, longRef.getQualifier(), longRef.getLongName(), newRef, visitLevels(longRef.getPLevels()), visitLevels(longRef.getHLevels()));
    } else if (Concrete.FixityReferenceExpression.class.equals(expr.getClass())) {
      var fixityRef = (Concrete.FixityReferenceExpression) expr;
      return new Concrete.FixityReferenceExpression(data, newRef, fixityRef.fixity);
    } else if (Concrete.ReferenceExpression.class.equals(expr.getClass())) {
      return new Concrete.ReferenceExpression(data, newRef, visitLevels(expr.getPLevels()), visitLevels(expr.getHLevels()));
    } else {
      throw new IllegalArgumentException("Unhandled reference expr: " + expr.getClass());
    }
  }

  @Override
  public Concrete.Expression visitThis(Concrete.ThisExpression expr, Void ignored) {
    return expr;
  }

  @SuppressWarnings("unchecked")
  protected <T extends Concrete.Parameter> T visitParameter(T parameter) {
    var data = myData != null ? myData : parameter.getData();
    if (Concrete.NameParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.NameParameter(data, parameter.isExplicit(), ((Concrete.NameParameter) parameter).getReferable());
    } else if (Concrete.TypeParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.TypeParameter(data, parameter.isExplicit(), nullableMap(parameter.getType()), parameter.isProperty());
    } else if (Concrete.DefinitionTypeParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.DefinitionTypeParameter(data, parameter.isExplicit(), parameter.isStrict(), nullableMap(parameter.getType()), parameter.isProperty());
    } else if (Concrete.TelescopeParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.TelescopeParameter(data, parameter.isExplicit(), new ArrayList<>(parameter.getReferableList()), nullableMap(parameter.getType()), parameter.isProperty());
    } else if (Concrete.DefinitionTelescopeParameter.class.equals(parameter.getClass())) {
      return (T) new Concrete.DefinitionTelescopeParameter(data, parameter.isExplicit(), parameter.isStrict(), new ArrayList<>(parameter.getReferableList()), nullableMap(parameter.getType()), parameter.isProperty());
    } else {
      throw new IllegalArgumentException("Unhandled parameter: " + parameter.getClass());
    }
  }

  public <T extends Concrete.Parameter> List<T> visitParameters(List<T> parameters) {
    return parameters.stream().map(this::visitParameter).collect(Collectors.toList());
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void ignored) {
    if (expr instanceof Concrete.PatternLamExpression) {
      return Concrete.PatternLamExpression.make(myData != null ? myData : expr.getData(), visitParameters(expr.getParameters()), visitPatterns(((Concrete.PatternLamExpression) expr).getPatterns()), expr.body.accept(this, null));
    } else {
      return new Concrete.LamExpression(myData != null ? myData : expr.getData(), visitParameters(expr.getParameters()), expr.body.accept(this, null));
    }
  }

  @Override
  public Concrete.Expression visitPi(Concrete.PiExpression expr, Void ignored) {
    return new Concrete.PiExpression(myData != null ? myData : expr.getData(), visitParameters(expr.getParameters()), expr.codomain.accept(this, null));
  }

  @Override
  public Concrete.Expression visitUniverse(Concrete.UniverseExpression expr, Void ignored) {
    if (myData == null && myLevelSubstitution.isEmpty()) return expr;
    return new Concrete.UniverseExpression(myData == null ? expr.getData() : myData, expr.getPLevel() == null ? null : expr.getPLevel().accept(this, null), expr.getHLevel() == null ? null : expr.getHLevel().accept(this, null));
  }

  @Override
  public Concrete.Expression visitHole(Concrete.HoleExpression expr, Void ignored) {
    return myData == null ? expr :
      expr instanceof Concrete.ErrorHoleExpression ? new Concrete.ErrorHoleExpression(myData, expr.getError()) :
      expr instanceof RecursiveInstanceHoleExpression ? new RecursiveInstanceHoleExpression(myData, ((RecursiveInstanceHoleExpression) expr).recursiveData) :
        new Concrete.HoleExpression(myData);
  }

  @Override
  public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, Void ignored) {
    return myData == null ? expr : new Concrete.ApplyHoleExpression(myData);
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
    var data = myData != null ? myData : expr.getData();
    if (expr instanceof Concrete.IncompleteExpression) {
      return new Concrete.IncompleteExpression(data);
    }
    return new Concrete.GoalExpression(data, expr.getName(), nullableMap(expr.expression), expr.goalSolver, expr.useGoalSolver, expr.errors);
  }

  @Override
  public Concrete.Expression visitTuple(Concrete.TupleExpression expr, Void ignored) {
    return new Concrete.TupleExpression(myData != null ? myData : expr.getData(), listMap(expr.getFields()));
  }

  @Override
  public Concrete.Expression visitSigma(Concrete.SigmaExpression expr, Void ignored) {
    return new Concrete.SigmaExpression(myData != null ? myData : expr.getData(), visitParameters(expr.getParameters()));
  }

  @Override
  public Concrete.Expression visitBinOpSequence(Concrete.BinOpSequenceExpression expr, Void ignored) {
    if (expr.getSequence().size() == 1) {
      return expr.getSequence().get(0).getComponent().accept(this, null);
    }
    var clauses = expr.getClauses();

    var functionClauses = clauses == null ? null : new Concrete.FunctionClauses(
      myData != null ? myData : clauses.getData(),
      clauses.getClauseList().stream().map(this::visitClause).collect(Collectors.toList()));
    return new Concrete.BinOpSequenceExpression(
      myData != null ? myData : expr.getData(),
      expr.getSequence().stream()
        .map(elem -> new Concrete.BinOpSequenceElem<>(elem.getComponent().accept(this, null), elem.fixity, elem.isExplicit))
        .collect(Collectors.toList()),
      functionClauses
    );
  }

  protected Concrete.Pattern visitPattern(Concrete.Pattern pattern) {
    if (pattern == null) return null;
    var data = myData != null ? myData : pattern.getData();
    if (pattern instanceof Concrete.NamePattern namePattern) {
      return new Concrete.NamePattern(data, namePattern.isExplicit(), namePattern.getReferable(), namePattern.type);
    } else if (pattern instanceof Concrete.ConstructorPattern conPattern) {
      return new Concrete.ConstructorPattern(data, conPattern.isExplicit(), conPattern.getConstructorData(), conPattern.getConstructor(), visitPatterns(conPattern.getPatterns()), visitTypedReferable(conPattern.getAsReferable()));
    } else if (pattern instanceof Concrete.TuplePattern tuplePattern) {
      return new Concrete.TuplePattern(data, tuplePattern.isExplicit(), visitPatterns(tuplePattern.getPatterns()), visitTypedReferable(tuplePattern.getAsReferable()));
    } else if (pattern instanceof Concrete.NumberPattern numberPattern) {
      return new Concrete.NumberPattern(data, numberPattern.getNumber(), visitTypedReferable(numberPattern.getAsReferable()));
    } else {
      throw new IllegalArgumentException("Unhandled pattern: " + pattern.getClass());
    }
  }

  private Concrete.TypedReferable visitTypedReferable(Concrete.TypedReferable asReferable) {
    return asReferable == null ? null : new Concrete.TypedReferable(myData != null ? myData : asReferable.getData(), asReferable.referable, asReferable.type.accept(this, null));
  }

  private @NotNull List<Concrete.Pattern> visitPatterns(List<Concrete.Pattern> patterns) {
    return patterns.stream().map(this::visitPattern).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  protected <T extends Concrete.Clause> T visitClause(T clause) {
    if (Concrete.ConstructorClause.class.equals(clause.getClass())) {
      var conClause = (Concrete.ConstructorClause) clause;
      return (T) new Concrete.ConstructorClause(myData != null ? myData : clause.getData(), visitPatterns(clause.getPatterns()), new ArrayList<>(conClause.getConstructors()));
    } else if (Concrete.FunctionClause.class.equals(clause.getClass())) {
      var funcClause = (Concrete.FunctionClause) clause;
      return (T) new Concrete.FunctionClause(myData != null ? myData : funcClause.getData(), visitPatterns(funcClause.getPatterns()), funcClause.expression.accept(this, null));
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
    return new Concrete.CaseExpression(myData != null ? myData : expr.getData(), expr.isSCase(), arguments, nullableMap(expr.getResultType()), nullableMap(expr.getResultTypeLevel()), clauses);
  }

  @Override
  public Concrete.Expression visitEval(Concrete.EvalExpression expr, Void ignored) {
    return new Concrete.EvalExpression(myData != null ? myData : expr.getData(), expr.isPEval(), expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitBox(Concrete.BoxExpression expr, Void params) {
    return new Concrete.BoxExpression(myData != null ? myData : expr.getData(), expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.Expression visitProj(Concrete.ProjExpression expr, Void ignored) {
    return new Concrete.ProjExpression(myData != null ? myData : expr.getData(), expr.expression.accept(this, null), expr.getField());
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void ignored) {
    var coclauses = expr.getCoclauses();
    var statements = coclauses.getCoclauseList().stream().map(this::visitClassElement).collect(Collectors.toList());
    var newCoclauses = new Concrete.Coclauses(myData != null ? myData : coclauses.getData(), statements);
    return Concrete.ClassExtExpression.make(myData != null ? myData : expr.getData(), expr.getBaseClassExpression().accept(this, null), newCoclauses);
  }

  @SuppressWarnings("unchecked")
  protected <T extends Concrete.ClassElement> T visitClassElement(T element) {
    if (Concrete.CoClauseFunctionReference.class.equals(element.getClass())) {
      var field = (Concrete.CoClauseFunctionReference) element;
      return (T) new Concrete.CoClauseFunctionReference(myData != null ? myData : field.getData(), field.getImplementedField(), field.getFunctionReference(), field.isDefault());
    } else if (Concrete.ClassFieldImpl.class.equals(element.getClass())) {
      var field = (Concrete.ClassFieldImpl) element;
      var subCoclauses = field.getSubCoclauses();
      var subClassFieldImpls = subCoclauses == null ? null : new Concrete.Coclauses(
        myData != null ? myData : subCoclauses.getData(),
        subCoclauses.getCoclauseList().stream().map(this::visitClassElement).collect(Collectors.toList()));
      return (T) new Concrete.ClassFieldImpl(myData != null ? myData : element.getData(), field.getImplementedField(), field.implementation.accept(this, null), subClassFieldImpls, field.isDefault());
    } else if (Concrete.ClassField.class.equals(element.getClass())) {
      var field = (Concrete.ClassField) element;
      // Ideally we should replace this `field.getData()` too
      return (T) new Concrete.ClassField(field.getData(), field.getRelatedDefinition(), field.isExplicit(), field.getKind(), visitParameters(field.getParameters()), nullableMap(field.getResultType()), nullableMap(field.getResultTypeLevel()), field.isCoerce());
    } else if (Concrete.OverriddenField.class.equals(element.getClass())) {
      var field = (Concrete.OverriddenField) element;
      return (T) new Concrete.OverriddenField(myData != null ? myData : field.getData(), field.getOverriddenField(), visitParameters(field.getParameters()), field.getResultType().accept(this, null), nullableMap(field.getResultTypeLevel()));
    } else {
      throw new IllegalArgumentException("Unhandled field: " + element.getClass());
    }
  }

  @Override
  public Concrete.Expression visitNew(Concrete.NewExpression expr, Void ignored) {
    return new Concrete.NewExpression(myData != null ? myData : expr.getData(), expr.expression.accept(this, null));
  }

  private Concrete.LetClause visitLetClause(Concrete.LetClause clause) {
    return new Concrete.LetClause(clause.getParameters().stream().map(this::visitParameter).collect(Collectors.toList()), nullableMap(clause.getResultType()), nullableMap(clause.getTerm()), visitPattern(clause.getPattern()));
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void ignored) {
    var clauses = expr.getClauses().stream().map(this::visitLetClause).collect(Collectors.toList());
    return new Concrete.LetExpression(myData != null ? myData : expr.getData(), expr.isHave(), expr.isStrict(), clauses, expr.expression.accept(this, null));
  }

  @Override
  public Concrete.Expression visitNumericLiteral(Concrete.NumericLiteral expr, Void ignored) {
    return myData == null ? expr : new Concrete.NumericLiteral(myData, expr.getNumber());
  }

  @Override
  public Concrete.Expression visitStringLiteral(Concrete.StringLiteral expr, Void ignored) {
    return myData == null ? expr : new Concrete.StringLiteral(myData, expr.getUnescapedString());
  }

  @Override
  public Concrete.Expression visitTyped(Concrete.TypedExpression expr, Void ignored) {
    return new Concrete.TypedExpression(myData != null ? myData : expr.getData(), expr.expression.accept(this, null), expr.type.accept(this, null));
  }

  @Override
  public Concrete.LevelExpression visitInf(Concrete.InfLevelExpression expr, Void param) {
    return myData == null ? expr : new Concrete.InfLevelExpression(myData);
  }

  @Override
  public Concrete.LevelExpression visitLP(Concrete.PLevelExpression expr, Void param) {
    return myData == null ? expr : new Concrete.PLevelExpression(myData);
  }

  @Override
  public Concrete.LevelExpression visitLH(Concrete.HLevelExpression expr, Void param) {
    return myData == null ? expr : new Concrete.HLevelExpression(myData);
  }

  @Override
  public Concrete.LevelExpression visitNumber(Concrete.NumberLevelExpression expr, Void param) {
    return myData == null ? expr : new Concrete.NumberLevelExpression(myData, expr.getNumber());
  }

  @Override
  public Concrete.LevelExpression visitId(Concrete.IdLevelExpression expr, Void param) {
    Concrete.LevelExpression result = myLevelSubstitution.get(expr.getReferent());
    return result != null ? (myData == null ? result : result.accept(new SubstConcreteVisitor(myData), null)) : (myData == null ? expr : new Concrete.IdLevelExpression(myData, expr.getReferent()));
  }

  @Override
  public Concrete.LevelExpression visitSuc(Concrete.SucLevelExpression expr, Void param) {
    return new Concrete.SucLevelExpression(myData == null ? expr.getData() : myData, expr.getExpression().accept(this, null));
  }

  @Override
  public Concrete.LevelExpression visitMax(Concrete.MaxLevelExpression expr, Void param) {
    return new Concrete.MaxLevelExpression(myData == null ? expr.getData() : myData, expr.getLeft().accept(this, null), expr.getRight().accept(this, null));
  }

  @Override
  public Concrete.LevelExpression visitVar(Concrete.VarLevelExpression expr, Void param) {
    return myData == null ? expr : new Concrete.VarLevelExpression(myData, expr.getVariable());
  }
}
