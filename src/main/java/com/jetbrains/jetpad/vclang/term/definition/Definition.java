package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.PrettyPrintable;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Universe;

public abstract class Definition extends Binding implements PrettyPrintable {
  private final int myID;
  private static int idCounter = 0;

  public Definition(String name, Signature signature) {
    super(name, signature);
    myID = idCounter++;
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
    return getName() + " : " + getSignature();
  }

  public Definition checkTypes(Map<String, Definition> globalContext, List<TypeCheckingError> errors) {
    getSignature().getType().checkType(globalContext, new ArrayList<Binding>(), Universe(-1), errors);
    return this;
  }
}
