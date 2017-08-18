package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class BaseResolveListener implements ResolveListener {
  @Override
  public void nameResolved(Abstract.ReferenceExpression referenceExpression, Abstract.ReferableSourceNode referable) {

  }

  @Override
  public void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Abstract.Definition module) {

  }

  @Override
  public void openCmdResolved(OpenCommand openCmd, Abstract.GlobalReferableSourceNode definition) {

  }

  @Override
  public void implementResolved(Abstract.Implementation identifier, Abstract.ClassField definition) {

  }

  @Override
  public void implementResolved(Abstract.ClassFieldImpl identifier, Abstract.ClassField definition) {

  }

  @Override
  public void classViewResolved(Abstract.ClassView classView, Abstract.ClassField classifyingField) {

  }

  @Override
  public void classViewFieldResolved(Abstract.ClassViewField field, Abstract.ClassField definition) {

  }

  @Override
  public void classViewInstanceResolved(Abstract.ClassViewInstance instance, Abstract.GlobalReferableSourceNode classifyingDefinition) {

  }

  @Override
  public Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Abstract.ReferableSourceNode definition, Abstract.ReferenceExpression var, Abstract.Expression right) {
    return null;
  }

  @Override
  public Abstract.Expression makeError(Abstract.BinOpSequenceExpression binOpExpr, Object node) {
    return null;
  }

  @Override
  public void replaceBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression expression) {

  }

  @Override
  public void replaceWithConstructor(Abstract.PatternContainer container, int index, Abstract.Constructor constructor) {

  }

  @Override
  public void patternResolved(Abstract.ConstructorPattern pattern, Abstract.Constructor definition) {

  }
}
