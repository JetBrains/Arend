package com.jetbrains.jetpad.vclang.typechecking.typecheckable;

import com.jetbrains.jetpad.vclang.term.Concrete;

public class Typecheckable {
  private final Concrete.Definition myDefinition;
  private final boolean myHeader;

  public Typecheckable(Concrete.Definition definition, boolean isHeader) {
    assert !isHeader || hasHeader(definition);
    this.myDefinition = definition;
    this.myHeader = isHeader;
  }

  public Concrete.Definition getDefinition() {
    return myDefinition;
  }

  public boolean isHeader() {
    return myHeader;
  }

  public static boolean hasHeader(Concrete.Definition definition) {
    return definition instanceof Concrete.FunctionDefinition || definition instanceof Concrete.DataDefinition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Typecheckable that = (Typecheckable) o;
    return myHeader == that.myHeader && myDefinition.getReferable().equals(that.myDefinition.getReferable());
  }

  @Override
  public int hashCode() {
    int result = myDefinition.getReferable().hashCode();
    result = 31 * result + (myHeader ? 1 : 0);
    return result;
  }
}