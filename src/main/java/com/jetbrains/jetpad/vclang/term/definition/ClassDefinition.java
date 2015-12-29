package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.ClassCall;

public class ClassDefinition extends Definition {
  private Map<String, ClassField> myFields = null;

  public ClassDefinition(Namespace parentNamespace, Name name) {
    super(parentNamespace, name, Abstract.Definition.DEFAULT_PRECEDENCE);
    super.hasErrors(false);
  }

  @Override
  public Expression getBaseType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public ClassCallExpression getDefCall() {
    return ClassCall(this, new HashMap<ClassField, ClassCallExpression.ImplementStatement>());
  }

  public ClassField getField(String name) {
    return myFields == null ? null : myFields.get(name);
  }

  public Collection<ClassField> getFields() {
    return myFields == null ? Collections.<ClassField>emptyList() : myFields.values();
  }

  public int getNumberOfVisibleFields() {
    if (myFields == null) {
      return 0;
    }
    int result = myFields.size();
    if (getParentField() != null) {
      --result;
    }
    return result;
  }

  public void addField(ClassField field) {
    if (myFields == null) {
      myFields = new HashMap<>();
    }
    myFields.put(field.getName().name, field);
    field.setThisClass(this);
  }

  public ClassField removeField(String name) {
    return myFields != null ? myFields.remove(name) : null;
  }

  public void removeField(ClassField field) {
    if (myFields != null) {
      myFields.remove(field.getName().name);
    }
  }

  public ClassField getParentField() {
    return getField("\\parent");
  }

  public void addParentField(ClassDefinition parentClass) {
    setThisClass(parentClass);
    addField(new ClassField(getParentNamespace().getChild(getName()), new Name("\\parent", Abstract.Definition.Fixity.PREFIX), Abstract.Definition.DEFAULT_PRECEDENCE, ClassCall(parentClass), this));
  }
}
