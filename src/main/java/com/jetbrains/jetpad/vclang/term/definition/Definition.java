package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;

import java.util.ArrayList;

public abstract class Definition extends Binding implements Abstract.Definition {
  private final int myID;
  private static int idCounter = 0;
  private final Precedence myPrecedence;
  private final Fixity myFixity;
  private Universe myUniverse;

  public Definition(String name, Precedence precedence, Fixity fixity, Universe universe) {
    super(name);
    myID = idCounter++;
    myPrecedence = precedence;
    myFixity = fixity;
    myUniverse = universe;
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
  public boolean equals(Object other) {
    return other == this || other instanceof Definition && ((Definition) other).myID == myID;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
    return builder.toString();
  }
}
