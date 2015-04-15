package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Definition extends Binding implements PrettyPrintable {
  private final int myID;
  private static int idCounter = 0;
  private final Precedence myPrecedence;
  private final Fixity myFixity;

  public enum Arrow { LEFT, RIGHT }

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

  public Definition(String name, Precedence precedence, Fixity fixity) {
    super(name);
    myID = idCounter++;
    myPrecedence = precedence;
    myFixity = fixity;
  }

  public Definition(String name, Fixity fixity) {
    this(name, new Precedence(Associativity.RIGHT_ASSOC, (byte) 10), fixity);
  }

  public Definition(String name) {
    this(name, Fixity.PREFIX);
  }

  public Precedence getPrecedence() {
    return myPrecedence;
  }

  public Fixity getFixity() {
    return myFixity;
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
    prettyPrint(builder, new ArrayList<String>(), (byte) 0);
    return builder.toString();
  }

  public abstract Definition checkTypes(Map<String, Definition> globalContext, List<Binding> localContext, List<TypeCheckingError> errors);
}
