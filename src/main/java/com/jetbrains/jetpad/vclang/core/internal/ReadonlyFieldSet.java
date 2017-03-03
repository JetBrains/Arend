package com.jetbrains.jetpad.vclang.core.internal;

import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.Collection;
import java.util.Map;

public interface ReadonlyFieldSet {
  Collection<? extends ClassField> getFields();
  Collection<? extends Map.Entry<ClassField, FieldSet.Implementation>> getImplemented();
  boolean isImplemented(ClassField field);
  FieldSet.Implementation getImplementation(ClassField field);
  Sort getSort();
  void updateSorts(ClassCallExpression classCall);
}
