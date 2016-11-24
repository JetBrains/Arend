package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;

public class Typecheckable {
  public Abstract.Definition definition;
  public boolean isHeader;

  public Typecheckable(Abstract.Definition definition, boolean isHeader) {
    assert !isHeader || hasHeader(definition);
    this.definition = definition;
    this.isHeader = isHeader;
  }

  public static boolean hasHeader(Abstract.Definition definition) {
    return definition instanceof Abstract.FunctionDefinition && ((Abstract.FunctionDefinition) definition).getResultType() != null || definition instanceof Abstract.DataDefinition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Typecheckable that = (Typecheckable) o;

    if (isHeader != that.isHeader) return false;
    return definition.equals(that.definition);
  }

  @Override
  public int hashCode() {
    int result = definition.hashCode();
    result = 31 * result + (isHeader ? 1 : 0);
    return result;
  }
}