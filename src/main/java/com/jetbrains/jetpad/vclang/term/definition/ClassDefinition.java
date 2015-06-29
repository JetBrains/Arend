package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;

import java.util.*;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private final Map<String, List<Definition>> myFields = new HashMap<>();
  private final Map<String, Definition> myExports = new HashMap<>();
  private final Map<String, FunctionDefinition> myAbstracts = new HashMap<>();

  public ClassDefinition(String name, Definition parent) {
    super(name, parent, DEFAULT_PRECEDENCE, Fixity.PREFIX);
    hasErrors(false);
  }

  public Map<String, FunctionDefinition> getAbstracts() {
    return myAbstracts;
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

  public Map<String, List<Definition>> getFieldsMap() {
    return myFields;
  }

  public List<Definition> getFields(String name) {
    return myFields.get(name);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  public boolean add(Definition definition, List<ModuleError> errors) {
    return add(definition, definition.getParent() == this || definition instanceof Constructor && definition.getParent() != null && definition.getParent().getParent() == this, errors);
  }

  public boolean add(Definition definition, boolean export, List<ModuleError> errors) {
    if (definition instanceof FunctionDefinition && definition.isAbstract()) {
      Universe max = getUniverse().max(definition.getUniverse());
      if (max == null) {
        String msg = "Universe " + definition.getUniverse() + " of the field is not compatible with universe " + getUniverse() + " of previous fields";
        errors.add(new ModuleError(new Module(this, definition.getName()), msg));
        return false;
      }

      Definition old = myAbstracts.putIfAbsent(definition.getName(), (FunctionDefinition) definition);
      if (old != null) {
        errors.add(new ModuleError(new Module(this, definition.getName()), "Name is already defined"));
        return false;
      }

      setUniverse(max);
    }

    List<Definition> children = myFields.get(definition.getName());
    if (children == null) {
      children = new ArrayList<>(1);
      children.add(definition);
      myFields.put(definition.getName(), children);
    } else {
      if (!children.contains(definition)) {
        children.add(definition);
      }
    }

    return !export || addExport(definition, errors);
  }

  public boolean addExport(Definition definition, List<ModuleError> errors) {
    Definition old = myExports.putIfAbsent(definition.getName(), definition);
    if (old != null) {
      errors.add(new ModuleError(new Module(this, definition.getName()), "Name is already exported"));
      return false;
    } else {
      return true;
    }
  }

  public void remove(Definition definition) {
    myExports.remove(definition.getName());
    List<Definition> definitions = myFields.get(definition.getName());
    if (definitions != null) {
      definitions.remove(definition);
    }
    if (definition instanceof FunctionDefinition && definition.isAbstract()) {
      myAbstracts.remove(definition.getName());
    }
  }

  @Override
  public Definition findChild(String name) {
    return myExports.get(name);
  }

  @Override
  public Collection<Definition> getChildren() {
    return myExports.values();
  }
}
