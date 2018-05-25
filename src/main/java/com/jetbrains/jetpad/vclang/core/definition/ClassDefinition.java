package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.naming.reference.TCClassReferable;

import java.util.*;

public class ClassDefinition extends Definition {
  private final Set<ClassDefinition> mySuperClasses;
  private final LinkedHashSet<ClassField> myFields;
  private final List<ClassField> myPersonalFields;
  private final Map<ClassField, LamExpression> myImplemented;
  private ClassField myCoercingField;
  private Sort mySort;

  public ClassDefinition(TCClassReferable referable) {
    super(referable, TypeCheckingStatus.HEADER_HAS_ERRORS);
    mySuperClasses = new HashSet<>();
    myFields = new LinkedHashSet<>();
    myPersonalFields = new ArrayList<>();
    myImplemented = new HashMap<>();
    mySort = Sort.PROP;
  }

  @Override
  public TCClassReferable getReferable() {
    return (TCClassReferable) super.getReferable();
  }

  public ClassField getCoercingField() {
    return myCoercingField;
  }

  public void setCoercingField(ClassField coercingField) {
    myCoercingField = coercingField;
  }

  public void updateSorts() {
    ClassCallExpression thisClass = new ClassCallExpression(this, Sort.STD, Collections.emptyMap(), mySort);
    mySort = Sort.PROP;

    for (ClassField field : myFields) {
      if (myImplemented.containsKey(field)) {
        continue;
      }

      PiExpression fieldType = field.getType(Sort.STD);
      if (fieldType.getCodomain().isInstance(ErrorExpression.class)) {
        continue;
      }

      Expression type = fieldType
        .applyExpression(new ReferenceExpression(ExpressionFactory.parameter("this", thisClass)))
        .normalize(NormalizeVisitor.Mode.WHNF)
        .getType();
      Sort sort = type == null ? null : type.toSort();
      if (sort != null) {
        mySort = mySort.max(sort);
      }
    }
  }

  public Sort getSort() {
    return mySort;
  }

  public void setSort(Sort sort) {
    mySort = sort;
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this.equals(classDefinition)) return true;
    for (ClassDefinition superClass : mySuperClasses) {
      if (superClass.isSubClassOf(classDefinition)) return true;
    }
    return false;
  }

  public Set<? extends ClassDefinition> getSuperClasses() {
    return mySuperClasses;
  }

  public void addSuperClass(ClassDefinition superClass) {
    mySuperClasses.add(superClass);
  }

  public Set<? extends ClassField> getFields() {
    return myFields;
  }

  public List<? extends ClassField> getPersonalFields() {
    return myPersonalFields;
  }

  public void addField(ClassField field) {
    myFields.add(field);
  }

  public void addPersonalField(ClassField field) {
    myPersonalFields.add(field);
  }

  public void addFields(Collection<? extends ClassField> fields) {
    myFields.addAll(fields);
  }

  public boolean isImplemented(ClassField field) {
    return myImplemented.containsKey(field);
  }

  public Set<Map.Entry<ClassField, LamExpression>> getImplemented() {
    return myImplemented.entrySet();
  }

  public LamExpression getImplementation(ClassField field) {
    return myImplemented.get(field);
  }

  public void implementField(ClassField field, LamExpression impl) {
    myImplemented.put(field, impl);
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    return new UniverseExpression(getSort().subst(sortArgument.toLevelSubstitution()));
  }

  @Override
  public ClassCallExpression getDefCall(Sort sortArgument, List<Expression> args) {
    return new ClassCallExpression(this, sortArgument, Collections.emptyMap(), mySort);
  }
}
