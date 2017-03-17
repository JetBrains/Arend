package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.BaseExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.ConCall;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Universe;

public class SubstVisitor extends BaseExpressionVisitor<Void, Expression> implements ElimTreeNodeVisitor<Void, ElimTreeNode> {
  private final ExprSubstitution myExprSubstitution;
  private final LevelSubstitution myLevelSubstitution;

  public SubstVisitor(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    myExprSubstitution = exprSubstitution;
    myLevelSubstitution = levelSubstitution;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    List<Expression> arguments = new ArrayList<>(expr.getArguments().size());
    for (Expression argument : expr.getArguments()) {
      arguments.add(argument.accept(this, null));
    }
    return new AppExpression(expr.getFunction().accept(this, null), arguments);
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }
    return expr.getDefinition().getDefCall(expr.getLevelArguments().subst(myLevelSubstitution), args);
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Void params) {
    List<Expression> dataTypeArgs = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression parameter : expr.getDataTypeArguments()) {
      Expression expr2 = parameter.accept(this, null);
      if (expr2 == null) {
        return null;
      }
      dataTypeArgs.add(expr2);
    }

    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }

    return ConCall(expr.getDefinition(), expr.getLevelArguments().subst(myLevelSubstitution), dataTypeArgs, args);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    FieldSet fieldSet = FieldSet.applyVisitorToImplemented(expr.getFieldSet(), expr.getDefinition().getFieldSet(), this, null);
    return new ClassCallExpression(expr.getDefinition(), expr.getLevelArguments().subst(myLevelSubstitution), fieldSet);
  }

  @Override
  public Expression visitLetClauseCall(LetClauseCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }

    Expression subst = myExprSubstitution.get(expr.getLetClause());
    if (subst != null) {
      if (subst.toReference() != null && subst.toReference().getBinding() instanceof LetClause) {
        return new LetClauseCallExpression((LetClause) subst.toReference().getBinding(), args);
      } else {
        return new AppExpression(subst, args);
      }
    } else {
      return new LetClauseCallExpression(expr.getLetClause(), args);
    }
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getDefinition());
    if (result != null) {
      return result.addArgument(expr.getExpression().accept(this, null));
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
    LamExpression result = ExpressionFactory.Lam(parameters, expr.getBody().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    PiExpression result = new PiExpression(expr.getPLevel().subst(myLevelSubstitution), parameters, expr.getCodomain().accept(this, null));
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
  public BranchElimTreeNode visitBranch(BranchElimTreeNode branchNode, Void params) {
    Binding newReference = visitReference(ExpressionFactory.Reference(branchNode.getReference()), null).toReference().getBinding();
    List<Binding> newContextTail = branchNode.getContextTail().stream().map(binding -> visitReference(ExpressionFactory.Reference(binding), null).toReference().getBinding()).collect(Collectors.toList());
    BranchElimTreeNode newNode = new BranchElimTreeNode(newReference, newContextTail);
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      ConstructorClause newClause = newNode.addClause(clause.getConstructor(), DependentLink.Helper.toNames(clause.getParameters()));
      for (DependentLink linkOld = clause.getParameters(), linkNew = newClause.getParameters(); linkOld.hasNext(); linkOld = linkOld.getNext(), linkNew = linkNew.getNext()) {
        myExprSubstitution.add(linkOld, ExpressionFactory.Reference(linkNew));
      }
      for (int i = 0; i < clause.getTailBindings().size(); i++) {
        myExprSubstitution.add(clause.getTailBindings().get(i), ExpressionFactory.Reference(newClause.getTailBindings().get(i)));
      }

      newClause.setChild(clause.getChild().accept(this, null));

      myExprSubstitution.removeAll(DependentLink.Helper.toContext(clause.getParameters()));
      myExprSubstitution.removeAll(clause.getTailBindings());
    }

    if (branchNode.getOtherwiseClause() != null) {
      OtherwiseClause newClause = newNode.addOtherwiseClause();
      newClause.setChild(branchNode.getOtherwiseClause().getChild().accept(this, null));
    }

    return newNode;
  }

  @Override
  public LeafElimTreeNode visitLeaf(LeafElimTreeNode leafNode, Void params) {
    LeafElimTreeNode result = new LeafElimTreeNode(leafNode.getArrow(), leafNode.getExpression().accept(this, null));
    if (leafNode.getMatched() != null) {
      List<Binding> matched = new ArrayList<>(leafNode.getMatched().size());
      for (Binding binding : leafNode.getMatched()) {
        Expression replacement = myExprSubstitution.get(binding);
        matched.add(replacement != null ? replacement.toReference().getBinding() : binding);
      }
      result.setMatched(matched);
    }
    return result;
  }

  @Override
  public ElimTreeNode visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return emptyNode;
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr, Void params) {
    return myLevelSubstitution.isEmpty() ? expr : Universe(expr.getSort().subst(myLevelSubstitution));
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, null), expr.getError());
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return ExpressionFactory.Tuple(fields, visitSigma(expr.getType(), null));
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return ExpressionFactory.Proj(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return ExpressionFactory.New(visitClassCall(expr.getExpression(), null));
  }

  @Override
  public LetExpression visitLet(LetExpression letExpression, Void params) {
    List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      LetClause newClause = visitLetClause(clause);
      clauses.add(newClause);
      myExprSubstitution.add(clause, ExpressionFactory.Reference(newClause));
    }
    LetExpression result = ExpressionFactory.Let(clauses, letExpression.getExpression().accept(this, null));
    letExpression.getClauses().forEach(myExprSubstitution::remove);
    return result;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return new OfTypeExpression(expr.getExpression().accept(this, null), expr.getType().subst(myExprSubstitution, myLevelSubstitution));
  }

  public LetClause visitLetClause(LetClause clause) {
    List<SingleDependentLink> parameters = new ArrayList<>(clause.getParameters().size());
    parameters.addAll(clause.getParameters().stream().map(link -> DependentLink.Helper.subst(link, myExprSubstitution, myLevelSubstitution)).collect(Collectors.toList()));
    List<Level> pLevels = clause.getPLevels().stream().map(pLevel -> pLevel.subst(myLevelSubstitution)).collect(Collectors.toList());
    Expression resultType = clause.getResultType() == null ? null : clause.getResultType().accept(this, null);
    ElimTreeNode elimTree = clause.getElimTree().accept(this, null);
    for (SingleDependentLink link : clause.getParameters()) {
      DependentLink.Helper.freeSubsts(link, myExprSubstitution);
    }
    return new LetClause(clause.getName(), pLevels, parameters, resultType, elimTree);
  }
}
