package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toNames;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

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
  public DefCallExpression visitDefCall(DefCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }
    return expr.getDefinition().getDefCall(expr.getPolyParamsSubst().subst(myLevelSubstitution), args);
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

    ConCallExpression conCall = ConCall(expr.getDefinition(), dataTypeArgs, args);
    conCall.setPolyParamsSubst(expr.getPolyParamsSubst().subst(myLevelSubstitution));
    return conCall;
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    ClassCallExpression classCall = expr.applyVisitorToImplementedHere(this, params);
    classCall.setPolyParamsSubst(classCall.getPolyParamsSubst().subst(myLevelSubstitution));
    return classCall;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getDefinition());
    if (result != null) {
      return Apps(result, expr.getExpression().accept(this, null));
    } else {
      FieldCallExpression defCall = new FieldCallExpression(expr.getDefinition(), expr.getExpression().accept(this, null));
      defCall.setPolyParamsSubst(expr.getPolyParamsSubst().subst(myLevelSubstitution));
      return defCall;
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
    DependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    LamExpression result = Lam(parameters, expr.getBody().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    DependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    PiExpression result = Pi(parameters, expr.getCodomain().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    SigmaExpression result = Sigma(DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public BranchElimTreeNode visitBranch(BranchElimTreeNode branchNode, Void params) {
    Binding newReference = visitReference(Reference(branchNode.getReference()), null).toReference().getBinding();
    List<Binding> newContextTail = new ArrayList<>(branchNode.getContextTail().size());
    for (Binding binding : branchNode.getContextTail()) {
      newContextTail.add(visitReference(Reference(binding), null).toReference().getBinding());
    }

    BranchElimTreeNode newNode = new BranchElimTreeNode(newReference, newContextTail);
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      ConstructorClause newClause = newNode.addClause(clause.getConstructor(), toNames(clause.getParameters()));
      for (DependentLink linkOld = clause.getParameters(), linkNew = newClause.getParameters(); linkOld.hasNext(); linkOld = linkOld.getNext(), linkNew = linkNew.getNext()) {
        myExprSubstitution.add(linkOld, Reference(linkNew));
      }
      for (int i = 0; i < clause.getTailBindings().size(); i++) {
        myExprSubstitution.add(clause.getTailBindings().get(i), Reference(newClause.getTailBindings().get(i)));
      }

      newClause.setChild(clause.getChild().accept(this, null));

      myExprSubstitution.getDomain().removeAll(toContext(clause.getParameters()));
      myExprSubstitution.getDomain().removeAll(clause.getTailBindings());
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
        matched.add(myExprSubstitution.getDomain().contains(binding) ? myExprSubstitution.get(binding).toReference().getBinding() : binding);
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
    return myLevelSubstitution.getDomain().isEmpty() ? expr : Universe(expr.getSort().subst(myLevelSubstitution));
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
    return Tuple(fields, visitSigma(expr.getType(), null));
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return Proj(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return New(visitClassCall(expr.getExpression(), null));
  }

  @Override
  public LetExpression visitLet(LetExpression letExpression, Void params) {
    List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      LetClause newClause = visitLetClause(clause);
      clauses.add(newClause);
      myExprSubstitution.add(clause, Reference(newClause));
    }
    LetExpression result = Let(clauses, letExpression.getExpression().subst(myExprSubstitution, myLevelSubstitution));
    for (LetClause clause : letExpression.getClauses()) {
      myExprSubstitution.getDomain().remove(clause);
    }
    return result;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return new OfTypeExpression(expr.getExpression().accept(this, null), expr.getType().accept(this, null));
  }

  public LetClause visitLetClause(LetClause clause) {
    DependentLink parameters = DependentLink.Helper.subst(clause.getParameters(), myExprSubstitution, myLevelSubstitution);
    Expression resultType = clause.getResultType() == null ? null : clause.getResultType().accept(this, null);
    ElimTreeNode elimTree = clause.getElimTree().accept(this, null);
    DependentLink.Helper.freeSubsts(clause.getParameters(), myExprSubstitution);
    return new LetClause(clause.getName(), parameters, resultType, elimTree);
  }
}
