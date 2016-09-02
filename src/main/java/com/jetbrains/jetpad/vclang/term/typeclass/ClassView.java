package com.jetbrains.jetpad.vclang.term.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.Map;

public class ClassView {
  private final Map<com.jetbrains.jetpad.vclang.term.definition.ClassField, Abstract.ClassField> myViews;

  public static ClassView DEFAULT = new ClassView(Collections.<com.jetbrains.jetpad.vclang.term.definition.ClassField, Abstract.ClassField>emptyMap());

  private ClassView(Map<com.jetbrains.jetpad.vclang.term.definition.ClassField, Abstract.ClassField> view) {
    myViews = view;
  }

  public Abstract.ClassField getView(com.jetbrains.jetpad.vclang.term.definition.ClassField field) {
    Abstract.ClassField viewField = myViews.get(field);
    return viewField != null ? viewField : field.getAbstractDefinition();
  }
}
