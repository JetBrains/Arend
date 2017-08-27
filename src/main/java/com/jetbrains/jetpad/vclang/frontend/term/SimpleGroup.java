package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class SimpleGroup extends StaticGroup {
  private final List<Group> myDynamicGroups;

  public SimpleGroup(GlobalReference reference, List<Group> staticGroups, List<Group> dynamicGroups, List<SimpleNamespaceCommand> namespaceCommands) {
    super(reference, staticGroups, namespaceCommands);
    myDynamicGroups = dynamicGroups;
  }

  @Nonnull
  @Override
  public List<Group> getDynamicSubgroups() {
    return myDynamicGroups;
  }
}
