package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClassDefinition extends Definition {
  private FieldSet myFieldSet;
  private Set<ClassDefinition> mySuperClasses;
  private Sort mySort;

  private ClassField myEnclosingThisField = null;

  public ClassDefinition(Abstract.ClassDefinition abstractDef) {
    super(abstractDef, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myFieldSet = null;
    mySuperClasses = null;
    mySort = Sort.PROP;
  }

  @Override
  public Abstract.ClassDefinition getAbstractDefinition() {
    return (Abstract.ClassDefinition) super.getAbstractDefinition();
  }

  public FieldSet getFieldSet() {
    return myFieldSet;
  }

  public void setFieldSet(FieldSet fieldSet) {
    myFieldSet = fieldSet;
  }

  public void updateSorts() {
    ClassCallExpression thisClass = new ClassCallExpression(this, Sort.STD, Collections.emptyMap(), mySort);
    mySort = Sort.PROP;

    for (ClassField field : myFieldSet.getFields()) {
      if (myFieldSet.isImplemented(field)) {
        continue;
      }

      Expression baseType = field.getBaseType(thisClass.getSortArgument());
      if (baseType.isInstance(ErrorExpression.class)) {
        continue;
      }

      DependentLink thisParam = ExpressionFactory.parameter("this", thisClass);
      Expression expr = baseType.subst(field.getThisParameter(), new ReferenceExpression(thisParam)).normalize(NormalizeVisitor.Mode.WHNF);
      Sort sort = expr.getType().toSort();
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

  public Set<ClassDefinition> getSuperClasses() {
    return Collections.unmodifiableSet(mySuperClasses);
  }

  public void setSuperClasses(Set<ClassDefinition> superClasses) {
    mySuperClasses = superClasses;
  }

  @Override
  public Expression getTypeWithParams(List<? super DependentLink> params, Sort sortArgument) {
    if (getThisClass() != null) {
      params.add(ExpressionFactory.parameter(null, new ClassCallExpression(getThisClass(), sortArgument)));
    }
    return new UniverseExpression(getSort().subst(sortArgument.toLevelSubstitution()));
  }

  @Override
  public ClassCallExpression getDefCall(Sort sortArgument, Expression thisExpr, List<Expression> args) {
    return new ClassCallExpression(this, sortArgument, thisExpr == null ? Collections.emptyMap() : Collections.singletonMap(myEnclosingThisField, thisExpr), mySort);
  }

  @Override
  public void setThisClass(ClassDefinition enclosingClass) {
    assert myEnclosingThisField == null;
    super.setThisClass(enclosingClass);
    if (enclosingClass != null && myFieldSet != null) {
      myEnclosingThisField = new ClassField(null, new ClassCallExpression(enclosingClass, Sort.STD), this, ExpressionFactory.parameter("\\this", new ClassCallExpression(this, Sort.STD)));
      myEnclosingThisField.setThisClass(this);
      myFieldSet.addField(myEnclosingThisField);
    }
  }

  public ClassField getEnclosingThisField() {
    return myEnclosingThisField;
  }

  public void setEnclosingThisField(ClassField field) {
    myEnclosingThisField = field;
  }
}
