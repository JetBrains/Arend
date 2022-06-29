package org.arend.term.group;

import org.arend.naming.reference.ClassReferable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ClassGroup extends StaticGroup {
  private final List<Group> myDynamicGroups;
  private final List<? extends InternalReferable> myInternalGlobalReferables;

  public ClassGroup(ClassReferable reference, List<? extends InternalReferable> internalGlobalReferables, List<Group> dynamicGroups, List<Statement> statements, ChildGroup parent) {
    super(reference, statements, parent);
    myInternalGlobalReferables = internalGlobalReferables;
    myDynamicGroups = dynamicGroups;
  }

  @NotNull
  @Override
  public ClassReferable getReferable() {
    return (ClassReferable) super.getReferable();
  }

  @NotNull
  @Override
  public List<Group> getDynamicSubgroups() {
    return myDynamicGroups;
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getInternalReferables() {
    return myInternalGlobalReferables;
  }

  @NotNull
  @Override
  public Collection<? extends InternalReferable> getFields() {
    return myInternalGlobalReferables;
  }
}
