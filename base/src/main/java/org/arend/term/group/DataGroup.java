package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.ChildNamespaceCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class DataGroup extends StaticGroup {
  private final List<? extends InternalReferable> myConstructors;

  public DataGroup(LocatedReferable referable, List<? extends InternalReferable> constructors, List<Group> staticGroups, List<ChildNamespaceCommand> namespaceCommands, ChildGroup parent) {
    super(referable, staticGroups, namespaceCommands, parent);
    myConstructors = constructors;
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getInternalReferables() {
    return myConstructors;
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getConstructors() {
    return myConstructors;
  }
}
