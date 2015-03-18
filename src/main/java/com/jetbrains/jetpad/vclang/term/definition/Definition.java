package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeCheckingError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Definition implements PrettyPrintable {
  private final String name;
  private final Signature signature;
  private final int id;
  private static int idCounter = 0;

  public Definition(String name, Signature signature) {
    this.name = name;
    this.signature = signature;
    id = idCounter++;
  }

  public String getName() {
    return name;
  }

  public Signature getSignature() {
    return signature;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Definition)) return false;
    Definition other = (Definition)o;
    return other.id == id;
  }

  @Override
  public String toString() {
    return name + " : " + signature.toString();
  }

  public Definition checkTypes(Map<String, Definition> globalContext, List<TypeCheckingError> errors) {
    signature.getType().checkType(globalContext, new ArrayList<Definition>(), Expression.Universe(-1), errors);
    return this;
  }
}
