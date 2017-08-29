package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class StaticGroup extends EmptyGroup {
  private final List<Group> myStaticGroups;
  private final List<SimpleNamespaceCommand> myNamespaceCommands;

  public StaticGroup(GlobalReferable referable, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands) {
    super(referable);
    myStaticGroups = staticGroups;
    myNamespaceCommands = namespaceCommands;
  }

  @Nonnull
  @Override
  public List<Group> getSubgroups() {
    return myStaticGroups;
  }

  @Nonnull
  @Override
  public List<SimpleNamespaceCommand> getNamespaceCommands() {
    return myNamespaceCommands;
  }
}
