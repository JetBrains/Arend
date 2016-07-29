package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.term.expr.type.PiUniverseType;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;

import java.util.HashSet;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ClassCall;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.param;

public class ClassDefinition extends Definition {
  private final Namespace myOwnNamespace;
  private final Namespace myInstanceNamespace;
  private final FieldSet myFieldSet;
  private final Set<ClassDefinition> mySuperClasses;

  private ClassField myEnclosingThisField = null;

  public ClassDefinition(String name, Namespace ownNamespace, Namespace instanceNamespace) {
    this(name, new FieldSet(), new HashSet<ClassDefinition>(), ownNamespace, instanceNamespace);
  }

  public ClassDefinition(String name, FieldSet fieldSet, Set<ClassDefinition> superClasses, Namespace ownNamespace, Namespace instanceNamespace) {
    super(name, Abstract.Binding.DEFAULT_PRECEDENCE);
    super.hasErrors(false);
    myFieldSet = fieldSet;
    mySuperClasses = superClasses;
    myOwnNamespace = ownNamespace;
    myInstanceNamespace = instanceNamespace;
  }

  public FieldSet getFieldSet() {
    return myFieldSet;
  }

  public SortMax getSorts() {
    return myFieldSet.getSorts(getDefCall());
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this.equals(classDefinition)) return true;
    for (ClassDefinition superClass : mySuperClasses) {
      if (superClass.isSubClassOf(classDefinition)) return true;
    }
    return false;
  }

  @Override
  public Type getType() {
    return new PiUniverseType(EmptyDependentLink.getInstance(), getSorts());
  }

  @Override
  public ClassCallExpression getDefCall() {
    return ClassCall(this, myFieldSet);
  }

  @Override
  public void setThisClass(ClassDefinition enclosingClass) {
    assert myEnclosingThisField == null;
    super.setThisClass(enclosingClass);
    if (enclosingClass != null) {
      myEnclosingThisField = new ClassField(getName() + "::\\parent", Abstract.Binding.DEFAULT_PRECEDENCE, ClassCall(enclosingClass), this, param("\\this", ClassCall(this)));
      myEnclosingThisField.setThisClass(this);
      myFieldSet.addField(myEnclosingThisField, getDefCall());
    }
  }

  public ClassField getEnclosingThisField() {
    return myEnclosingThisField;
  }

  @Override
  public Type getTypeWithThis() {
    DependentLink link = EmptyDependentLink.getInstance();
    if (getThisClass() != null) {
      link = param(ClassCall(getThisClass()));
    }
    return new PiUniverseType(link, getSorts());
  }

  @Override
  public Namespace getOwnNamespace() {
    return myOwnNamespace;
  }

  @Override
  public Namespace getInstanceNamespace() {
    return myInstanceNamespace;
  }

  // Used only in ToAbstractVisitor
  public String getFieldName(ClassField field) {
    return "<some field>"; // HACK
  }
}
