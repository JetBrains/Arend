package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.ModuleError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.*;

public class ClassDefinition extends Definition implements Abstract.ClassDefinition {
  private Map<String, Definition> myPublicFields;
  private boolean myIsLocal;

  public ClassDefinition(String name, Definition parent) {
    this(name, parent, false);
  }

  public ClassDefinition(String name, Definition parent, boolean isLocal) {
    super(new Utils.Name(name, Fixity.PREFIX), parent, DEFAULT_PRECEDENCE);
    myIsLocal = isLocal;
    myPublicFields = new HashMap<>();
    hasErrors(false);
  }

  public boolean isLocal() {
    return myIsLocal;
  }

  @Override
  public Expression getType() {
    return new UniverseExpression(getUniverse());
  }

  @Override
  public boolean addField(Definition definition, List<ModuleError> errors) {
    if (getFields().contains(definition))
      return true;
    if (getField(definition.getName().name) != null) {
      errors.add(new ModuleError(getEnclosingModule(), "Name " + getFullNestedMemberName(definition.getName().name) + " is already defined"));
      return false;
    }

    myPublicFields.put(definition.getName().name, definition);
    updateDependencies(definition);
    addPrivateField(definition);

    if (definition.isAbstract()) {
      Universe max = getUniverse().max(definition.getUniverse());
      if (max == null) {
        String msg = "Universe " + definition.getUniverse() + " of the field " + getFullNestedMemberName(definition.getName().getPrefixName()) + "is not compatible with universe " + getUniverse() + " of previous fields";
        errors.add(new ModuleError(getEnclosingModule(), msg));
        return false;
      }
      setUniverse(max);
      return true;
    }

    if (isStatic(definition))
      addStaticField(definition, errors);
    return true;
  }

  @Override
  public Definition getField(String name) {
    return myPublicFields.get(name);
  }

  @Override
  public Collection<Definition> getFields() {
    return myPublicFields.values();
  }

  private boolean isStatic(Definition field) {
    boolean isStatic = true;
    if (field.getDependencies() != null) {
      for (Definition dependency : field.getDependencies()) {
        if (myPublicFields.values().contains(dependency)) {
          isStatic = false;
        }
      }
    }
    return isStatic;
  }

  public void reopen() {
    getPrivateFields().clear();
    for (Definition field : myPublicFields.values()) {
      addPrivateField(field);
    }
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClass(this, params);
  }

  public boolean hasAbstracts() {
    for (Definition field : myPublicFields.values()) {
      if (field.isAbstract()) return true;
    }
    return false;
  }

  @Override
  public void updateDependencies(Definition definition) {
    if (definition.getDependencies() != null) {
      for (Definition dependency : definition.getDependencies()) {
        if (!myPublicFields.values().contains(dependency)) {
          addDependency(dependency);
        }
      }
    }
  }
}
