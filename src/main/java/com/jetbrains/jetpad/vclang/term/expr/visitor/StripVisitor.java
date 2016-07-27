package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class StripVisitor implements ExpressionVisitor<Void, Expression>, ElimTreeNodeVisitor<Void, ElimTreeNode> {
  @Override
  public AppExpression visitApp(AppExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getArguments().size());
    for (Expression arg : expr.getArguments()) {
      args.add(arg.accept(this, null));
    }
    return new AppExpression(expr.getFunction().accept(this, null), args, expr.getFlags());
  }

  @Override
  public FunCallExpression visitFunCall(FunCallExpression expr, Void params) {
    return expr;
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression arg : expr.getDataTypeArguments()) {
      args.add(arg.accept(this, null));
    }
    return new ConCallExpression(expr.getDefinition(), args);
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Void params) {
    return expr;
  }

  @Override
  public FieldCallExpression visitFieldCall(FieldCallExpression expr, Void params) {
    return expr;
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    return expr.applyVisitorToImplementedHere(this, params);
  }

  @Override
  public ReferenceExpression visitReference(ReferenceExpression expr, Void params) {
    return expr;
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    Substitution substitution = new Substitution();
    DependentLink link = DependentLink.Helper.accept(expr.getParameters(), substitution, this, null);
    return new LamExpression(link, expr.getBody().accept(this, null).subst(substitution));
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    Substitution substitution = new Substitution();
    DependentLink link = DependentLink.Helper.accept(expr.getParameters(), substitution, this, null);
    return new PiExpression(link, expr.getCodomain().accept(this, null).subst(substitution));
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    return new SigmaExpression(DependentLink.Helper.accept(expr.getParameters(), this, null));
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public ErrorExpression visitError(ErrorExpression expr, Void params) {
    return new ErrorExpression(expr.getExpr() == null ? null : expr.getExpr().accept(this, null), expr.getError());
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return new TupleExpression(fields, visitSigma(expr.getType(), null));
  }

  @Override
  public ProjExpression visitProj(ProjExpression expr, Void params) {
    return new ProjExpression(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public NewExpression visitNew(NewExpression expr, Void params) {
    return new NewExpression(expr.getExpression().accept(this, null));
  }

  @Override
  public LetExpression visitLet(LetExpression expr, Void params) {
    Substitution substitution = new Substitution();
    List<LetClause> clauses = new ArrayList<>(expr.getClauses().size());
    for (LetClause clause : expr.getClauses()) {
      DependentLink link = DependentLink.Helper.accept(clause.getParameters(), substitution, this, null);
      LetClause clause1 = new LetClause(clause.getName(), link, clause.getResultType() == null ? null : clause.getResultType().accept(this, null).subst(substitution), clause.getElimTree().accept(this, null).subst(substitution));
      clauses.add(clause1);
      substitution.add(clause, new ReferenceExpression(clause1));
    }
    return new LetExpression(clauses, expr.getExpression().accept(this, null).subst(substitution));
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public Expression visitLevel(LevelExpression expr, Void params) {
    return expr;
  }

  @Override
  public BranchElimTreeNode visitBranch(BranchElimTreeNode branchNode, Void params) {
    BranchElimTreeNode result = new BranchElimTreeNode(branchNode.getReference(), branchNode.getContextTail());
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      ConstructorClause clause1 = result.addClause(clause.getConstructor(), DependentLink.Helper.toNames(clause.getParameters()));
      Substitution substitution = new Substitution();
      for (DependentLink linkOld = clause.getParameters(), linkNew = clause1.getParameters(); linkOld.hasNext(); linkOld = linkOld.getNext(), linkNew = linkNew.getNext()) {
        substitution.add(linkOld, new ReferenceExpression(linkNew));
      }
      for (int i = 0; i < clause.getTailBindings().size(); i++) {
        substitution.add(clause.getTailBindings().get(i), new ReferenceExpression(clause1.getTailBindings().get(i)));
      }
      clause1.setChild(clause.getChild().accept(this, null).subst(substitution));
    }
    if (branchNode.getOtherwiseClause() != null) {
      result.addOtherwiseClause().setChild(branchNode.getOtherwiseClause().getChild().accept(this, null));
    }
    return result;
  }

  @Override
  public LeafElimTreeNode visitLeaf(LeafElimTreeNode leafNode, Void params) {
    LeafElimTreeNode result = new LeafElimTreeNode(leafNode.getArrow(), leafNode.getExpression().accept(this, null));
    if (leafNode.getMatched() != null) {
      result.setMatched(new ArrayList<>(leafNode.getMatched()));
    }
    return result;
  }

  @Override
  public EmptyElimTreeNode visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return emptyNode;
  }
}
