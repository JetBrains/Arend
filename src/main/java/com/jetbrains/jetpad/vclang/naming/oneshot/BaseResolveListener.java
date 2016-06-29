package com.jetbrains.jetpad.vclang.naming.oneshot;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

public class BaseResolveListener implements ResolveListener {
  @Override
  public void nameResolved(Abstract.DefCallExpression defCallExpression, Referable resolvedDefinition) {

  }

  @Override
  public void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Referable module) {

  }

  @Override
  public void nsCmdResolved(Abstract.NamespaceCommandStatement nsCmdStatement, Referable definition) {

  }

  @Override
  public void superClassResolved(Abstract.SuperClass superClass, Referable definition) {

  }

  @Override
  public void idPairFirstResolved(Abstract.IdPair idPair, Referable definition) {

  }

  @Override
  public void identifierResolved(Abstract.Identifier identifier, Referable definition) {

  }

  @Override
  public Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Referable definition, Abstract.DefCallExpression var, Abstract.Expression right) {
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
