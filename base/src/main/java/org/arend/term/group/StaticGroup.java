package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.ChildNamespaceCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StaticGroup implements ChildGroup {
  private final LocatedReferable myReferable;
  private final List<Group> myStaticGroups;
  private final List<ChildNamespaceCommand> myNamespaceCommands;
  private final ChildGroup myParent;

  public StaticGroup(LocatedReferable referable, List<Group> staticGroups, List<ChildNamespaceCommand> namespaceCommands, ChildGroup parent) {
    myReferable = referable;
    myStaticGroups = staticGroups;
    myNamespaceCommands = namespaceCommands;
    myParent = parent;
  }

  @NotNull
  @Override
  public LocatedReferable getReferable() {
    return myReferable;
  }

  @NotNull
  @Override
  public List<Group> getSubgroups() {
    return myStaticGroups;
  }

  @NotNull
  @Override
  public List<ChildNamespaceCommand> getNamespaceCommands() {
    return myNamespaceCommands;
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getInternalReferables() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public ChildGroup getParentGroup() {
    return myParent;
  }
}
