package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StaticGroup implements Group {
  private final GlobalReferable myReferable;
  private final List<Group> myStaticGroups;
  private final List<SimpleNamespaceCommand> myNamespaceCommands;

  public StaticGroup(GlobalReferable referable, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands) {
    myReferable = referable;
    myStaticGroups = staticGroups;
    myNamespaceCommands = namespaceCommands;
  }

  @Nonnull
  @Override
  public GlobalReferable getReferable() {
    return myReferable;
  }

  @Nonnull
  @Override
  public List<Group> getSubgroups() {
    return myStaticGroups;
  }

  @Nonnull
  @Override
  public List<SimpleNamespaceCommand> getNamespaceCommands() {
    return myNamespaceCommands;
  }

  @Nonnull
  @Override
  public Collection<? extends GlobalReferable> getConstructors() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends Group> getDynamicSubgroups() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends GlobalReferable> getFields() {
    return Collections.emptyList();
  }
}
