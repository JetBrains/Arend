package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleClassViewInstanceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExpressionResolveInstanceVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final Scope myParentScope;
  private final SimpleClassViewInstanceProvider myInstanceProvider;

  public ExpressionResolveInstanceVisitor(Scope parentScope, SimpleClassViewInstanceProvider instanceProvider) {
    myParentScope = parentScope;
    myInstanceProvider = instanceProvider;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitReference(Abstract.ReferenceExpression expr, Void params) {
    Abstract.Expression expression = expr.getExpression();
    if (expression != null) {
      expression.accept(this, null);
    }

    Abstract.ReferableSourceNode definition = expr.getReferent();
    if (definition instanceof Abstract.ClassViewField) {
      Abstract.ClassView classView = ((Abstract.ClassViewField) definition).getOwnView();
      Collection<? extends Abstract.ClassViewInstance> instances = myParentScope.getInstances();
      List<Abstract.ClassViewInstance> filteredInstances = new ArrayList<>();
      for (Abstract.ClassViewInstance instance : instances) {
        if (instance.getClassView().getReferent() == classView) {
          filteredInstances.add(instance);
        }
      }
      myInstanceProvider.addInstances(expr, 0, filteredInstances);
    } else if (definition instanceof Abstract.Definition) {
      Collection<? extends Abstract.Parameter> arguments = Abstract.getParameters((Abstract.Definition) definition);
      if (arguments != null) {
        checkParameters(expr, arguments);
      }
    }
    return null;
  }

  @Override
  public Void visitInferenceReference(Abstract.InferenceReferenceExpression expr, Void params) {
    return null;
  }

  private void checkParameters(Abstract.ReferenceExpression defCall, Collection<? extends Abstract.Parameter> parameters) {
    int i = 0;
    for (Abstract.Parameter parameter : parameters) {
      if (parameter instanceof Abstract.NameParameter) {
        i++;
      } else
      if (parameter instanceof Abstract.TypeParameter) {
        int size = i + (parameter instanceof Abstract.TelescopeParameter ? ((Abstract.TelescopeParameter) parameter).getReferableList().size() : 1);
        Abstract.ClassView classView = Abstract.getUnderlyingClassView(((Abstract.TypeParameter) parameter).getType());
        if (classView != null) {
          Collection<? extends Abstract.ClassViewInstance> instances = myParentScope.getInstances();
          List<Abstract.ClassViewInstance> filteredInstances = new ArrayList<>();
          for (Abstract.ClassViewInstance instance : instances) {
            if (instance.isDefault() && ((Abstract.ClassView) instance.getClassView().getReferent()).getUnderlyingClassReference().getReferent() == classView.getUnderlyingClassReference().getReferent()) {
              filteredInstances.add(instance);
            }
          }

          for (; i < size; i++) {
            myInstanceProvider.addInstances(defCall, i, filteredInstances);
          }
        } else {
          i += size;
        }
      } else {
        throw new IllegalStateException();
      }
    }
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    return null;
  }

  public void visitParameters(List<? extends Abstract.Parameter> parameters) {
    for (Abstract.Parameter parameter : parameters) {
      if (parameter instanceof Abstract.TypeParameter) {
        ((Abstract.TypeParameter) parameter).getType().accept(this, null);
      }
    }
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void params) {
    visitParameters(expr.getParameters());
    expr.getBody().accept(this, null);
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void params) {
    visitParameters(expr.getParameters());
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Void params) {
    Abstract.Expression expression = expr.getExpr();
    if (expression != null) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Void params) {
    for (Abstract.Expression expression : expr.getFields()) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Void params) {
    visitParameters(expr.getParameters());
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Void params) {
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void params) {
    expr.getLeft().accept(this, null);
    for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
      visitReference(elem.binOp, null);
      elem.argument.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void params) {
    for (Abstract.Expression expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    for (Abstract.FunctionClause clause : expr.getClauses()) {
      if (clause.getExpression() != null) {
        clause.getExpression().accept(this, null);
      }
    }
    return null;
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Void params) {
    expr.getBaseClassExpression().accept(this, null);
    for (Abstract.ClassFieldImpl impl : expr.getStatements()) {
      impl.getImplementation().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Abstract.LetExpression expr, Void params) {
    for (Abstract.LetClause clause : expr.getClauses()) {
      visitParameters(clause.getParameters());
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, null);
      }
      clause.getTerm().accept(this, null);
    }

    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Abstract.NumericLiteral expr, Void params) {
    return null;
  }
}
