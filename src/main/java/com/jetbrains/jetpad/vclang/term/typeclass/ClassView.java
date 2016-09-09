package com.jetbrains.jetpad.vclang.term.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;

import java.util.Collections;
import java.util.Map;

public class ClassView {
  private final Map<ClassField, Abstract.ClassField> myViews;

  public static ClassView DEFAULT = new ClassView(Collections.<ClassField, Abstract.ClassField>emptyMap());

  private ClassView(Map<ClassField, Abstract.ClassField> view) {
    myViews = view;
  }

  public Abstract.ClassField getView(ClassField field) {
    Abstract.ClassField viewField = myViews.get(field);
    return viewField != null ? viewField : field.getAbstractDefinition();
  }

  public ClassField getClassifyingField() {
    return null;
  }
}
