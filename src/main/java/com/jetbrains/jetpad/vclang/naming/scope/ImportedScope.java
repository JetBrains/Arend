package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public class ImportedScope implements Scope {
  private final Group myGroup;
  private final ModuleScopeProvider myProvider;

  public ImportedScope(@Nonnull Group group, ModuleScopeProvider provider) {
    myGroup = group;
    myProvider = provider;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    return null; // TODO[abstract]
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name) {
    return null; // TODO[abstract]
  }
}
