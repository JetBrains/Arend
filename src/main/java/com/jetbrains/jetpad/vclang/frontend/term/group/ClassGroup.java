package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteClassReferable;
import com.jetbrains.jetpad.vclang.frontend.reference.InternalConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class ClassGroup extends StaticGroup {
  private final List<Group> myDynamicGroups;

  public ClassGroup(ConcreteClassReferable reference, List<Group> dynamicGroups, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    super(reference, staticGroups, namespaceCommands, parent);
    myDynamicGroups = dynamicGroups;
  }

  @Nonnull
  @Override
  public ConcreteClassReferable getReferable() {
    return (ConcreteClassReferable) super.getReferable();
  }

  @Nonnull
  @Override
  public List<Group> getDynamicSubgroups() {
    return myDynamicGroups;
  }

  @Nonnull
  @Override
  public Collection<? extends InternalConcreteGlobalReferable> getFields() {
    return getReferable().getFieldReferables();
  }
}
