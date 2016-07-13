package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class ClassDefinition extends Definition {
  private final Namespace myOwnNamespace;
  private final Namespace myInstanceNamespace;

  private final List<ClassCallExpression> mySuperClasses = new ArrayList<>();
  private ClassField myEnclosingThisField = null;
  private final Set<ClassField> myFields = new HashSet<>();
  private final Map<ClassField, ClassCallExpression.ImplementStatement> myImplemented;

  public ClassDefinition(String name, Namespace ownNamespace, Namespace instanceNamespace) {
    this(name, Collections.<ClassField, ClassCallExpression.ImplementStatement>emptyMap(), ownNamespace, instanceNamespace);
  }

  public ClassDefinition(String name, Map<ClassField, ClassCallExpression.ImplementStatement> implemented, Namespace ownNamespace, Namespace instanceNamespace) {
    super(name, Abstract.Binding.DEFAULT_PRECEDENCE);
    super.hasErrors(false);
    myImplemented = implemented;  /* this map will be updated outside by DefinitionCheckTypeVisitor during creation */
    myOwnNamespace = ownNamespace;
    myInstanceNamespace = instanceNamespace;
  }

  public boolean isSubClassOf(ClassDefinition classDefinition) {
    if (this == classDefinition) {
      return true;
    }
    for (ClassCallExpression superClass : mySuperClasses) {
      if (superClass.getDefinition().isSubClassOf(classDefinition)) {
        return true;
      }
    }
    return false;
  }

  public void addSuperClass(ClassCallExpression superClass) {
    mySuperClasses.add(superClass);
    myFields.addAll(superClass.getDefinition().getFields());
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public ClassCallExpression getDefCall() {
    return ClassCall(this, new HashMap<ClassField, ClassCallExpression.ImplementStatement>());
  }

  public Set<ClassField> getFields() {
    return myFields;
  }

  public void addField(ClassField field) {
    myFields.add(field);
  }

  @Override
  public void setThisClass(ClassDefinition enclosingClass) {
    super.setThisClass(enclosingClass);
    if (enclosingClass != null) {
      myEnclosingThisField = new ClassField("\\parent", Abstract.Binding.DEFAULT_PRECEDENCE, ClassCall(enclosingClass), this, param("\\this", ClassCall(this)));
      myEnclosingThisField.setThisClass(this);
      addField(myEnclosingThisField);
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

  public Map<ClassField, ClassCallExpression.ImplementStatement> getImplemented() {
    return myImplemented;
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
