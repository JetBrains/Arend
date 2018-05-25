package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.reference.LocalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    Set<TCReferable> fields = new HashSet<>();
    for (Concrete.ClassField field : def.getFields()) {
      fields.add(field.getData());
    }

    Set<TCReferable> previousFields = new HashSet<>();
    ClassFieldChecker classFieldChecker = new ClassFieldChecker(null, fields, previousFields, new ProxyErrorReporter(def.getData(), myErrorReporter));
    for (Concrete.ClassField classField : def.getFields()) {
      Concrete.Expression fieldType = classField.getResultType();
      Referable thisParameter = new LocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      classField.setResultType(fieldType.accept(classFieldChecker, null));
      classField.setResultType(new Concrete.PiExpression(fieldType.getData(), Collections.singletonList(new Concrete.TelescopeParameter(fieldType.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(fieldType.getData(), def.getData()))), fieldType));
      previousFields.add(classField.getData());
    }
    return null;
  }

  @Override
  public Void visitClassSynonym(Concrete.ClassSynonym def, Void params) {
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Void params) {
    return null;
  }
}
