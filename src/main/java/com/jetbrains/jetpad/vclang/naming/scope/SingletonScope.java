package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class SingletonScope implements Scope {
  private final Referable myReferable;

  public SingletonScope(Referable referable) {
    myReferable = referable;
  }

  @Nonnull
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
  public Scope resolveNamespace(String name, boolean resolveModuleNames) {
    return myReferable.textRepresentation().equals(name) ? EmptyScope.INSTANCE : null;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    return pred.test(myReferable) ? myReferable : null;
  }
}
