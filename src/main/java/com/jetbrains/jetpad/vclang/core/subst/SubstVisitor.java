package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.BaseExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubstVisitor extends BaseExpressionVisitor<Void, Expression> {
  private final ExprSubstitution myExprSubstitution;
  private final LevelSubstitution myLevelSubstitution;

  public SubstVisitor(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    myExprSubstitution = exprSubstitution;
    myLevelSubstitution = levelSubstitution;
  }

  @Override
  public AppExpression visitApp(AppExpression expr, Void params) {
    return new AppExpression(expr.getFunction().accept(this, null), expr.getArgument().accept(this, null));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    List<Expression> args = expr.getDefCallArguments().stream().map(arg -> arg.accept(this, null)).collect(Collectors.toList());
    return expr.getDefinition().getDefCall(expr.getSortArgument().subst(myLevelSubstitution), null, args);
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Void params) {
    return (DataCallExpression) visitDefCall(expr, null);
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Void params) {
    List<Expression> dataTypeArgs = expr.getDataTypeArguments().stream().map(arg -> arg.accept(this, null)).collect(Collectors.toList());
    List<Expression> args = expr.getDefCallArguments().stream().map(arg -> arg.accept(this, null)).collect(Collectors.toList());
    return new ConCallExpression(expr.getDefinition(), expr.getSortArgument().subst(myLevelSubstitution), dataTypeArgs, args);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    FieldSet fieldSet = FieldSet.applyVisitorToImplemented(expr.getFieldSet(), expr.getDefinition().getFieldSet(), this, null);
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument().subst(myLevelSubstitution), fieldSet);
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getDefinition());
    if (result != null) {
      return new AppExpression(result, expr.getExpression().accept(this, null));
    } else {
      return ExpressionFactory.FieldCall(expr.getDefinition(), expr.getExpression().accept(this, null));
    }
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getBinding());
    if (result != null) {
      return result;
    }
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() != null) {
      return new InferenceReferenceExpression(expr.getOriginalVariable(), expr.getSubstExpression().accept(this, null));
    }
    Expression result = myExprSubstitution.get(expr.getVariable());
    return result != null ? result : expr;
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    LamExpression result = new LamExpression(expr.getResultSort().subst(myLevelSubstitution), parameters, expr.getBody().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    PiExpression result = new PiExpression(expr.getResultSort().subst(myLevelSubstitution), parameters, expr.getCodomain().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    SigmaExpression result = new SigmaExpression(expr.getSort().subst(myLevelSubstitution), DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr, Void params) {
    return myLevelSubstitution.isEmpty() ? expr : new UniverseExpression(expr.getSort().subst(myLevelSubstitution));
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, null), expr.getError());
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = expr.getFields().stream().map(field -> field.accept(this, null)).collect(Collectors.toList());
    return new TupleExpression(fields, visitSigma(expr.getSigmaType(), null));
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return new ProjExpression(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return new NewExpression(visitClassCall(expr.getExpression(), null));
  }

  @Override
  public LetExpression visitLet(LetExpression letExpression, Void params) {
    List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      LetClause newClause = new LetClause(clause.getName(), clause.getExpression().accept(this, null));
      clauses.add(newClause);
      myExprSubstitution.add(clause, new ReferenceExpression(newClause));
    }
    LetExpression result = new LetExpression(clauses, letExpression.getExpression().accept(this, null));
    letExpression.getClauses().forEach(myExprSubstitution::remove);
    return result;
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    List<Expression> arguments = expr.getArguments().stream().map(arg -> arg.accept(this, null)).collect(Collectors.toList());
    DependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    Expression type = expr.getResultType().accept(this, null);
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return new CaseExpression(parameters, type, substElimTree(expr.getElimTree()), arguments);
  }

  private ElimTree substElimTree(ElimTree elimTree) {
    DependentLink vars = DependentLink.Helper.subst(elimTree.getParameters(), myExprSubstitution, myLevelSubstitution);
    if (elimTree instanceof LeafElimTree) {
      elimTree = new LeafElimTree(vars, ((LeafElimTree) elimTree).getExpression().accept(this, null));
    } else {
      Map<Constructor, ElimTree> children = new HashMap<>();
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        children.put(entry.getKey(), substElimTree(entry.getValue()));
      }
      elimTree = new BranchElimTree(vars, children);
    }
    DependentLink.Helper.freeSubsts(elimTree.getParameters(), myExprSubstitution);
    return elimTree;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return new OfTypeExpression(expr.getExpression().accept(this, null), expr.getTypeOf().accept(this, null));
  }
}
