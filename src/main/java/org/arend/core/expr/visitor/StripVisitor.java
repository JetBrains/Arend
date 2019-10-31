package org.arend.core.expr.visitor;

import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.error.ErrorReporter;
import org.arend.typechecking.error.local.LocalError;

import java.util.*;

public class StripVisitor implements ExpressionVisitor<Void, Expression> {
  private final Set<EvaluatingBinding> myBoundEvaluatingBindings = new HashSet<>();
  private final ErrorReporter myErrorReporter;

  public StripVisitor(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    return AppExpression.make(expr.getFunction().accept(this, null), expr.getArgument().accept(this, null));
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
  public Expression visitConCall(ConCallExpression expr, Void params) {
    List<Expression> dataTypeArgs = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression arg : expr.getDataTypeArguments()) {
      dataTypeArgs.add(arg.accept(this, null));
    }

    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }

    return ConCallExpression.make(expr.getDefinition(), expr.getSortArgument(), dataTypeArgs, args);
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
    NewExpression newExpr = expr.getArgument().cast(NewExpression.class);
    if (newExpr != null) {
      return newExpr.getExpression().getImplementation(expr.getDefinition(), expr.getArgument()).accept(this, null);
    } else {
      return FieldCallExpression.make(expr.getDefinition(), expr.getSortArgument(), expr.getArgument().accept(this, null));
    }
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      entry.setValue(entry.getValue().accept(this, null));
    }
    return expr;
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    if (expr.getBinding() instanceof EvaluatingBinding && !myBoundEvaluatingBindings.contains(expr.getBinding())) {
      return ((EvaluatingBinding) expr.getBinding()).getExpression().accept(this, null);
    }
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() == null) {
      LocalError error = expr.getVariable().getErrorInfer();
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
  public Expression visitProj(ProjExpression expr, Void params) {
    return ProjExpression.make(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public NewExpression visitNew(NewExpression expr, Void params) {
    return new NewExpression(visitClassCall(expr.getExpression(), null));
  }

  @Override
  public LetExpression visitLet(LetExpression expr, Void params) {
    for (LetClause clause : expr.getClauses()) {
      clause.setExpression(clause.getExpression().accept(this, null));
      myBoundEvaluatingBindings.add(clause);
    }

    LetExpression result = new LetExpression(expr.isStrict(), expr.getClauses(), expr.getExpression().accept(this, null));
    myBoundEvaluatingBindings.removeAll(expr.getClauses());
    return result;
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    ElimTree elimTree = stripElimTree(expr.getElimTree());
    for (int i = 0; i < expr.getArguments().size(); i++) {
      expr.getArguments().set(i, expr.getArguments().get(i).accept(this, null));
    }
    visitParameters(expr.getParameters());
    return new CaseExpression(expr.isSFunc(), expr.getParameters(), expr.getResultType().accept(this, null), expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(this, null), elimTree, expr.getArguments());
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

  @Override
  public IntegerExpression visitInteger(IntegerExpression expr, Void params) {
    return expr;
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    return new PEvalExpression(expr.getExpression().accept(this, null));
  }
}
