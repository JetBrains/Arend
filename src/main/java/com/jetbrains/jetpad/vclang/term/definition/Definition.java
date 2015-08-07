package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleError;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public abstract class Definition extends Binding implements Abstract.Definition {
  private Definition myParent;
  private Precedence myPrecedence;
  private Universe myUniverse;
  private boolean myHasErrors;
  private Set<Definition> myDependencies;
  private Map<String, Definition> myStaticFields;
  private Map<String, Definition> myPrivateFields;

  public Definition(Utils.Name name, Definition parent, Precedence precedence) {
    super(name);
    myParent = parent;
    myPrecedence = precedence;
    myUniverse = new Universe.Type(0, Universe.Type.PROP);
    myHasErrors = true;
    myDependencies = null;
    myStaticFields = new HashMap<>();
    myPrivateFields = new HashMap<>();
  }

  public Definition getParent() {
    return myParent;
  }

  public void setParent(Definition parent) {
    myParent = parent;
  }

  public boolean isDescendantOf(Definition definition) {
    return this == definition || myParent != null && myParent.isDescendantOf(definition);
  }

  public Definition getStaticField(String name) {
    return myStaticFields.get(name);
  }

  public Collection<Definition> getStaticFields() {
    return myStaticFields.values();
  }

  protected boolean addStaticField(Definition definition, List<ModuleError> errors) {
    if (getStaticFields().contains(definition))
      return true;
    if (getStaticField(definition.getName().name) != null) {
      errors.add(new ModuleError(getEnclosingModule(), "Name " + getFullNestedMemberName(definition.getName().name) + " is already defined"));
      return false;
    }
    myStaticFields.put(definition.getName().name, definition);
    return true;
  }

 public Definition getPrivateField(String name) {
    return myPrivateFields.get(name);
  }

  public Collection<Definition> getPrivateFields() {
    return myPrivateFields.values();
  }


  public void addPrivateField(Definition definition) {
    myPrivateFields.put(definition.getName().name, definition);
  }

  public void removePrivateField(Definition definition) {
    myPrivateFields.values().remove(definition);
  }

  public boolean addField(Definition definition, List<ModuleError> errors) {
    throw new IllegalStateException("Adding fields is not supported.");
  }

  public Definition getField(String name) {
    throw new IllegalStateException("Getting fields is not supported.");
  }

  public Collection<Definition> getFields() {
    throw new IllegalStateException("Getting fields is not supported.");
  }

  public ClassDefinition getClass(String name, List<ModuleError> errors) {
    Definition definition = getField(name);
    if (definition != null) {
      if (definition instanceof ClassDefinition && definition.getParent() == this) {
        ((ClassDefinition) definition).reopen();
        addPrivateField(definition);
        return (ClassDefinition) definition;
      } else {
        errors.add(new ModuleError(getEnclosingModule(), "Name " + getFullNestedMemberName(name) + " is already defined"));
        return null;
      }
    }

    ClassDefinition result = new ClassDefinition(name, this, !(this instanceof ClassDefinition) || ((ClassDefinition) this).isLocal());
    addPrivateField(result);
    result.hasErrors(true);
    return result;
  }

  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
  }

  public void setPrecedence(Precedence precedence) {
    myPrecedence = precedence;
  }

  @Override
  public Universe getUniverse() {
    return myUniverse;
  }

  public void setUniverse(Universe universe) {
    myUniverse = universe;
  }

  public boolean hasErrors() {
    return myHasErrors;
  }

  public void hasErrors(boolean has) {
    myHasErrors = has;
  }

  public boolean isAbstract() {
    return false;
  }

  public boolean canOpen(Definition definition) {
    if (definition.getDependencies() == null)
      return true;
    for (Definition dependency : definition.getDependencies()) {
      if (!getFields().contains(dependency) && !(getDependencies() != null && getDependencies().contains(dependency))) {
        return false;
      }
    }
    return true;
  }

  public Set<Definition> getDependencies() {
    return myDependencies;
  }

  public void setDependencies(Set<Definition> dependencies) {
    myDependencies = dependencies;
  }

  public void addDependency(Definition dependency) {
    if (myDependencies == null) {
      myDependencies = new HashSet<>();
    }
    myDependencies.add(dependency);
  }

  public void updateDependencies(Definition definition) {
    if (definition.getDependencies() != null) {
      for (Definition dependency : definition.getDependencies()) {
        addDependency(dependency);
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
    return builder.toString();
  }

  public String getFullName() {
    return myParent == null || myParent.getParent() == null ? getName().getPrefixName() : myParent.getFullName() + "." + getName();
  }

  @Override
  public Definition lift(int on) {
    return this;
  }

  public Module getEnclosingModule() {
    for (Definition def = this;; def = def.getParent()) {
      if (def instanceof ClassDefinition && !((ClassDefinition) def).isLocal())
        return new Module((ClassDefinition)def.getParent(), def.getName().name);
    }
  }

  public String getFullNestedMemberName(String name) {
    String result = name;
    for (Definition def = this; !(def instanceof ClassDefinition) || ((ClassDefinition) def).isLocal(); def = def.getParent()) {
      result = def.getName() + "." + result;
    }
    return result;
  }
}
