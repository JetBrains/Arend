package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import java.util.*;
import java.util.function.Predicate;

public class ListScope implements Scope {
  private final List<Referable> myContext;

  public ListScope(List<Referable> context) {
    myContext = context;
  }

  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>(myContext);
    Collections.reverse(elements);
    return elements;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (int i = myContext.size() - 1; i >= 0; i--) {
      if (pred.test(myContext.get(i))) {
        return myContext.get(i);
      }
    }
    return null;
  }
}
