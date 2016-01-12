package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.param.Binding;
import com.jetbrains.jetpad.vclang.term.expr.param.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class SubstVisitor extends BaseExpressionVisitor<Void, Expression> implements ElimTreeNodeVisitor<Void, ElimTreeNode> {
  private final Map<Binding, Expression> mySubstExprs;

  public SubstVisitor(Map<Binding, Expression> substExprs) {
    mySubstExprs = substExprs;
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
    if (expr.getParameters().isEmpty()) return expr;
    List<Expression> parameters = new ArrayList<>(expr.getParameters().size());
    for (Expression parameter : expr.getParameters()) {
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
    Expression result = mySubstExprs.get(expr.getBinding());
    return result != null ? result : expr;
  }

  @Override
  public Expression visitTypedDependent(TypedDependentExpression expr, Void params) {
    TypedDependentExpression result = expr.newInstance(expr.isExplicit(), expr.getName(), expr.getLeft().accept(this, null));
    mySubstExprs.put(expr, Reference(result));
    result.setRight(expr.getRight().accept(this, null));
    mySubstExprs.remove(expr);
    return result;
  }

  @Override
  public Expression visitUntypedDependent(UntypedDependentExpression expr, Void params) {
    UntypedDependentExpression result = expr.newInstance(expr.getName());
    mySubstExprs.put(expr, Reference(result));
    result.setRight((DependentExpression) expr.getRight().accept(this, null));
    mySubstExprs.remove(expr);
    return result;
  }

  @Override
  public Expression visitNonDependent(NonDependentExpression expr, Void params) {
    return expr.newInstance(expr.getLeft().accept(this, null), expr.getRight().accept(this, null));
  }

  @Override
  public ElimTreeNode visitBranch(BranchElimTreeNode branchNode, Void params) {
    assert !mySubstExprs.containsKey(branchNode.getReference());
    BranchElimTreeNode newNode = new BranchElimTreeNode(branchNode.getReference());
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      newNode.addClause(clause.getConstructor(), clause.getChild().accept(this, null));
    }
    return newNode;
  }

  @Override
  public ElimTreeNode visitLeaf(LeafElimTreeNode leafNode, Void params) {
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
    return Tuple(fields, (DependentExpression) expr.getType().accept(this, null));
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
      mySubstExprs.put(clause, Reference(newClause));
    }
    LetExpression result = Let(clauses, letExpression.getExpression().subst(mySubstExprs));
    for (LetClause clause : letExpression.getClauses()) {
      mySubstExprs.remove(clause);
    }
    return result;
  }

  public LetClause visitLetClause(LetClause clause) {
    List<TypeArgument> arguments = new ArrayList<>(clause.getArguments().size());
    for (TypeArgument argument : clause.getArguments()) {
      arguments.add(argument.newInstance(argument.getType().accept(this, null)));
    }
    Expression resultType = clause.getResultType() == null ? null : clause.getResultType().accept(this, null);
    ElimTreeNode elimTree = clause.getElimTree().accept(this, null);
    return new LetClause(clause.getName(), arguments, resultType, elimTree);
  }
}
