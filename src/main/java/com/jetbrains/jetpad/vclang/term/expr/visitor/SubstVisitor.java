package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.context.param.DependentLink.Helper.toContext;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class SubstVisitor extends BaseExpressionVisitor<Void, Expression> implements ElimTreeNodeVisitor<Void, ElimTreeNode> {
  private final Substitution mySubstitution;

  public SubstVisitor(Substitution substitution) {
    mySubstitution = substitution;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
   return Apps(expr.getFunction().accept(this, null), new ArgumentExpression(expr.getArgument().getExpression().accept(this, null), expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public DefCallExpression visitDefCall(DefCallExpression expr, Void params) {
    return expr;
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Void params) {
    if (expr.getDataTypeArguments().isEmpty()) return expr;
    List<Expression> parameters = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression parameter : expr.getDataTypeArguments()) {
      Expression expr2 = parameter.accept(this, null);
      if (expr2 == null) return null;
      parameters.add(expr2);
    }
    return ConCall(expr.getDefinition(), parameters);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    Map<ClassField, ClassCallExpression.ImplementStatement> statements = new HashMap<>();
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      statements.put(elem.getKey(), new ClassCallExpression.ImplementStatement(elem.getValue().type == null ? null : elem.getValue().type.accept(this, null), elem.getValue().term == null ? null : elem.getValue().term.accept(this, null)));
    }
    return ClassCall(expr.getDefinition(), statements);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    Expression result = mySubstitution.get(expr.getBinding());
    return result != null ? result : expr;
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    LamExpression result = Lam(expr.getParameters().subst(mySubstitution), expr.getBody().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), mySubstitution);
    return result;
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    PiExpression result = Pi(expr.getParameters().subst(mySubstitution), expr.getCodomain().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), mySubstitution);
    return result;
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    SigmaExpression result = Sigma(expr.getParameters().subst(mySubstitution));
    DependentLink.Helper.freeSubsts(expr.getParameters(), mySubstitution);
    return result;
  }

  @Override
  public BranchElimTreeNode visitBranch(BranchElimTreeNode branchNode, Void params) {
    Binding newReference = ((ReferenceExpression) Reference(branchNode.getReference()).accept(this, null)).getBinding();
    List<Binding> newContextTail = new ArrayList<>(branchNode.getContextTail().size());
    for (Binding binding : branchNode.getContextTail()) {
      newContextTail.add(((ReferenceExpression) Reference(binding).accept(this, null)).getBinding());
    }

    BranchElimTreeNode newNode = new BranchElimTreeNode(newReference, newContextTail);
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      ConstructorClause newClause = newNode.addClause(clause.getConstructor());
      for (DependentLink linkOld = clause.getParameters(), linkNew = newClause.getParameters(); linkOld.hasNext(); linkOld = linkOld.getNext(), linkNew = linkNew.getNext()) {
        mySubstitution.add(linkOld, Reference(linkNew));
      }
      for (int i = 0; i < clause.getTailBindings().size(); i++) {
        mySubstitution.add(clause.getTailBindings().get(i), Reference(newClause.getTailBindings().get(i)));
      }

      newClause.setChild(clause.getChild().accept(this, null));

      mySubstitution.getDomain().removeAll(toContext(clause.getParameters()));
      mySubstitution.getDomain().removeAll(clause.getTailBindings());
    }

    if (branchNode.getOtherwiseClause() != null) {
      OtherwiseClause newClause = newNode.addOtherwiseClause();
      newClause.setChild(branchNode.getOtherwiseClause().getChild().accept(this, null));
    }

    return newNode;
  }

  @Override
  public LeafElimTreeNode visitLeaf(LeafElimTreeNode leafNode, Void params) {
    return new LeafElimTreeNode(leafNode.getArrow(), leafNode.getExpression().accept(this, null));
  }

  @Override
  public ElimTreeNode visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return emptyNode;
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, null), expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
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
    return New(expr.getExpression().accept(this, null));
  }

  @Override
  public LetExpression visitLet(LetExpression letExpression, Void params) {
    List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      LetClause newClause = visitLetClause(clause);
      clauses.add(newClause);
      mySubstitution.add(clause, Reference(newClause));
    }
    LetExpression result = Let(clauses, letExpression.getExpression().subst(mySubstitution));
    for (LetClause clause : letExpression.getClauses()) {
      mySubstitution.getDomain().remove(clause);
    }
    return result;
  }

  public LetClause visitLetClause(LetClause clause) {
    DependentLink parameters = clause.getParameters().subst(mySubstitution);
    Expression resultType = clause.getResultType() == null ? null : clause.getResultType().accept(this, null);
    ElimTreeNode elimTree = clause.getElimTree().accept(this, null);
    DependentLink.Helper.freeSubsts(clause.getParameters(), mySubstitution);
    return new LetClause(clause.getName(), parameters, resultType, elimTree);
  }
}
