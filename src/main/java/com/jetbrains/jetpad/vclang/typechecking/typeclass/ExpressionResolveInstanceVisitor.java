package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleClassViewInstanceProvider;

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
  public Void visitDefCall(Abstract.DefCallExpression expr, Void params) {
    Abstract.Expression expression = expr.getExpression();
    if (expression != null) {
      expression.accept(this, null);
    }

    Abstract.Definition definition = expr.getReferent();
    if (definition instanceof Abstract.FunctionDefinition) {
      checkArguments(expr, ((Abstract.FunctionDefinition) definition).getArguments());
    } else
    if (definition instanceof Abstract.DataDefinition) {
      checkArguments(expr, ((Abstract.DataDefinition) definition).getParameters());
    } else
    if (definition instanceof Abstract.Constructor) {
      checkArguments(expr, ((Abstract.Constructor) definition).getArguments());
    }
    return null;
  }

  private void checkArguments(Abstract.DefCallExpression defCall, Collection<? extends Abstract.Argument> args) {
    int i = 0;
    for (Abstract.Argument arg : args) {
      if (arg instanceof Abstract.NameArgument) {
        i++;
      } else
      if (arg instanceof Abstract.TypeArgument) {
        int size = i + (arg instanceof Abstract.TelescopeArgument ? ((Abstract.TelescopeArgument) arg).getNames().size() : 1);
        Abstract.Expression type = ((Abstract.TypeArgument) arg).getType();
        // TODO: check that type is a class view call and filter instances appropriately
        Collection<? extends Abstract.ClassViewInstance> instances = myParentScope.getInstances();
        for (; i < size; i++) {
          myInstanceProvider.addInstances(defCall, i, instances);
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

  public void visitArguments(List<? extends Abstract.Argument> arguments) {
    for (Abstract.Argument argument : arguments) {
      if (argument instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) argument).getType().accept(this, null);
      }
    }
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void params) {
    visitArguments(expr.getArguments());
    expr.getBody().accept(this, null);
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void params) {
    visitArguments(expr.getArguments());
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitLvl(Abstract.LvlExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitPolyUniverse(Abstract.PolyUniverseExpression expr, Void params) {
    if (expr.getPLevel() != null) {
      for (Abstract.Expression maxArg : expr.getPLevel()) {
        maxArg.accept(this, null);
      }
    }
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
    visitArguments(expr.getArguments());
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
      visitDefCall(elem.binOp, null);
      elem.argument.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Void params) {
    visitElimCase(expr);
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void params) {
    visitElimCase(expr);
    return null;
  }

  private void visitElimCase(Abstract.ElimCaseExpression expr) {
    for (Abstract.Expression expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    for (Abstract.Clause clause : expr.getClauses()) {
      if (clause.getExpression() != null) {
        clause.getExpression().accept(this, null);
      }
    }
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
      visitArguments(clause.getArguments());
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
