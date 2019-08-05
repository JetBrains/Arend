package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.ChildNamespaceCommand;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class DataGroup extends StaticGroup {
  private final List<? extends InternalReferable> myConstructors;

  public DataGroup(LocatedReferable referable, List<? extends InternalReferable> constructors, List<Group> staticGroups, List<ChildNamespaceCommand> namespaceCommands, ChildGroup parent) {
    super(referable, staticGroups, namespaceCommands, parent);
    myConstructors = constructors;
  }

  @Nonnull
  @Override
  public Collection<? extends InternalReferable> getInternalReferables() {
    return myConstructors;
  }

  @Nonnull
  @Override
  public Collection<? extends InternalReferable> getConstructors() {
    return myConstructors;
  }
}
