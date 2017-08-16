package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckableProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CollectDefCallsVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final InstanceProvider myInstanceProvider;
  private final TypecheckableProvider myTypecheckableProvider;
  private final Set<Abstract.GlobalReferableSourceNode> myDependencies;

  public CollectDefCallsVisitor(InstanceProvider instanceProvider, TypecheckableProvider typecheckableProvider, Set<Abstract.GlobalReferableSourceNode> dependencies) {
    myInstanceProvider = instanceProvider;
    myTypecheckableProvider = typecheckableProvider;
    myDependencies = dependencies;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void ignore) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  private void addDependencies(Abstract.ReferableSourceNode referable) {
    if (!(referable instanceof Abstract.GlobalReferableSourceNode)) {
      return;
    }

    Abstract.Definition definition = myTypecheckableProvider.forReferable((Abstract.GlobalReferableSourceNode) referable);
    if (myInstanceProvider != null) {
      if (definition instanceof Abstract.ClassViewField) {
        myDependencies.addAll(myInstanceProvider.getInstances(((Abstract.ClassViewField) definition).getOwnView()));
      } else {
        Collection<? extends Abstract.Parameter> parameters = Abstract.getParameters(definition);
        if (parameters != null) {
          if (definition instanceof Abstract.Constructor) {
            List<? extends Abstract.TypeParameter> dataTypeParameters = ((Abstract.Constructor) definition).getDataType().getParameters();
            List<Abstract.Parameter> totalParameters = new ArrayList<>(dataTypeParameters.size() + parameters.size());
            totalParameters.addAll(dataTypeParameters);
            totalParameters.addAll(parameters);
            parameters = totalParameters;
          }
          for (Abstract.Parameter parameter : parameters) {
            Abstract.ClassView classView = Abstract.getUnderlyingClassView(((Abstract.TypeParameter) parameter).getType());
            if (classView != null) {
              myDependencies.addAll(myInstanceProvider.getInstances(classView));
            }
          }
        }
      }
    }
    myDependencies.add(definition);
  }

  @Override
  public Void visitReference(Abstract.ReferenceExpression expr, Void ignore) {
    addDependencies(expr.getReferent());
    if (expr.getExpression() != null) {
      expr.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Abstract.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    if (expr.getModule() != null)
      myDependencies.add(expr.getModule());
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    expr.getBody().accept(this, null);
    return null;
  }

  private void visitParameters(List<? extends Abstract.Parameter> args) {
    for (Abstract.Parameter arg : args) {
      if (arg instanceof Abstract.TypeParameter) {
        ((Abstract.TypeParameter) arg).getType().accept(this, null);
      }
    }
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitGoal(Abstract.GoalExpression expr, Void ignore) {
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Void ignore) {
    for (Abstract.Expression comp : expr.getFields()) {
      comp.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Void ignore) {
    visitParameters(expr.getParameters());
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Void ignore) {
    addDependencies(expr.getReferent());
    expr.getLeft().accept(this, null);
    if (expr.getRight() != null) {
      expr.getRight().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void ignore) {
    expr.getLeft().accept(this, null);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      visitReference(elem.binOp, null);
      if (elem.argument != null) {
        elem.argument.accept(this, null);
      }
    }
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void ignore) {
    for (Abstract.Expression caseExpr : expr.getExpressions()) {
      caseExpr.accept(this, null);
    }
    for (Abstract.FunctionClause clause : expr.getClauses()) {
      if (clause.getExpression() != null)
        clause.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Void ignore) {
    expr.getBaseClassExpression().accept(this, null);
    for (Abstract.ClassFieldImpl statement : expr.getStatements()) {
      statement.getImplementation().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Void ignore) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Abstract.LetExpression letExpression, Void ignore) {
    for (Abstract.LetClause clause : letExpression.getClauses()) {
      visitParameters(clause.getParameters());
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, null);
      }
      clause.getTerm().accept(this, null);
    }
    letExpression.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Abstract.NumericLiteral expr, Void ignore) {
    return null;
  }
}
