package org.arend.naming.scope;

import org.arend.ext.reference.ArendRef;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ListScope implements Scope {
  private final List<? extends ArendRef> myContext;

  public ListScope(List<? extends ArendRef> context) {
    myContext = context;
  }

  public ListScope(Referable... context) {
    myContext = Arrays.asList(context);
  }

  @NotNull
  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>(myContext.size());
    for (int i = myContext.size() - 1; i >= 0; i--) {
      if (myContext.get(i) instanceof Referable) {
        elements.add((Referable) myContext.get(i));
      }
    }
    return elements;
  }

  @Override
  public Referable find(Predicate<Referable> pred) {
    for (int i = myContext.size() - 1; i >= 0; i--) {
      if (myContext.get(i) instanceof Referable && pred.test((Referable) myContext.get(i))) {
        return (Referable) myContext.get(i);
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
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    return EmptyScope.INSTANCE;
  }
}
