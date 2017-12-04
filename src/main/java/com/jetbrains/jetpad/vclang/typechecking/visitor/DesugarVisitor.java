package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;

public class DesugarVisitor implements ConcreteDefinitionVisitor<Void, Void> {
  private final ErrorReporter myErrorReporter;

  public DesugarVisitor(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView def, Void params) {
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Void params) {
    return null;
  }
}
