package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class StaticGroup implements ChildGroup {
  private final LocatedReferable myReferable;
  private final List<Group> myStaticGroups;
  private final List<SimpleNamespaceCommand> myNamespaceCommands;
  private final ChildGroup myParent;

  public StaticGroup(LocatedReferable referable, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    myReferable = referable;
    myStaticGroups = staticGroups;
    myNamespaceCommands = namespaceCommands;
    myParent = parent;
  }

  @Nonnull
  @Override
  public LocatedReferable getReferable() {
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
  public Collection<? extends InternalReferable> getInternalReferables() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public ChildGroup getParentGroup() {
    return myParent;
  }
}
