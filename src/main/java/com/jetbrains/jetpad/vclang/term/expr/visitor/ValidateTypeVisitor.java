package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class ValidateTypeVisitor extends BaseExpressionVisitor<Void, Void>
        implements ElimTreeNodeVisitor<Void, Void> {

  public static class ErrorReporter {
    private final ArrayList<Expression> expressions = new ArrayList<>();
    private final ArrayList<String> reasons = new ArrayList<>();

    public void addError(Expression expr, String reason) {
      expressions.add(expr);
      reasons.add(reason);
    }

    public ArrayList<Expression> getExpressions() {
      return expressions;
    }

    public ArrayList<String> getReasons() {
      return reasons;
    }

    public int errors() {
      return expressions.size();
    }
  }

  private final ErrorReporter myErrorReporter;

  public ValidateTypeVisitor(ErrorReporter errorReporter) {
    this.myErrorReporter = errorReporter;
  }

  private void visitDependentLink(DependentLink link) {
    if (!link.isExplicit()) {
      myErrorReporter.addError(link.getType(), "Explicit argument expected");
    }
    if (link.hasNext()) {
      visitDependentLink(link.getNext());
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    expr.getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitApp(AppExpression expr, Void params) {
    Expression fun = expr.getFunction();
    Expression funType = fun.getType();
    if (!(funType instanceof PiExpression)) {
      myErrorReporter.addError(expr, "Function " + fun + " doesn't have Pi-type");
    } else {
      ArrayList<DependentLink> links = new ArrayList<>();
      funType.getPiParameters(links, false, false);
      ArrayList<ArgumentExpression> args = new ArrayList<>();
      fun.getFunctionArgs(args);
      if (args.size() > links.size()) {
        myErrorReporter.addError(expr, "Too few Pi abstractions");
      }
      for (int i = 0; i < args.size(); i++) {
        if (!args.get(i).getExpression().getType().equals(links.get(i).getType())) {
          myErrorReporter.addError(args.get(i).getExpression(), "Expected type: " + links.get(i).getType());
        }
      }
    }

    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Void params) {
    expr.getType().accept(this, params);
    expr.getBinding().getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr, Void params) {
    visitDependentLink(expr.getParameters());
    expr.getType().accept(this, params);
    expr.getBody().accept(this, params);
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, Void params) {
    visitDependentLink(expr.getParameters());
    expr.getType().accept(this, params);
    expr.getCodomain().accept(this, params);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Void params) {
    visitDependentLink(expr.getParameters());
    expr.getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr, Void params) {
    myErrorReporter.addError(expr, expr.getError().toString());
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr, Void params) {
    SigmaExpression type = expr.getType();
    DependentLink link = type.getParameters();
    visitDependentLink(link);
    type.accept(this, params);
    for (Expression field : expr.getFields()) {
      field.accept(this, params);
      if (!field.getType().equals(link.getType())) {
        myErrorReporter.addError(field, "Expected type: " + link.getType() + ", found: " + field.getType());
      }
      if (!link.hasNext()) {
        myErrorReporter.addError(expr, "Too few abstractions in Sigma");
      }
      link = link.getNext();
    }
    return null;
  }

  @Override
  public Void visitProj(ProjExpression expr, Void params) {
    Expression type = expr.getType();
    type.accept(this, params);
    Expression tuple = expr.getExpression();
    tuple.accept(this, params);
    if (!(tuple instanceof TupleExpression)) {
      myErrorReporter.addError(tuple, "Tuple expected");
    } else {
      List<Expression> fields = ((TupleExpression)tuple).getFields();
      if (fields.size() <= expr.getField()) {
        myErrorReporter.addError(tuple, "Too few fields (at least " + (expr.getField() + 1) + " expected)");
      }
    }
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, Void params) {
    expr.getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitLet(LetExpression letExpression, Void params) {
    for (LetClause clause : letExpression.getClauses()) {
      clause.getResultType().accept(this, params);
    }
    letExpression.getType().accept(this, params);
    return null;
  }

  @Override
  public Void visitBranch(BranchElimTreeNode branchNode, Void params) {
    return null;
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode leafNode, Void params) {
    leafNode.getExpression().accept(this, params);
    return null;
  }

  @Override
  public Void visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return null;
  }

}
