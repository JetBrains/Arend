package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.PrettyPrintable;

import java.util.ArrayList;

public abstract class Definition extends Binding implements PrettyPrintable {
  protected final int myID;
  private static int idCounter = 0;
  private final Precedence myPrecedence;
  private final Fixity myFixity;
  private final Universe myUniverse;

  public enum Fixity { PREFIX, INFIX }
  public enum Associativity { LEFT_ASSOC, RIGHT_ASSOC, NON_ASSOC }

  public static class Precedence {
    public Associativity associativity;
    public byte priority;

    public Precedence(Associativity associativity, byte priority) {
      this.associativity = associativity;
      this.priority = priority;
    }

    @Override
    public String toString() {
      String result = "infix";
      if (associativity == Associativity.LEFT_ASSOC) {
        result += "l";
      }
      if (associativity == Associativity.RIGHT_ASSOC) {
        result += "r";
      }
      return result + " " + priority;
    }
  }

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

  public Definition(String name, Fixity fixity, Universe universe) {
    this(name, new Precedence(Associativity.RIGHT_ASSOC, (byte) 10), fixity, universe);
  }

  public Definition(String name, Universe universe) {
    this(name, Fixity.PREFIX, universe);
  }

  public Precedence getPrecedence() {
    return myPrecedence;
  }

  public Fixity getFixity() {
    return myFixity;
  }

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
}
