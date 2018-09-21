package org.arend.typechecking.typecheckable;

import org.arend.term.concrete.Concrete;

public class TypecheckingUnit {
  private final Concrete.Definition myDefinition;
  private final boolean myHeader;

  public TypecheckingUnit(Concrete.Definition definition, boolean isHeader) {
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

    TypecheckingUnit that = (TypecheckingUnit) o;
    return myHeader == that.myHeader && myDefinition.getData().equals(that.myDefinition.getData());
  }

  @Override
  public int hashCode() {
    int result = myDefinition.getData().hashCode();
    result = 31 * result + (myHeader ? 1 : 0);
    return result;
  }
}