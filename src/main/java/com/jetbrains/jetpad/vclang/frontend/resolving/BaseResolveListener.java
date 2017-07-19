package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class BaseResolveListener implements ResolveListener {
  private final ErrorReporter myErrorReporter;

  public BaseResolveListener(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter != null ? errorReporter : new DummyErrorReporter();
  }

  @Override
  public void nameResolved(Abstract.ReferenceExpression referenceExpression, Abstract.ReferableSourceNode referable) {

  }

  @Override
  public void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Abstract.Definition module) {

  }

  @Override
  public void openCmdResolved(OpenCommand openCmd, Abstract.Definition definition) {

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
  public void classViewInstanceResolved(Abstract.ClassViewInstance instance, Abstract.Definition classifyingDefinition) {

  }

  @Override
  public Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Abstract.Definition definition, Abstract.ReferenceExpression var, Abstract.Expression right) {
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
  public void replaceWithConstructor(List<? extends Abstract.Pattern> patterns, int index, Abstract.Constructor constructor) {

  }

  @Override
  public void replaceWithConstructor(Abstract.FunctionClause clause, int index, Abstract.Constructor constructor) {

  }

  @Override
  public void patternResolved(Abstract.ConstructorPattern pattern, Abstract.Constructor definition) {

  }
}
