package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;

import java.util.ArrayList;

public abstract class Definition extends Binding implements Abstract.Definition {
  private final Definition myParent;
  private Precedence myPrecedence;
  private Fixity myFixity;
  private Universe myUniverse;
  private boolean myHasErrors;

  public Definition(String name, Definition parent, Precedence precedence, Fixity fixity) {
    super(name);
    myParent = parent;
    myPrecedence = precedence;
    myFixity = fixity;
    myUniverse = new Universe.Type(0, Universe.Type.PROP);
    myHasErrors = true;
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

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
    return builder.toString();
  }

  public String getFullName() {
    return myParent == null || myParent.getParent() == null ? getName() : myParent.getFullName() + "." + getName();
  }
}
