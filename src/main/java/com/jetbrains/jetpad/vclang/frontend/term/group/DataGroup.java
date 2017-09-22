package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class DataGroup extends StaticGroup {
  private final List<GlobalReference> myConstructors;

  public DataGroup(GlobalReferable referable, List<GlobalReference> constructors, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands) {
    super(referable, staticGroups, namespaceCommands);
    myConstructors = constructors;
  }

  @Nonnull
  @Override
  public List<GlobalReference> getConstructors() {
    return myConstructors;
  }
}
