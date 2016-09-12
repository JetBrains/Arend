package com.jetbrains.jetpad.vclang.term.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClassView {
  private final Map<ClassField, Abstract.ClassField> myViews;
  private final ClassField myClassifyingField;

  public static ClassView DEFAULT = new ClassView(Collections.<ClassField, Abstract.ClassField>emptyMap());

  public ClassView(ClassField classifyingField) {
    myViews = new HashMap<>();
    myClassifyingField = classifyingField;
  }

  private ClassView(Map<ClassField, Abstract.ClassField> view) {
    myViews = view;
    myClassifyingField = null;
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
