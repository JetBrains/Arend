package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.ClassCall;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.param;

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

  public FieldSet getFieldSet() {
    return myFieldSet;
  }

  public void setFieldSet(FieldSet fieldSet) {
    myFieldSet = fieldSet;
  }

  public SortMax getSorts() {
    return myFieldSet.getSorts(ClassCall(this, myFieldSet));
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
  public TypeMax getTypeWithParams(List<DependentLink> params, LevelArguments polyArguments) {
    if (getThisClass() != null) {
      params.add(param(ClassCall(getThisClass(), polyArguments)));
    }
    return new PiUniverseType(EmptyDependentLink.getInstance(), getSorts().subst(polyArguments.toLevelSubstitution()));
  }

  @Override
  public ClassCallExpression getDefCall(LevelArguments polyArguments) {
    return ClassCall(this, polyArguments, myFieldSet);
  }

  @Override
  public ClassCallExpression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    return new ClassCallExpression(this, polyArguments, myFieldSet);
  }

  @Override
  public void setThisClass(ClassDefinition enclosingClass) {
    assert myEnclosingThisField == null;
    super.setThisClass(enclosingClass);
    if (enclosingClass != null) {
      myEnclosingThisField = new ClassField(null, ClassCall(enclosingClass, new LevelArguments(new Level(LevelBinding.PLVL_BND), new Level(LevelBinding.HLVL_BND))), this, param("\\this", ClassCall(this, new LevelArguments(new Level(LevelBinding.PLVL_BND), new Level(LevelBinding.HLVL_BND)))));
      myEnclosingThisField.setThisClass(this);
      myFieldSet.addField(myEnclosingThisField);
    }
  }

  public ClassField getEnclosingThisField() {
    return myEnclosingThisField;
  }
}
