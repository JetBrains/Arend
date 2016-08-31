package com.jetbrains.jetpad.vclang.term.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;

import java.util.Collections;
import java.util.Map;

public class ClassView {
  private final Map<ClassField, Abstract.ClassViewField> myViews;

  public static ClassView DEFAULT = new ClassView(Collections.<ClassField, Abstract.ClassViewField>emptyMap());

  private ClassView(Map<ClassField, Abstract.ClassViewField> view) {
    myViews = view;
  }

  public Abstract.ClassViewField getView(ClassField field) {
    Abstract.ClassViewField viewField = myViews.get(field);
    return viewField != null ? viewField : field.getAbstractDefinition();
  }
}
