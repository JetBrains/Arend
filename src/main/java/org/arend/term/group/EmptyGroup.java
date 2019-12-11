package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.NamespaceCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  @Nonnull
  @Override
  public LocatedReferable getReferable() {
    return myReferable;
  }

  @Nonnull
  @Override
  public Collection<? extends Group> getSubgroups() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends NamespaceCommand> getNamespaceCommands() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends InternalReferable> getInternalReferables() {
    return Collections.emptyList();
  }
}
