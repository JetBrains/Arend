package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.definition.visitor.PrettyPrintVisitor;

import java.util.ArrayList;
import java.util.List;

public abstract class Definition extends Binding implements PrettyPrintable, Abstract.Definition {
  protected final int myID;
  private static int idCounter = 0;
  private final Precedence myPrecedence;
  private final Fixity myFixity;
  private final Universe myUniverse;

  protected Definition(int id, String name, Precedence precedence, Fixity fixity, Universe universe) {
    super(name);
    myID = id;
    myPrecedence = precedence;
    myFixity = fixity;
    myUniverse = universe;
  }

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

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Definition)) return false;
    Definition other = (Definition)o;
    return other.myID == myID;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
    return builder.toString();
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    accept(new PrettyPrintVisitor(builder, names), prec);
  }
}
