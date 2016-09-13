package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class StripVisitor implements ExpressionVisitor<Void, Expression>, ElimTreeNodeVisitor<Void, ElimTreeNode> {
  private final Set<Binding> myBounds;
  private final ErrorReporter myErrorReporter;
  private final Stack<InferenceReferenceExpression> myVariables;

  public StripVisitor(Set<Binding> bounds, ErrorReporter errorReporter) {
    myBounds = bounds;
    myErrorReporter = errorReporter;
    myVariables = new Stack<>();
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    Expression fun = expr.getFunction().accept(this, null);
    List<? extends Expression> args = expr.getArguments();
    if (fun.toFieldCall() != null && expr.getArguments().get(0).toNew() != null) {
      fun = expr.getArguments().get(0).toNew().getExpression().getFieldSet().getImplementation(fun.toFieldCall().getDefinition()).term.accept(this, null);
      args = args.subList(1, args.size());
    }
    if (args.isEmpty()) {
      return fun;
    }

    List<Expression> newArgs = new ArrayList<>(args.size());
    for (Expression arg : args) {
      newArgs.add(arg.accept(this, null));
    }
    return new AppExpression(fun, newArgs);
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
    return new ConCallExpression(expr.getDefinition(), args).applyLevelSubst(expr.getPolyParamsSubst());
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
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (myBounds.contains(expr.getBinding())) {
      return expr;
    }

    TypeCheckingError error = myVariables.empty() ? new TypeCheckingError("Cannot infer some expressions", null) : myVariables.peek().getOriginalVariable().getErrorInfer(myVariables.peek().getSubstExpression());
    myErrorReporter.report(error);
    return new ErrorExpression(expr, error);
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getVariable() != null) {
      TypeCheckingError error = expr.getVariable().getErrorInfer();
      myErrorReporter.report(error);
      Expression result = new ErrorExpression(null, error);
      expr.setSubstExpression(result);
      return result;
    } else {
      myVariables.push(expr);
      Expression result = expr.getSubstExpression().accept(this, null);
      myVariables.pop();
      return result;
    }
  }

  private void visitArguments(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      link1.setType(link1.getType().accept(this, null));

      for (; link != link1; link = link.getNext()) {
        myBounds.add(link);
      }
      myBounds.add(link);
    }
  }

  private void freeArguments(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      myBounds.remove(link);
    }
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    visitArguments(expr.getParameters());
    LamExpression result = new LamExpression(expr.getParameters(), expr.getBody().accept(this, null));
    freeArguments(expr.getParameters());
    return result;
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    visitArguments(expr.getParameters());
    PiExpression result = new PiExpression(expr.getParameters(), expr.getCodomain().accept(this, null));
    freeArguments(expr.getParameters());
    return result;
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    visitArguments(expr.getParameters());
    freeArguments(expr.getParameters());
    return expr;
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
    return new NewExpression(visitClassCall(expr.getExpression(), null));
  }

  @Override
  public LetExpression visitLet(LetExpression expr, Void params) {
    for (LetClause clause : expr.getClauses()) {
      visitArguments(clause.getParameters());
      if (clause.getResultType() != null) {
        clause.setResultType(clause.getResultType().accept(this, null));
      }
      clause.setElimTree(clause.getElimTree().accept(this, null));
      freeArguments(clause.getParameters());
      myBounds.add(clause);
    }

    LetExpression result = new LetExpression(expr.getClauses(), expr.getExpression().accept(this, null));
    for (LetClause clause : expr.getClauses()) {
      myBounds.remove(clause);
    }
    return result;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }

  @Override
  public BranchElimTreeNode visitBranch(BranchElimTreeNode branchNode, Void params) {
    myBounds.remove(branchNode.getReference());
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext()) {
        myBounds.add(link);
      }
      for (Binding binding : clause.getTailBindings()) {
        myBounds.add(binding);
      }

      clause.setChild(clause.getChild().accept(this, null));

      for (DependentLink link = clause.getParameters(); link.hasNext(); link = link.getNext()) {
        myBounds.remove(link);
      }
      for (Binding binding : clause.getTailBindings()) {
        myBounds.remove(binding);
      }
    }

    myBounds.add(branchNode.getReference());
    if (branchNode.getOtherwiseClause() != null) {
      branchNode.getOtherwiseClause().setChild(branchNode.getOtherwiseClause().getChild().accept(this, null));
    }
    return branchNode;
  }

  @Override
  public LeafElimTreeNode visitLeaf(LeafElimTreeNode leafNode, Void params) {
    leafNode.setExpression(leafNode.getExpression().accept(this, null));
    return leafNode;
  }

  @Override
  public EmptyElimTreeNode visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return emptyNode;
  }
}
