package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.internal.ReadonlyFieldSet;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClassDefinition extends Definition {
  private FieldSet myFieldSet;
  private Set<ClassDefinition> mySuperClasses;

  private ClassField myEnclosingThisField = null;

  public ClassDefinition(Abstract.ClassDefinition abstractDef) {
    super(abstractDef, TypeCheckingStatus.HEADER_HAS_ERRORS);
    myFieldSet = null;
    mySuperClasses = null;
  }

  @Override
  public Abstract.ClassDefinition getAbstractDefinition() {
    return (Abstract.ClassDefinition) super.getAbstractDefinition();
  }

  public ReadonlyFieldSet getFieldSet() {
    return myFieldSet;
  }

  public void setFieldSet(FieldSet fieldSet) {
    myFieldSet = fieldSet;
  }

  public void updateSorts() {
    myFieldSet.updateSorts(new ClassCallExpression(this, Sort.STD, myFieldSet));
  }

  public Sort getSort() {
    return myFieldSet.getSort();
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
    FieldSet fieldSet;
    if (thisExpr != null) {
      fieldSet = new FieldSet(myFieldSet);
      boolean success = fieldSet.implementField(myEnclosingThisField, new FieldSet.Implementation(null, thisExpr));
      assert success;
    } else {
      fieldSet = myFieldSet;
    }

    ClassCallExpression classCall = new ClassCallExpression(this, sortArgument, fieldSet);
    if (thisExpr != null) {
      fieldSet.updateSorts(classCall);
    }
    return classCall;
  }

  @Override
  public ClassCallExpression getDefCall(Sort sortArgument, List<Expression> args) {
    return new ClassCallExpression(this, sortArgument, myFieldSet);
  }

  @Override
  public void setThisClass(ClassDefinition enclosingClass) {
    assert myEnclosingThisField == null;
    super.setThisClass(enclosingClass);
    if (enclosingClass != null) {
      myEnclosingThisField = new ClassField(null, new ClassCallExpression(enclosingClass, Sort.STD), this, ExpressionFactory.parameter("\\this", new ClassCallExpression(this, Sort.STD)));
      myEnclosingThisField.setThisClass(this);
      myFieldSet.addField(myEnclosingThisField, enclosingClass.getSort());
    }
  }

  public ClassField getEnclosingThisField() {
    return myEnclosingThisField;
  }
}
