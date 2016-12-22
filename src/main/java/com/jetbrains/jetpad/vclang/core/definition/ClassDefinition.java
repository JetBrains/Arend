package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.core.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;

import java.util.*;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.ClassCall;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.param;

public class ClassDefinition extends Definition {
  private FieldSet myFieldSet;
  private Set<ClassDefinition> mySuperClasses;
  private boolean myHasErrors;

  private ClassField myEnclosingThisField = null;

  public ClassDefinition(Abstract.ClassDefinition abstractDef) {
    this(abstractDef, new FieldSet(), new HashSet<ClassDefinition>());
  }

  public ClassDefinition(Abstract.ClassDefinition abstractDef, FieldSet fieldSet, Set<ClassDefinition> superClasses) {
    super(abstractDef);
    myFieldSet = fieldSet;
    mySuperClasses = superClasses;
    myHasErrors = false;
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
    DependentLink link = EmptyDependentLink.getInstance();
    if (getThisClass() != null) {
      link = param(ClassCall(getThisClass(), polyArguments));
    }
    return new PiUniverseType(link, getSorts().subst(polyArguments.toLevelSubstitution(this)));
  }

  @Override
  public ClassCallExpression getDefCall(LevelArguments polyArguments) {
    return ClassCall(this, polyArguments, myFieldSet);
  }

  @Override
  public ClassCallExpression getDefCall(LevelArguments polyArguments, List<Expression> args) {
    return new ClassCallExpression(this, polyArguments, myFieldSet);
  }

  public List<LevelBinding> getEnclosingPolyParams() {
    assert myEnclosingThisField != null;
    int numEnclosingParams = myEnclosingThisField.getBaseType().toClassCall().getDefinition().getPolyParams().size();
    List<LevelBinding> enclosingParams = new ArrayList<>();
    for (int i = myPolyParams.size() - numEnclosingParams; i < myPolyParams.size(); ++i) {
      enclosingParams.add(myPolyParams.get(i));
    }
    return enclosingParams;
  }

  @Override
  public void setThisClass(ClassDefinition enclosingClass) {
    assert myEnclosingThisField == null;
    super.setThisClass(enclosingClass);
    if (enclosingClass != null) {
      for (LevelBinding param : enclosingClass.getPolyParams()) {
        myPolyParams.add(new LevelBinding(param.getName(), param.getType()));
      }

      List<Level> enclosingArgs = new ArrayList<>();
      List<Level> thisArgs = new ArrayList<>();

      for (int i = 0; i < myPolyParams.size(); ++i) {
        thisArgs.add(new Level(myPolyParams.get(i)));
        if (i >= myPolyParams.size() - enclosingArgs.size()) {
          enclosingArgs.add(new Level(myPolyParams.get(i)));
        }
      }

      myEnclosingThisField = new ClassField(null, ClassCall(enclosingClass, new LevelArguments(enclosingArgs)), this, param("\\this", ClassCall(this, new LevelArguments(thisArgs))));
      myEnclosingThisField.setThisClass(this);
      myFieldSet.addField(myEnclosingThisField);
    }
  }

  @Override
  public boolean typeHasErrors() {
    return false;
  }

  @Override
  public TypeCheckingStatus hasErrors() {
    return myHasErrors ? TypeCheckingStatus.HAS_ERRORS : TypeCheckingStatus.NO_ERRORS;
  }

  @Override
  public void hasErrors(TypeCheckingStatus status) {
    myHasErrors = !TypeCheckingStatus.NO_ERRORS.equals(status);
  }

  public ClassField getEnclosingThisField() {
    return myEnclosingThisField;
  }
}
