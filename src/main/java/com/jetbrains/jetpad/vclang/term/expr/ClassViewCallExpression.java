package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;

public class ClassViewCallExpression extends ClassCallExpression {
  private final Abstract.ClassView myClassView;

  public ClassViewCallExpression(ClassDefinition definition, LevelSubstitution polyParams, Abstract.ClassView classView) {
    super(definition, polyParams);
    myClassView = classView;
  }

  public ClassViewCallExpression(ClassDefinition definition, LevelSubstitution polyParams, FieldSet fieldSet, Abstract.ClassView classView) {
    super(definition, polyParams, fieldSet);
    myClassView = classView;
  }

  public Abstract.ClassView getClassView() {
    return myClassView;
  }
}
