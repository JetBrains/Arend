package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.naming.oneshot.ResolveListener;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

public class ConcreteResolveListener implements ResolveListener {
  @Override
  public void nameResolved(Abstract.DefCallExpression defCallExpression, Referable definition) {
    ((Concrete.DefCallExpression) defCallExpression).setResolvedDefinition(definition);
  }

  @Override
  public void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Referable module) {
    ((Concrete.ModuleCallExpression) moduleCallExpression).setModule(module);
  }

  @Override
  public void nsCmdResolved(Abstract.NamespaceCommandStatement nsCmdStatement, Referable definition) {
    ((Concrete.NamespaceCommandStatement) nsCmdStatement).setResolvedClass(definition);
  }

  @Override
  public void superClassResolved(Abstract.SuperClass superClass, Referable definition) {
    ((Concrete.SuperClass) superClass).setReferent(definition);
  }

  @Override
  public void idPairFirstResolved(Abstract.IdPair idPair, Referable definition) {
    ((Concrete.IdPair) idPair).setFirstReferent(definition);
  }

  @Override
  public Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Referable binOp, Abstract.DefCallExpression var, Abstract.Expression right) {
    return ((Concrete.BinOpSequenceExpression) binOpExpr).makeBinOp(left, binOp, var, right);
  }

  @Override
  public Abstract.Expression makeError(Abstract.BinOpSequenceExpression binOpExpr, Abstract.SourceNode node) {
    return ((Concrete.BinOpSequenceExpression) binOpExpr).makeError(node);
  }

  @Override
  public void replaceBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression expression) {
    ((Concrete.BinOpSequenceExpression) binOpExpr).replace(expression);
  }

  @Override
  public void replaceWithConstructor(Abstract.PatternArgument patternArg) {
    ((Concrete.PatternArgument) patternArg).replaceWithConstructor();
  }

  @Override
  public void replaceWithConstructor(Abstract.PatternContainer container, int index) {
    ((Concrete.PatternContainer) container).replaceWithConstructor(index);
  }
}
