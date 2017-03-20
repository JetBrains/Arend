package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.*;

public class StripVisitor implements ExpressionVisitor<Void, Expression>, ElimTreeNodeVisitor<Void, ElimTreeNode> {
  private final Set<Binding> myBounds;
  private final LocalErrorReporter myErrorReporter;
  private final Stack<InferenceReferenceExpression> myVariables;

  public StripVisitor(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    myBounds = bounds;
    myErrorReporter = errorReporter;
    myVariables = new Stack<>();
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getArguments().size());
    for (Expression arg : expr.getArguments()) {
      args.add(arg.accept(this, null));
    }
    return new AppExpression(expr.getFunction().accept(this, null), args);
  }

  @Override
  public FunCallExpression visitFunCall(FunCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }
    return new FunCallExpression(expr.getDefinition(), expr.getSortArgument(), args);
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Void params) {
    List<Expression> dataTypeArgs = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression arg : expr.getDataTypeArguments()) {
      dataTypeArgs.add(arg.accept(this, null));
    }

    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }

    return new ConCallExpression(expr.getDefinition(), expr.getSortArgument(), dataTypeArgs, args);
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }
    return new DataCallExpression(expr.getDefinition(), expr.getSortArgument(), args);
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    if (expr.getExpression().toNew() != null) {
      return expr.getExpression().toNew().getExpression().getFieldSet().getImplementation(expr.getDefinition()).term.accept(this, null);
    } else {
      return ExpressionFactory.FieldCall(expr.getDefinition(), expr.getExpression().accept(this, null));
    }
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    FieldSet fieldSet = FieldSet.applyVisitorToImplemented(expr.getFieldSet(), expr.getDefinition().getFieldSet(), this, null);
    ClassCallExpression classCall = new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), fieldSet);
    fieldSet.updateSorts(classCall);
    return classCall;
  }

  private Expression cannotInferError(Expression expr) {
    LocalTypeCheckingError error = myVariables.empty() ? new LocalTypeCheckingError("Cannot infer some expressions", null) : myVariables.peek().getOriginalVariable().getErrorInfer(myVariables.peek().getSubstExpression());
    myErrorReporter.report(error);
    return new ErrorExpression(expr, error);
  }

  @Override
  public Expression visitLetClauseCall(LetClauseCallExpression expr, Void params) {
    if (myBounds.contains(expr.getLetClause())) {
      List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
      for (Expression arg : expr.getDefCallArguments()) {
        args.add(arg.accept(this, null));
      }
      return new LetClauseCallExpression(expr.getLetClause(), args);
    } else {
      return cannotInferError(expr);
    }
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (myBounds.contains(expr.getBinding())) {
      return expr;
    } else {
      return cannotInferError(expr);
    }
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getVariable() != null) {
      LocalTypeCheckingError error = expr.getVariable().getErrorInfer();
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
    PiExpression result = new PiExpression(expr.getPLevel(), expr.getParameters(), expr.getCodomain().accept(this, null));
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
      clause.getParameters().forEach(this::visitArguments);
      if (clause.getResultType() != null) {
        clause.setResultType(clause.getResultType().accept(this, null));
      }
      clause.setElimTree(clause.getElimTree().accept(this, null));
      clause.getParameters().forEach(this::freeArguments);
      myBounds.add(clause);
    }

    LetExpression result = new LetExpression(expr.getClauses(), expr.getExpression().accept(this, null));
    expr.getClauses().forEach(myBounds::remove);
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
      visitArguments(clause.getParameters());
      for (TypedBinding binding : clause.getTailBindings()) {
        binding.setType(binding.getType().strip(myBounds, myErrorReporter));
        myBounds.add(binding);
      }

      clause.setChild(clause.getChild().accept(this, null));

      freeArguments(clause.getParameters());
      clause.getTailBindings().forEach(myBounds::remove);
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
