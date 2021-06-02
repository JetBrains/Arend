package org.arend.core.subst;

import org.arend.core.constructor.ClassConstructor;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.inference.LambdaInferenceVariable;
import org.arend.core.context.binding.inference.MetaInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.UnusedIntervalDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.let.HaveClause;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.visitor.ExpressionTransformer;
import org.arend.core.pattern.Pattern;

import java.util.*;

public class SubstVisitor extends ExpressionTransformer<Void> {
  private final ExprSubstitution myExprSubstitution;
  private final LevelSubstitution myLevelSubstitution;
  private final boolean myClearInferenceVariables;

  public static class SubstException extends RuntimeException {}

  public SubstVisitor(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    this(exprSubstitution, levelSubstitution, true);
  }

  public SubstVisitor(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution, boolean clearInferenceVariables) {
    myExprSubstitution = exprSubstitution;
    myLevelSubstitution = levelSubstitution;
    myClearInferenceVariables = clearInferenceVariables;
  }

  public ExprSubstitution getExprSubstitution() {
    return myExprSubstitution;
  }

  public LevelSubstitution getLevelSubstitution() {
    return myLevelSubstitution;
  }

  public boolean isEmpty() {
    return myExprSubstitution.isEmpty() && myLevelSubstitution.isEmpty();
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    return AppExpression.make(expr.getFunction().accept(this, null), expr.getArgument().accept(this, null), expr.isExplicit());
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }
    return expr.getDefinition().getDefCall(expr.getLevels().subst(myLevelSubstitution), args);
  }

  @Override
  protected ConCallExpression makeConCall(Constructor constructor, LevelPair levels, List<Expression> dataTypeArguments, List<Expression> arguments) {
    return new ConCallExpression(constructor, levels.subst(myLevelSubstitution), dataTypeArguments, arguments);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Void params) {
    if (expr.getDefCallArguments().isEmpty()) {
      List<Expression> dataTypeArgs = new ArrayList<>(expr.getDataTypeArguments().size());
      for (Expression parameter : expr.getDataTypeArguments()) {
        dataTypeArgs.add(parameter.accept(this, null));
      }
      return ConCallExpression.make(expr.getDefinition(), expr.getLevels().subst(myLevelSubstitution), dataTypeArgs, Collections.emptyList());
    } else {
      return super.visitConCall(expr, null);
    }
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Void params) {
    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassCallExpression result = new ClassCallExpression(expr.getDefinition(), expr.getLevels().subst(myLevelSubstitution), fieldSet, expr.getSort().subst(myLevelSubstitution), expr.getUniverseKind());
    if (expr.getImplementedHere().isEmpty()) {
      return result;
    }

    myExprSubstitution.add(expr.getThisBinding(), new ReferenceExpression(result.getThisBinding()));
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      fieldSet.put(entry.getKey(), entry.getValue().accept(this, null));
    }
    myExprSubstitution.remove(expr.getThisBinding());
    return result;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    return FieldCallExpression.make(expr.getDefinition(), expr.getLevels().subst(myLevelSubstitution), expr.getArgument().accept(this, null));
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getBinding());
    if (result != null) {
      return result;
    }
    if (expr.getBinding() instanceof EvaluatingBinding) {
      Expression e = ((EvaluatingBinding) expr.getBinding()).getExpression();
      return e.findFreeBindings(myExprSubstitution.getKeys()) == null ? expr : e.accept(this, null);
    }
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() != null) {
      return expr.getSubstExpression().accept(this, null);
    }

    if (!myClearInferenceVariables || expr.getVariable() instanceof MetaInferenceVariable || expr.getVariable() instanceof LambdaInferenceVariable) {
      if (myLevelSubstitution.isEmpty() && Collections.disjoint(expr.getVariable().getBounds(), myExprSubstitution.getKeys())) {
        return expr;
      }

      ExprSubstitution newSubst = new ExprSubstitution();
      for (Binding var : expr.getVariable().getBounds()) {
        Expression substExpr = myExprSubstitution.get(var);
        if (substExpr != null) {
          newSubst.add(var, substExpr);
        }
      }
      return SubstExpression.make(expr, newSubst, myLevelSubstitution);
    }

    expr.getVariable().getBounds().removeAll(myExprSubstitution.getKeys());
    return expr;
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    return SubstExpression.make(expr, myExprSubstitution, myLevelSubstitution);
  }

  @Override
  public Expression visitLam(LamExpression expr, Void params) {
    boolean isUnused = expr.getParameters() == UnusedIntervalDependentLink.INSTANCE;
    SingleDependentLink oldParameters = isUnused ? expr.getParameters().getNext() : expr.getParameters();
    Expression result;
    if (oldParameters.hasNext()) {
      SingleDependentLink parameters = DependentLink.Helper.subst(oldParameters, this);
      result = new LamExpression(expr.getResultSort().subst(myLevelSubstitution), parameters, expr.getBody().accept(this, null));
      DependentLink.Helper.freeSubsts(oldParameters, myExprSubstitution);
    } else {
      result = expr.getBody().accept(this, null);
    }
    return isUnused ? new LamExpression(expr.getResultSort().subst(myLevelSubstitution), UnusedIntervalDependentLink.INSTANCE, result) : result;
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), this);
    PiExpression result = new PiExpression(expr.getResultSort().subst(myLevelSubstitution), parameters, expr.getCodomain().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    SigmaExpression result = new SigmaExpression(expr.getSort().subst(myLevelSubstitution), DependentLink.Helper.subst(expr.getParameters(), this));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return myLevelSubstitution.isEmpty() ? expr : new UniverseExpression(expr.getSort().subst(myLevelSubstitution));
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpression() == null ? expr : expr.replaceExpression(expr.getExpression().accept(this, null));
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    Expression arg = visitSigma(expr.getSigmaType(), null);
    if (!(arg instanceof SigmaExpression)) {
      throw new SubstException();
    }
    return new TupleExpression(fields, (SigmaExpression) arg);
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return ProjExpression.make(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    Expression renewExpression = expr.getRenewExpression() == null ? null : expr.getRenewExpression().accept(this, null);
    Expression arg = visitClassCall(expr.getClassCall(), null);
    if (!(arg instanceof ClassCallExpression)) {
      throw new SubstException();
    }
    return new NewExpression(renewExpression, (ClassCallExpression) arg, false);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    return new PEvalExpression(expr.getExpression().accept(this, null));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Void params) {
    List<HaveClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (HaveClause clause : letExpression.getClauses()) {
      HaveClause newClause = LetClause.make(clause instanceof LetClause, clause.getName(), clause.getPattern(), clause.getExpression().accept(this, null));
      clauses.add(newClause);
      myExprSubstitution.add(clause, new ReferenceExpression(newClause));
    }
    LetExpression result = new LetExpression(letExpression.isStrict(), clauses, letExpression.getExpression().accept(this, null));
    letExpression.getClauses().forEach(myExprSubstitution::remove);
    return result;
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    List<Expression> arguments = new ArrayList<>(expr.getArguments().size());
    for (Expression arg : expr.getArguments()) {
      arguments.add(arg.accept(this, null));
    }

    DependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), this);
    Expression type = expr.getResultType().accept(this, null);
    Expression typeLevel = expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(this, null);
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);

    List<ElimClause<Pattern>> clauses = new ArrayList<>();
    for (ElimClause<Pattern> clause : expr.getElimBody().getClauses()) {
      DependentLink clauseParameters = DependentLink.Helper.subst(clause.getParameters(), this);
      Expression clauseExpr = clause.getExpression() == null ? null : clause.getExpression().accept(this, null);
      DependentLink.Helper.freeSubsts(clause.getParameters(), myExprSubstitution);
      clauses.add(new ElimClause<>(Pattern.replaceBindings(clause.getPatterns(), clauseParameters), clauseExpr));
    }
    return new CaseExpression(expr.isSCase(), parameters, type, typeLevel, new ElimBody(clauses, myLevelSubstitution.isEmpty() ? expr.getElimBody().getElimTree() : substElimTree(expr.getElimBody().getElimTree())), arguments);
  }

  public ElimTree substElimTree(ElimTree elimTree) {
    if (!(elimTree instanceof BranchElimTree)) {
      return elimTree;
    }

    BranchElimTree result = new BranchElimTree(elimTree.getSkip(), ((BranchElimTree) elimTree).keepConCall());
    for (Map.Entry<BranchKey, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
      BranchKey key;
      if (entry.getKey() instanceof ClassConstructor) {
        ClassConstructor classCon = (ClassConstructor) entry.getKey();
        key = new ClassConstructor(classCon.getClassDefinition(), classCon.getLevels().subst(myLevelSubstitution), classCon.getImplementedFields());
      } else {
        key = entry.getKey();
      }
      result.addChild(key, substElimTree(entry.getValue()));
    }
    return result;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return new OfTypeExpression(expr.getExpression().accept(this, null), expr.getTypeOf().accept(this, null));
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitTypeCoerce(TypeCoerceExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getClauseArguments().size());
    for (Expression arg : expr.getClauseArguments()) {
      args.add(arg.accept(this, null));
    }
    return TypeCoerceExpression.make(expr.getDefinition(), expr.getLevels().subst(myLevelSubstitution), expr.getClauseIndex(), args, expr.getArgument().accept(this, null), expr.isFromLeftToRight());
  }

  @Override
  public Expression visitArray(ArrayExpression expr, Void params) {
    List<Expression> elements = new ArrayList<>(expr.getElements().size());
    for (Expression arg : expr.getElements()) {
      elements.add(arg.accept(this, null));
    }
    return ArrayExpression.make(expr.getLevels().subst(myLevelSubstitution), expr.getElementsType().accept(this, null), elements, expr.getTail() == null ? null : expr.getTail().accept(this, null));
  }
}
