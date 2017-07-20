package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.ClassViewInstanceProvider;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CollectDefCallsVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final ClassViewInstanceProvider myInstanceProvider;
  private final Set<Abstract.Definition> myDependencies;

  public CollectDefCallsVisitor(ClassViewInstanceProvider instanceProvider, Set<Abstract.Definition> dependencies) {
    myInstanceProvider = instanceProvider;
    myDependencies = dependencies;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void ignore) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitReference(Abstract.ReferenceExpression expr, Void ignore) {
    if (expr.getReferent() instanceof Abstract.Definition) {
      if (myInstanceProvider != null) {
        if (expr.getReferent() instanceof Abstract.ClassViewField) {
          myDependencies.addAll(myInstanceProvider.getInstances(expr, 0));
        } else {
          Collection<? extends Abstract.Parameter> arguments = Abstract.getParameters((Abstract.Definition) expr.getReferent());
          if (arguments != null) {
            int i = 0;
            for (Abstract.Parameter arg : arguments) {
              myDependencies.addAll(myInstanceProvider.getInstances(expr, i));
              if (arg instanceof Abstract.TelescopeParameter) {
                i += ((Abstract.TelescopeParameter) arg).getReferableList().size();
              } else {
                i++;
              }
            }
          }
        }
      }
      myDependencies.add((Abstract.Definition) expr.getReferent());
    }
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
  public Void visitError(Abstract.ErrorExpression expr, Void ignore) {
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
    myDependencies.add(expr.getReferent());
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void ignore) {
    expr.getLeft().accept(this, null);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      visitReference(elem.binOp, null);
      elem.argument.accept(this, null);
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
