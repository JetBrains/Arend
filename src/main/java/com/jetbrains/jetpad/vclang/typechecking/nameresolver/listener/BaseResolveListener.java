package com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.BaseDefinition;

public class BaseResolveListener implements ResolveListener {
  @Override
  public void nameResolved(Abstract.DefCallExpression defCallExpression, BaseDefinition resolvedDefinition) {

  }

  @Override
  public void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, BaseDefinition module) {

  }

  @Override
  public Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, BaseDefinition definition, Abstract.DefCallExpression var, Abstract.Expression right) {
    return null;
  }

  @Override
  public Abstract.Expression makeError(Abstract.BinOpSequenceExpression binOpExpr, Abstract.SourceNode node) {
    return null;
  }

  @Override
  public void replaceBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression expression) {

  }

  @Override
  public void replaceWithConstructor(Abstract.PatternArgument patternArg) {

  }

  @Override
  public void replaceWithConstructor(Abstract.PatternContainer container, int index) {

  }
}
