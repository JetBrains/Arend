package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

public class EmptyGroup implements Group {
  private final GlobalReferable myReferable;

  public EmptyGroup(GlobalReferable referable) {
    myReferable = referable;
  }

  @Nonnull
  @Override
  public GlobalReferable getReferable() {
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
