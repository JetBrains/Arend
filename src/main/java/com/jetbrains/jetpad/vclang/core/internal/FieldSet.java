package com.jetbrains.jetpad.vclang.core.internal;

import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

import java.util.*;
import java.util.stream.Collectors;

public class FieldSet {
  public static class Implementation {
    public final TypedDependentLink thisParam;
    public final Expression term;

    public Implementation(TypedDependentLink thisParam, Expression term) {
      this.thisParam = thisParam;
      this.term = term;
    }

    public Expression substThisParam(Expression thisExpr) {
      return thisParam == null ? term : term.subst(thisParam, thisExpr);
    }
  }

  private final LinkedHashSet<ClassField> myFields;
  private final Map<ClassField, Implementation> myImplemented;

  public FieldSet() {
    myFields = new LinkedHashSet<>();
    myImplemented = new HashMap<>();
  }

  public void addField(ClassField field) {
    myFields.add(field);
  }

  public void addFieldsFrom(FieldSet other) {
    myFields.addAll(other.getFields());
  }

  @SuppressWarnings("UnusedReturnValue")
  public boolean implementField(ClassField field, Implementation impl) {
    assert myFields.contains(field);
    Implementation old = myImplemented.put(field, impl);
    return old == null;
  }

  public boolean isImplemented(ClassField field) {
    return myImplemented.containsKey(field);
  }

  public Set<ClassField> getFields() {
    return myFields;
  }

  public Set<Map.Entry<ClassField, Implementation>> getImplemented() {
    return myImplemented.entrySet();
  }

  public Implementation getImplementation(ClassField field) {
    return myImplemented.get(field);
  }

  @Override
  public String toString() {
    ArrayList<String> fields = myFields.stream().map(ClassField::getName).collect(Collectors.toCollection(ArrayList::new));
    ArrayList<String> impl = myImplemented.keySet().stream().map(ClassField::getName).collect(Collectors.toCollection(ArrayList::new));
    return "All: " + fields.toString() + " Impl: " + impl.toString();
  }
}
