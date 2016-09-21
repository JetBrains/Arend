package com.jetbrains.jetpad.vclang.term.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;

import java.util.HashMap;
import java.util.Map;

public class ClassView {
  private final Map<ClassField, Abstract.ClassField> myViews;
  private final ClassField myClassifyingField;
  private final Abstract.ClassView myAbstractClassView;

  public ClassView(ClassField classifyingField, Abstract.ClassView classView) {
    myViews = new HashMap<>();
    myClassifyingField = classifyingField;
    myAbstractClassView = classView;
  }

  public Abstract.ClassView getAbstract() {
    return myAbstractClassView;
  }

  public void addView(ClassField field, Abstract.ClassField abstractField) {
    myViews.put(field, abstractField);
  }

  public Abstract.ClassField getView(ClassField field) {
    Abstract.ClassField viewField = myViews.get(field);
    return viewField != null ? viewField : field.getAbstractDefinition();
  }

  public ClassField getClassifyingField() {
    return myClassifyingField;
  }
}
