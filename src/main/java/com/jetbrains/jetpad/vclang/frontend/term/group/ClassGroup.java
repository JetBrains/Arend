package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class ClassGroup extends StaticGroup {
  private final List<Group> myDynamicGroups;
  private final List<ConcreteGlobalReferable> myFields;

  public ClassGroup(ConcreteGlobalReferable reference, List<Group> dynamicGroups, List<ConcreteGlobalReferable> fields, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    super(reference, staticGroups, namespaceCommands, parent);
    myDynamicGroups = dynamicGroups;
    myFields = fields;
  }

  @Nonnull
  @Override
  public List<Group> getDynamicSubgroups() {
    return myDynamicGroups;
  }

  @Nonnull
  @Override
  public List<ConcreteGlobalReferable> getFields() {
    return myFields;
  }
}
