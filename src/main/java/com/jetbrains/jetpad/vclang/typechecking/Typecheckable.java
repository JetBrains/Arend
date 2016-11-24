package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class Typecheckable {
  private final Abstract.Definition myDefinition;
  private final boolean myHeader;

  public Typecheckable(Abstract.Definition definition, boolean isHeader) {
    assert !isHeader || hasHeader(definition);
    this.myDefinition = definition;
    this.myHeader = isHeader;
  }

  public Abstract.Definition getDefinition() {
    return myDefinition;
  }

  public boolean isHeader() {
    return myHeader;
  }

  public static boolean hasHeader(Abstract.Definition definition) {
    return definition instanceof Abstract.FunctionDefinition && ((Abstract.FunctionDefinition) definition).getResultType() != null || definition instanceof Abstract.DataDefinition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Typecheckable that = (Typecheckable) o;

    if (myHeader != that.myHeader) return false;
    return myDefinition.equals(that.myDefinition);
  }

  @Override
  public int hashCode() {
    int result = myDefinition.hashCode();
    result = 31 * result + (myHeader ? 1 : 0);
    return result;
  }
}