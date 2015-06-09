package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;

import java.util.ArrayList;

public abstract class Definition extends Binding implements Abstract.Definition {
  private final Module myModule;
  private final Precedence myPrecedence;
  private final Fixity myFixity;
  private Universe myUniverse;

  public Definition(String name, Module module, Precedence precedence, Fixity fixity, Universe universe) {
    super(name);
    myModule = module;
    myPrecedence = precedence;
    myFixity = fixity;
    myUniverse = universe;
  }

  public Module getModule() {
    return myModule;
  }

  @Override
  public Precedence getPrecedence() {
    return myPrecedence;
  }

  @Override
  public Fixity getFixity() {
    return myFixity;
  }

  @Override
  public Universe getUniverse() {
    return myUniverse;
  }

  public void setUniverse(Universe universe) {
    myUniverse = universe;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
    return builder.toString();
  }
}
