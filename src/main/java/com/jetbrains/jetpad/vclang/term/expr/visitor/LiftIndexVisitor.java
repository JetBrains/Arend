package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.param.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.param.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.param.Utils.splitArguments;

public class LiftIndexVisitor extends BaseExpressionVisitor<Integer, Expression> implements ElimTreeNodeVisitor<Integer, ElimTreeNode> {
  private final int myOn;

  public LiftIndexVisitor(int on) {
    myOn = on;
  }

  @Override
  public Expression visitApp(AppExpression expr, Integer from) {
    Expression fun = expr.getFunction().accept(this, from);
    if (fun == null) return null;
    Expression arg = expr.getArgument().getExpression().accept(this, from);
    if (arg == null) return null;
    return Apps(fun, new ArgumentExpression(arg, expr.getArgument().isExplicit(), expr.getArgument().isHidden()));
  }

  @Override
  public DefCallExpression visitDefCall(DefCallExpression expr, Integer from) {
    return expr;
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Integer from) {
    if (expr.getParameters().isEmpty()) return expr;
    List<Expression> parameters = new ArrayList<>(expr.getParameters().size());
    for (Expression parameter : expr.getParameters()) {
      Expression expr2 = parameter.accept(this, from);
      if (expr2 == null) return null;
      parameters.add(expr2);
    }
    return ConCall(expr.getDefinition(), parameters);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Integer from) {
    Map<ClassField, ClassCallExpression.ImplementStatement> statements = new HashMap<>();
    for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
      statements.put(elem.getKey(), new ClassCallExpression.ImplementStatement(elem.getValue().type == null ? null : elem.getValue().type.accept(this, from), elem.getValue().term == null ? null : elem.getValue().term.accept(this, from)));
    }
    return ClassCall(expr.getDefinition(), statements);
  }

  @Override
  public IndexExpression visitIndex(IndexExpression expr, Integer from) {
    if (expr.getIndex() < from) return expr;
    if (expr.getIndex() + myOn >= from) return Index(expr.getIndex() + myOn);
    return null;
  }

  @Override
  public Expression visitLam(LamExpression expr, Integer from) {
    List<TelescopeArgument> arguments = new ArrayList<>(expr.getArguments().size());
    from = visitArguments(expr.getArguments(), arguments, from);
    if (from == -1) return null;
    Expression body = expr.getBody().accept(this, from);
    return body == null ? null : Lam(arguments, body);
  }

  private int visitArguments(List<TelescopeArgument> arguments, List<TelescopeArgument> result, int from) {
    for (TelescopeArgument argument : arguments) {
      Expression arg = argument.getType().accept(this, from);
      if (arg == null) return -1;
      result.add(Tele(argument.getExplicit(), argument.getNames(), arg));
      from += argument.getNames().size();
    }
    return from;
  }

  private int visitTypeArguments(List<TypeArgument> arguments, List<TypeArgument> result, int from) {
    for (TypeArgument argument : arguments) {
      if (argument instanceof TelescopeArgument) {
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        Expression arg = teleArgument.getType().accept(this, from);
        if (arg == null) return -1;
        result.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), arg));
        from += teleArgument.getNames().size();
      } else {
        Expression arg = argument.getType().accept(this, from);
        if (arg == null) return -1;
        result.add(new TypeArgument(argument.getExplicit(), arg));
        ++from;
      }
    }
    return from;
  }

  @Override
  public Expression visitPi(DependentExpression expr, Integer from) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    from = visitTypeArguments(expr.getArguments(), result, from);
    if (from == -1) return null;
    Expression codomain = expr.getCodomain().accept(this, from);
    return codomain == null ? null : Pi(result, codomain);
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Integer from) {
    return expr;
  }

  @Override
  public Expression visitError(ErrorExpression expr, Integer from) {
    if (expr.getExpr() == null) return expr;
    Expression expr1 = expr.accept(this, from);
    return expr1 == null ? null : new ErrorExpression(expr1, expr.getError());
  }

  @Override
  public Expression visitInferHole(InferHoleExpression expr, Integer from) {
    return expr;
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Integer from) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      Expression expr1 = field.accept(this, from);
      if (expr1 == null) return null;
      fields.add(expr1);
    }
    return Tuple(fields, (SigmaExpression) expr.getType().accept(this, from));
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Integer from) {
    List<TypeArgument> result = new ArrayList<>(expr.getArguments().size());
    return visitTypeArguments(expr.getArguments(), result, from) == -1 ? null : Sigma(result);
  }

  @Override
  public Expression visitProj(ProjExpression expr, Integer from) {
    Expression expr1 = expr.getExpression().accept(this, from);
    return expr1 == null ? null : Proj(expr1, expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Integer from) {
    return New(expr.getExpression().accept(this, from));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Integer from) {
    final List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      clauses.add(visitLetClause(clause, from));
      if (clauses.get(clauses.size() - 1) == null)
        return null;
      from++;
    }
    final Expression expr = letExpression.getExpression().accept(this, from);
    return expr == null ? null : Let(clauses, expr);
  }

  public LetClause visitLetClause(LetClause clause, Integer from) {
    final List<TypeArgument> arguments = new ArrayList<>(clause.getArguments().size());
    from = visitTypeArguments(clause.getArguments(), arguments, from);
    if (from == -1) return null;
    Expression resultType = null;
    if (clause.getResultType() != null) {
      resultType = clause.getResultType().accept(this, from);
      if (resultType == null) {
        return null;
      }
    }
    final ElimTreeNode elimTree = clause.getElimTree().accept(this, from);
    if (elimTree == null)
      return null;
    return new LetClause(clause.getName(), arguments, resultType, elimTree);
  }

  @Override
  public ElimTreeNode visitBranch(BranchElimTreeNode branchNode, Integer from) {
    IndexExpression newIndex = visitIndex(Index(branchNode.getIndex()), from);
    if (newIndex == null)
      return null;
    BranchElimTreeNode result = new BranchElimTreeNode(newIndex.getIndex());

    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      int liftShift = branchNode.getIndex() >= from ? 0
          : splitArguments(clause.getConstructor().getArguments()).size() - 1;
      ElimTreeNode node = clause.getChild().accept(this, from + liftShift);

      if (node == null)
        return null;
      result.addClause(clause.getConstructor(), node);
    }
    return result;
  }

  @Override
  public ElimTreeNode visitLeaf(LeafElimTreeNode leafNode, Integer from) {
    Expression expr = leafNode.getExpression().accept(this, from);
    if (expr == null)
      return null;
    return new LeafElimTreeNode(leafNode.getArrow(), expr);
  }

  @Override
  public ElimTreeNode visitEmpty(EmptyElimTreeNode emptyNode, Integer params) {
    return EmptyElimTreeNode.getInstance();
  }
}
