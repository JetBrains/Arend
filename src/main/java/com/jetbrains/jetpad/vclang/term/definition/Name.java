package com.jetbrains.jetpad.vclang.term.definition;

public class Name {
  public enum Fixity { PREFIX, INFIX }

  public String name;
  public Fixity fixity;

  public Name(String name) {
    this.name = name;
    this.fixity = name.isEmpty() || name.charAt(0) == '_' || Character.isLetter(name.charAt(0)) || name.charAt(0) == '?' || name.charAt(0) == '\\' || Character.isLetter(name.charAt(0)) ? Fixity.PREFIX : Fixity.INFIX;
  }

  public String getPrefixName() {
    return fixity == Fixity.PREFIX ? name : "(" + name + ")";
  }

  public String getInfixName() {
    return fixity == Fixity.PREFIX ? "`" + name + "`" : name;
  }

  @Override
  public String toString() {
    return getPrefixName();
  }

  @Override
  public boolean equals(Object other) {
    return other == this || other instanceof Name && ((Name) other).name.equals(name) && fixity == ((Name) other).fixity;
  }

  @Override
  public int hashCode() {
    return name.hashCode() + fixity.hashCode();
  }
}
