package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;

import java.util.*;

public abstract class Definition extends Binding implements Abstract.Definition {
  private final Definition myParent;
  private Precedence myPrecedence;
  private Fixity myFixity;
  private Universe myUniverse;
  private boolean myHasErrors;
  private Set<Definition> myDependencies;

  public Definition(String name, Definition parent, Precedence precedence, Fixity fixity) {
    super(name);
    myParent = parent;
    myPrecedence = precedence;
    myFixity = fixity;
    myUniverse = new Universe.Type(0, Universe.Type.PROP);
    myHasErrors = true;
    myDependencies = null;
  }

  public Definition getParent() {
    return myParent;
  }

  public boolean isDescendantOf(Definition definition) {
    return this == definition || myParent != null && myParent.isDescendantOf(definition);
  }

  public Definition findChild(String name) {
    return null;
  }

  public Collection<? extends Definition> getChildren() {
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
  public Fixity getFixity() {
    return myFixity;
  }

  public void setFixity(Fixity fixity) {
    myFixity = fixity;
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

  public boolean isRelativelyStatic(Definition definition) {
    if (!isAbstract() && (myDependencies == null || myDependencies.isEmpty())) return true;
    if (definition == null) return false;
    if (definition.isDescendantOf(myParent)) return true;

    Set<Definition> dependencies = new HashSet<>(myDependencies);
    while (definition != null) {
      if (definition instanceof ClassDefinition) {
        for (List<Definition> definitions : ((ClassDefinition) definition).getFieldsMap().values()) {
          for (Definition definition1 : definitions) {
            if (definition1.isAbstract()) {
              dependencies.remove(definition1);
              if (dependencies.isEmpty()) return true;
            }
          }
        }
      }
      definition = definition.getParent();
    }
    return false;
  }

  public Set<Definition> getDependencies() {
    return myDependencies;
  }

  public void setDependencies(Set<Definition> dependencies) {
    myDependencies = dependencies;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
    return builder.toString();
  }

  public String getFullName() {
    return myParent == null || myParent.getParent() == null ? getName() : myParent.getFullName() + "." + (myFixity == Fixity.PREFIX ? getName() : "(" + getName() + ")");
  }
}
