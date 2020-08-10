package org.arend.term.concrete;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ConcreteSubstitution {
  private final Map<Referable, Concrete.Expression> mySubstitution;

  public ConcreteSubstitution(@NotNull Map<Referable, Concrete.Expression> substitution) {
    mySubstitution = substitution;
  }

  public ConcreteSubstitution() {
    this(new HashMap<>());
  }

  public void bind(@NotNull Referable referable, @Nullable Concrete.Expression expression) {
    mySubstitution.put(referable, expression);
  }

  public void unbind(@NotNull Referable referable) {
    mySubstitution.remove(referable);
  }

  public int size() {
    return mySubstitution.size();
  }
}
