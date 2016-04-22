package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class ClassDefinition extends Definition {
  private Map<String, ClassField> myFields = null;

  public ClassDefinition(ResolvedName rn) {
    super(rn, Abstract.Definition.DEFAULT_PRECEDENCE);
    super.hasErrors(false);
  }

  public ClassDefinition(ResolvedName rn, TypeUniverseNew universe) {
    super(rn, Abstract.Definition.DEFAULT_PRECEDENCE, universe);
    super.hasErrors(false);
  }

  @Override
  public Expression getType() {
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
    myFields.put(field.getName(), field);
    field.setThisClass(this);
  }

  public ClassField removeField(String name) {
    return myFields != null ? myFields.remove(name) : null;
  }

  public void removeField(ClassField field) {
    if (myFields != null) {
      myFields.remove(field.getName());
    }
  }

  public ClassField getParentField() {
    return getField("\\parent");
  }

  public void addParentField(ClassDefinition parentClass) {
    setThisClass(parentClass);
    ClassField field = new ClassField(getResolvedName().toNamespace().getChild("\\parent").getResolvedName(), Abstract.Definition.DEFAULT_PRECEDENCE, ClassCall(parentClass), this, param("\\this", ClassCall(this)));
    addField(field);
    getResolvedName().toNamespace().addDefinition(field);
  }

  @Override
  public Expression getTypeWithThis() {
    Expression type = getType();
    if (getThisClass() != null) {
      type = Pi(getThisClass().getDefCall(), type);
    }
    return type;
  }
}
