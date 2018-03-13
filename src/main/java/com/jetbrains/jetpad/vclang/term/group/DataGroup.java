package com.jetbrains.jetpad.vclang.term.group;

import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class DataGroup extends StaticGroup {
  private final List<? extends InternalReferable> myConstructors;

  public DataGroup(LocatedReferable referable, List<? extends InternalReferable> constructors, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    super(referable, staticGroups, namespaceCommands, parent);
    myConstructors = constructors;
  }

  @Nonnull
  @Override
  public Collection<? extends InternalReferable> getConstructors() {
    return myConstructors;
  }
}
