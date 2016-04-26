package com.jetbrains.jetpad.vclang.module.utils;

import com.jetbrains.jetpad.vclang.module.FileModuleID;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;

public class LoadModulesRecursively implements AbstractStatementVisitor<Void, Void>, AbstractDefinitionVisitor<Void, Void>, AbstractExpressionVisitor<Void, Void> {
  private final ModuleLoader myModuleLoader;

  public LoadModulesRecursively(ModuleLoader myModuleLoader) {
    this.myModuleLoader = myModuleLoader;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Void params) {
    if (expr.getExpression() != null) expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitModuleCall(Abstract.ModuleCallExpression expr, Void params) {
    myModuleLoader.load(new FileModuleID(new ModulePath(expr.getPath())));
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void params) {
    for (Abstract.Argument argument : expr.getArguments()) {
      visitArgument(argument);
    }
    expr.getBody().accept(this, null);
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void params) {
    for (Abstract.Argument argument : expr.getArguments()) {
      if (argument instanceof Abstract.TelescopeArgument) {
        ((Abstract.TelescopeArgument) argument).getType().accept(this, null);
      }
    }
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
    for (Abstract.TypeArgument typeArgument : expr.getArguments()) {
      typeArgument.getType().accept(this, null);
    }
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
    for (Abstract.BinOpSequenceElem binOpSequenceElem : expr.getSequence()) {
      binOpSequenceElem.binOp.accept(this, null);
      binOpSequenceElem.argument.accept(this, null);
    }

    return null;
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Void params) {
    for (Abstract.Expression expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    for (Abstract.Clause clause : expr.getClauses()) {
      clause.getExpression().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void params) {
    for (Abstract.Expression expression : expr.getExpressions()) {
      expression.accept(this, null);
    }
    for (Abstract.Clause clause : expr.getClauses()) {
      clause.getExpression().accept(this, null);
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
    for (Abstract.ImplementStatement implementStatement : expr.getStatements()) {
      implementStatement.getExpression().accept(this, null);
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
    for (Abstract.LetClause letClause : expr.getClauses()) {
      for (Abstract.Argument argument : letClause.getArguments()) {
        visitArgument(argument);
      }
      letClause.getResultType().accept(this, null);
      letClause.getTerm().accept(this, null);
    }
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitNumericLiteral(Abstract.NumericLiteral expr, Void params) {
    return null;
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, Void params) {
    stat.getDefinition().accept(this, null);
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
    myModuleLoader.load(new FileModuleID(new ModulePath(stat.getPath())));
    return null;
  }

  @Override
  public Void visitDefaultStaticCommand(Abstract.DefaultStaticStatement stat, Void params) {
    return null;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Void params) {
    for (Abstract.Argument argument : def.getArguments()) {
      visitArgument(argument);
    }
    if (def.getResultType() != null) def.getResultType().accept(this, null);
    def.getTerm().accept(this, null);
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitAbstract(Abstract.AbstractDefinition def, Void params) {
    for (Abstract.Argument argument : def.getArguments()) {
      visitArgument(argument);
    }
    def.getResultType().accept(this, null);
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Void params) {
    for (Abstract.TypeArgument typeArgument : def.getParameters()) {
      typeArgument.getType().accept(this, null);
    }
    for (Abstract.Constructor constructor : def.getConstructors()) {
      for (Abstract.TypeArgument typeArgument : constructor.getArguments()) {
        typeArgument.getType().accept(this, null);
      }
    }
    for (Abstract.Condition condition : def.getConditions()) {
      condition.getTerm().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Void params) {
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(this, null);
    }
    return null;
  }

  private void visitArgument(Abstract.Argument argument) {
    if (argument instanceof Abstract.TelescopeArgument) {
      ((Abstract.TelescopeArgument) argument).getType().accept(this, null);
    }
  }
}
