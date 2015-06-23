package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private final Map<String, List<Definition>> myFields = new HashMap<>();
  private final Map<String, Definition> myExports = new HashMap<>();

  public ClassDefinition(String name, Definition parent) {
    super(name, parent, DEFAULT_PRECEDENCE, Fixity.PREFIX);
    hasErrors(false);
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public List<Definition> getFields() {
    List<Definition> fields = new ArrayList<>(myFields.size());
    for (List<Definition> definitions : myFields.values()) {
      for (Definition definition : definitions) {
        if (definition.getParent() == this) {
          fields.add(definition);
        }
      }
    }
    return fields;
  }

  public List<Definition> getFields(String name) {
    return myFields.get(name);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  public Definition add(Definition definition) {
    return add(definition, definition.getParent() == this || definition instanceof Constructor && definition.getParent() != null && definition.getParent().getParent() == this);
  }

  public Definition add(Definition definition, boolean export) {
    List<Definition> children = myFields.get(definition.getName());
    if (children == null) {
      children = new ArrayList<>(1);
      children.add(definition);
      myFields.put(definition.getName(), children);
    } else {
      children.add(definition);
    }

    if (export) {
      Definition oldDef = myExports.get(definition.getName());
      if (oldDef == null) {
        myExports.put(definition.getName(), definition);
      } else {
        return oldDef == definition ? null : oldDef;
      }
    }

    return null;
  }

  public void remove(Definition definition) {
    myExports.remove(definition.getName());
    List<Definition> definitions = myFields.get(definition.getName());
    if (definitions != null) {
      definitions.remove(definition);
    }
  }

  @Override
  public Definition findChild(String name) {
    return myExports.get(name);
  }
}
