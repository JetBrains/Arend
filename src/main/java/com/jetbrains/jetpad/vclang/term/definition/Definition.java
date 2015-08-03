package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class Definition extends Binding implements Abstract.Definition {
  private Definition myParent;
  private Precedence myPrecedence;
  private Universe myUniverse;
  private boolean myHasErrors;
  private Set<Definition> myDependencies;

  public Definition(Utils.Name name, Definition parent, Precedence precedence) {
    super(name);
    myParent = parent;
    myPrecedence = precedence;
    myUniverse = new Universe.Type(0, Universe.Type.PROP);
    myHasErrors = true;
    myDependencies = null;
  }

  public Definition getParent() {
    return myParent;
  }

  public Namespace getNamespace() {
    return null;
  }

  public void setParent(Definition parent) {
    myParent = parent;
  }

  public boolean isDescendantOf(Definition definition) {
    return this == definition || myParent != null && myParent.isDescendantOf(definition);
  }

  public Definition getStaticField(String name) {
    return null;
  }

  public Collection<? extends Definition> getStaticFields() {
    return null;
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
}
