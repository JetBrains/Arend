package com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;

public class ConcreteResolveListener implements ResolveListener {
  @Override
  public void nameResolved(Abstract.DefCallExpression defCallExpression, ResolvedName resolvedName) {
    ((Concrete.DefCallExpression) defCallExpression).setResolvedName(resolvedName);
  }

  @Override
  public Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, ResolvedName name, Abstract.DefCallExpression var, Abstract.Expression right) {
    return ((Concrete.BinOpSequenceExpression) binOpExpr).makeBinOp(left, name, var, right);
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
