package com.jetbrains.jetpad.vclang.typechecking.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;

import java.util.*;

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
    Set<TCReferable> futureFields = new HashSet<>();
    for (Concrete.ClassField field : def.getFields()) {
      futureFields.add(field.getData());
    }

    Set<LocatedReferable> fields = new HashSet<>();
    Set<GlobalReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(def.getData());
    while (!toVisit.isEmpty()) {
      ClassReferable classRef = toVisit.pop();
      if (!visitedClasses.add(classRef)) {
        continue;
      }

      fields.addAll(classRef.getFieldReferables());
      toVisit.addAll(classRef.getSuperClassReferences());
    }

    ClassFieldChecker classFieldChecker = new ClassFieldChecker(null, fields, futureFields, new ProxyErrorReporter(def.getData(), myErrorReporter));
    for (Concrete.ClassField classField : def.getFields()) {
      Concrete.Expression fieldType = classField.getResultType();
      Referable thisParameter = new LocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      classField.setResultType(new Concrete.PiExpression(fieldType.getData(), Collections.singletonList(new Concrete.TelescopeParameter(fieldType.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(fieldType.getData(), def.getData()))), fieldType.accept(classFieldChecker, null)));
      futureFields.remove(classField.getData());
    }

    for (Concrete.ClassFieldImpl classFieldImpl : def.getImplementations()) {
      Concrete.Expression impl = classFieldImpl.implementation;
      Referable thisParameter = new LocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      classFieldImpl.implementation = new Concrete.LamExpression(impl.getData(), Collections.singletonList(new Concrete.TelescopeParameter(impl.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(impl.getData(), def.getData()))), impl.accept(classFieldChecker, null));
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
