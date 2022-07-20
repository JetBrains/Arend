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
  public List<Referable> getElements(Referable.RefKind kind) {
    return myReferable.getRefKind() == kind ? Collections.singletonList(myReferable) : Collections.emptyList();
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, Referable.RefKind kind) {
    return (kind == null || myReferable.getRefKind() == kind) && myReferable.textRepresentation().equals(name) ? myReferable : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    return myReferable.textRepresentation().equals(name) ? EmptyScope.INSTANCE : null;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    return pred.test(myReferable) ? myReferable : null;
  }
}
