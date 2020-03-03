package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ListScope implements Scope {
  private final List<Referable> myContext;

  public ListScope(List<Referable> context) {
    myContext = context;
  }

  public ListScope(Referable... context) {
    myContext = Arrays.asList(context);
  }

  @NotNull
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

  @NotNull
  @Override
  public Scope getGlobalSubscope() {
    return EmptyScope.INSTANCE;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    return EmptyScope.INSTANCE;
  }
}
