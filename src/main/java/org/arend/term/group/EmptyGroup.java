package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.NamespaceCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class EmptyGroup implements ChildGroup {
  private final LocatedReferable myReferable;
  private final ChildGroup myParent;

  public EmptyGroup(LocatedReferable referable, ChildGroup parent) {
    myReferable = referable;
    myParent = parent;
  }

  @Nullable
  @Override
  public ChildGroup getParentGroup() {
    return myParent;
  }

  @NotNull
  @Override
  public LocatedReferable getReferable() {
    return myReferable;
  }

  @NotNull
  @Override
  public Collection<? extends Group> getSubgroups() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends NamespaceCommand> getNamespaceCommands() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getInternalReferables() {
    return Collections.emptyList();
  }
}
