package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.frontend.resolving.ResolveListener;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class ConcreteResolveListener implements ResolveListener {
  private final ErrorReporter myErrorReporter;

  public ConcreteResolveListener(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public void nameResolved(Abstract.DefCallExpression defCallExpression, Abstract.Definition definition) {
    ((Concrete.DefCallExpression) defCallExpression).setResolvedDefinition(definition);
  }

  @Override
  public void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Abstract.Definition module) {
    ((Concrete.ModuleCallExpression) moduleCallExpression).setModule(module);
  }

  @Override
  public void nsCmdResolved(Abstract.NamespaceCommandStatement nsCmdStatement, Abstract.Definition definition) {
    ((Concrete.NamespaceCommandStatement) nsCmdStatement).setResolvedClass(definition);
  }

  @Override
  public void implementResolved(Abstract.Implementation implementDef, Abstract.ClassField definition) {
    ((Concrete.Implementation) implementDef).setImplemented(definition);
  }

  @Override
  public void implementResolved(Abstract.ClassFieldImpl implementStmt, Abstract.ClassField definition) {
    ((Concrete.ClassFieldImpl) implementStmt).setImplementedField(definition);
  }

  @Override
  public void classViewResolved(Abstract.ClassView classView, Abstract.ClassField classifyingField) {
    ((Concrete.ClassView) classView).setClassifyingField(classifyingField);
  }

  @Override
  public void classViewFieldResolved(Abstract.ClassViewField classViewField, Abstract.ClassField definition) {
    ((Concrete.ClassViewField) classViewField).setUnderlyingField(definition);
  }

  @Override
  public Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Abstract.Definition binOp, Abstract.DefCallExpression var, Abstract.Expression right) {
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

  @Override
  public void report(GeneralError error) {
    myErrorReporter.report(error);
  }
}
