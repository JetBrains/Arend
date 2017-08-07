package com.jetbrains.jetpad.vclang.core.internal;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

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
  private Sort mySort;

  public FieldSet(Sort sort) {
    this(new LinkedHashSet<>(), new HashMap<>(), sort);
  }

  public FieldSet(FieldSet other) {
    this(new LinkedHashSet<>(other.myFields), new HashMap<>(other.myImplemented), other.mySort);
  }

  private FieldSet(LinkedHashSet<ClassField> fields, Map<ClassField, Implementation> implemented, Sort sort) {
    myFields = fields;
    myImplemented = implemented;
    mySort = sort;
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

  public Sort getSort() {
    return mySort;
  }

  public void updateSorts(ClassCallExpression thisClass) {
    mySort = Sort.PROP;
    for (ClassField field : myFields) {
      updateUniverseSingleField(field, thisClass);
    }
  }

  private void updateUniverseSingleField(ClassField field, ClassCallExpression thisClass) {
    if (myImplemented.containsKey(field)) return;

    Expression baseType = field.getBaseType(thisClass.getSortArgument());
    if (baseType.isInstance(ErrorExpression.class)) return;

    DependentLink thisParam = ExpressionFactory.parameter("\\this", thisClass);
    Expression expr = baseType.subst(field.getThisParameter(), new ReferenceExpression(thisParam)).normalize(NormalizeVisitor.Mode.WHNF);
    Sort sort = expr.getType().toSort();
    if (sort != null) {
      mySort = mySort.max(sort);
    }
  }

  @Override
  public String toString() {
    ArrayList<String> fields = myFields.stream().map(ClassField::getName).collect(Collectors.toCollection(ArrayList::new));
    ArrayList<String> impl = myImplemented.keySet().stream().map(ClassField::getName).collect(Collectors.toCollection(ArrayList::new));
    return "All: " + fields.toString() + " Impl: " + impl.toString();
  }
}
