package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SingletonScope implements Scope {
  private final Referable myReferable;

  public SingletonScope(Referable referable) {
    myReferable = referable;
  }

  @NotNull
  @Override
  public List<Referable> getElements() {
    return Collections.singletonList(myReferable);
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    return myReferable.textRepresentation().equals(name) ? myReferable : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    return myReferable.textRepresentation().equals(name) ? EmptyScope.INSTANCE : null;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    return pred.test(myReferable) ? myReferable : null;
  }
}
