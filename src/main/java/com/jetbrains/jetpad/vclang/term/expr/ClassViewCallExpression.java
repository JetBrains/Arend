package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;

public class ClassViewCallExpression extends ClassCallExpression {
  private final ClassView myClassView;

  public ClassViewCallExpression(ClassDefinition definition, ClassView classView) {
    super(definition);
    myClassView = classView;
  }

  public ClassViewCallExpression(ClassDefinition definition, FieldSet fieldSet, ClassView classView) {
    super(definition, fieldSet);
    myClassView = classView;
  }

  public ClassView getClassView() {
    return myClassView;
  }
}
