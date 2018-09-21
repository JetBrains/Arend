package org.arend.term.group;

import org.arend.naming.reference.ClassReferable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class ClassGroup extends StaticGroup {
  private final List<Group> myDynamicGroups;
  private final List<? extends InternalReferable> myInternalGlobalReferables;

  public ClassGroup(ClassReferable reference, List<? extends InternalReferable> internalGlobalReferables, List<Group> dynamicGroups, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    super(reference, staticGroups, namespaceCommands, parent);
    myInternalGlobalReferables = internalGlobalReferables;
    myDynamicGroups = dynamicGroups;
  }

  @Nonnull
  @Override
  public ClassReferable getReferable() {
    return (ClassReferable) super.getReferable();
  }

  @Nonnull
  @Override
  public List<Group> getDynamicSubgroups() {
    return myDynamicGroups;
  }

  @Nonnull
  @Override
  public Collection<? extends InternalReferable> getFields() {
    return myInternalGlobalReferables;
  }
}
