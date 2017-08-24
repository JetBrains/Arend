package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.frontend.reference.ModuleReference;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class SimpleGroup implements Group {
  private final ModuleReference myReference;
  private final List<Group> myStaticGroups;
  private final List<Group> myDynamicGroups;
  private final List<SimpleNamespaceCommand> myNamespaceCommands;

  public SimpleGroup(ModuleReference reference, List<Group> staticGroups, List<Group> dynamicGroups, List<SimpleNamespaceCommand> namespaceCommands) {
    myReference = reference;
    myStaticGroups = staticGroups;
    myDynamicGroups = dynamicGroups;
    myNamespaceCommands = namespaceCommands;
  }

  @Nonnull
  @Override
  public ModuleReference getReferable() {
    return myReference;
  }

  @Nonnull
  @Override
  public List<Group> getStaticSubgroups() {
    return myStaticGroups;
  }

  @Nonnull
  @Override
  public List<Group> getDynamicSubgroups() {
    return myDynamicGroups;
  }

  @Nonnull
  @Override
  public List<SimpleNamespaceCommand> getNamespaceCommands() {
    return myNamespaceCommands;
  }

}
