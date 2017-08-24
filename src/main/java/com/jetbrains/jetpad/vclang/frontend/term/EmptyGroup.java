package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

public class EmptyGroup implements Group {
  private final GlobalReference myReferable;

  public EmptyGroup(GlobalReference referable) {
    myReferable = referable;
  }

  @Nonnull
  @Override
  public GlobalReference getReferable() {
    return myReferable;
  }

  @Nonnull
  @Override
  public Collection<? extends Group> getStaticSubgroups() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends Group> getDynamicSubgroups() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends NamespaceCommand> getNamespaceCommands() {
    return Collections.emptyList();
  }
}
