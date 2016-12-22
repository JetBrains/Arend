package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class BaseResolveListener implements ResolveListener {
  @Override
  public void nameResolved(Abstract.DefCallExpression defCallExpression, Abstract.Definition resolvedDefinition) {

  }

  @Override
  public void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Abstract.Definition module) {

  }

  @Override
  public void nsCmdResolved(Abstract.NamespaceCommandStatement nsCmdStatement, Abstract.Definition definition) {

  }

  @Override
  public void implementResolved(Abstract.Implementation identifier, Abstract.Definition definition) {

  }

  @Override
  public void implementResolved(Abstract.ClassFieldImpl identifier, Abstract.Definition definition) {

  }

  @Override
  public void classViewResolved(Abstract.ClassView classView, Abstract.ClassField classifyingField) {

  }

  @Override
  public void classViewFieldResolved(Abstract.ClassViewField field, Abstract.ClassField definition) {

  }

  @Override
  public Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Abstract.Definition definition, Abstract.DefCallExpression var, Abstract.Expression right) {
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
