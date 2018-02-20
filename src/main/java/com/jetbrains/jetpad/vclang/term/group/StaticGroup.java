package com.jetbrains.jetpad.vclang.term.group;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StaticGroup implements ChildGroup {
  private final GlobalReferable myReferable;
  private final List<Group> myStaticGroups;
  private final List<SimpleNamespaceCommand> myNamespaceCommands;
  private final ChildGroup myParent;

  public StaticGroup(GlobalReferable referable, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    myReferable = referable;
    myStaticGroups = staticGroups;
    myNamespaceCommands = namespaceCommands;
    myParent = parent;
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
  public Collection<? extends InternalReferable> getConstructors() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends Group> getDynamicSubgroups() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends InternalReferable> getFields() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public ChildGroup getParentGroup() {
    return myParent;
  }
}
