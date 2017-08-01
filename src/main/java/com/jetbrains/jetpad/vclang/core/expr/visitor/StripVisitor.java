package com.jetbrains.jetpad.vclang.core.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StripVisitor implements ExpressionVisitor<Void, Expression> {
  private final LocalErrorReporter myErrorReporter;

  public StripVisitor(LocalErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public AppExpression visitApp(AppExpression expr, Void params) {
    return new AppExpression(expr.getFunction().accept(this, null), expr.getArgument().accept(this, null));
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
    if (expr.getExpression().isInstance(NewExpression.class)) {
      return expr.getExpression().cast(NewExpression.class).getExpression().getFieldSet().getImplementation(expr.getDefinition()).term.accept(this, null);
    } else {
      return ExpressionFactory.FieldCall(expr.getDefinition(), expr.getExpression().accept(this, null));
    }
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    FieldSet fieldSet = FieldSet.applyVisitorToImplemented(expr.getFieldSet(), expr.getDefinition().getFieldSet(), this, null);
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument(), fieldSet);
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() == null) {
      LocalTypeCheckingError error = expr.getVariable().getErrorInfer();
      myErrorReporter.report(error);
      Expression result = new ErrorExpression(null, error);
      expr.setSubstExpression(result);
      return result;
    } else {
      return expr.getSubstExpression().accept(this, null);
    }
  }

  private void visitParameters(DependentLink link) {
    for (; link.hasNext(); link = link.getNext()) {
      DependentLink link1 = link.getNextTyped(null);
      link1.setType(link1.getType().strip(myErrorReporter));
    }
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return new LamExpression(expr.getResultSort(), expr.getParameters(), expr.getBody().accept(this, null));
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return new PiExpression(expr.getResultSort(), expr.getParameters(), expr.getCodomain().accept(this, null));
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return expr;
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr, Void params) {
    return expr;
  }

  @Override
  public ErrorExpression visitError(ErrorExpression expr, Void params) {
    return new ErrorExpression(expr.getExpression() == null ? null : expr.getExpression().accept(this, null), expr.getError());
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return new TupleExpression(fields, visitSigma(expr.getSigmaType(), null));
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
      clause.setExpression(clause.getExpression().accept(this, null));
    }

    return new LetExpression(expr.getClauses(), expr.getExpression().accept(this, null));
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    ElimTree elimTree = stripElimTree(expr.getElimTree());
    for (int i = 0; i < expr.getArguments().size(); i++) {
      expr.getArguments().set(i, expr.getArguments().get(i).accept(this, null));
    }
    visitParameters(expr.getParameters());
    return new CaseExpression(expr.getParameters(), expr.getResultType().accept(this, null), elimTree, expr.getArguments());
  }

  private ElimTree stripElimTree(ElimTree elimTree) {
    visitParameters(elimTree.getParameters());
    if (elimTree instanceof LeafElimTree) {
      return new LeafElimTree(elimTree.getParameters(), ((LeafElimTree) elimTree).getExpression().accept(this, null));
    } else {
      Map<Constructor, ElimTree> children = new HashMap<>();
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        children.put(entry.getKey(), stripElimTree(entry.getValue()));
      }
      return new BranchElimTree(elimTree.getParameters(), children);
    }
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return expr.getExpression().accept(this, null);
  }
}
