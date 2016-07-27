package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.internal.FieldSet;

import java.util.HashSet;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

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

  @Override
  public TypeUniverse getUniverse() {
    return myFieldSet.getUniverse(getDefCall());
  }

  @Override
  public void setUniverse(TypeUniverse universe) {
    throw new UnsupportedOperationException();
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this.equals(classDefinition)) return true;
    for (ClassDefinition superClass : mySuperClasses) {
      if (superClass.isSubClassOf(classDefinition)) return true;
    }
    return false;
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
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
  public Expression getTypeWithThis() {
    Expression type = getType();
    if (getThisClass() != null) {
      type = Pi(ClassCall(getThisClass()), type);
    }
    return type;
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
