package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.frontend.reference.InternalConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class DataGroup extends StaticGroup {
  private final List<InternalConcreteGlobalReferable> myConstructors;

  public DataGroup(GlobalReferable referable, List<InternalConcreteGlobalReferable> constructors, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands, ChildGroup parent) {
    super(referable, staticGroups, namespaceCommands, parent);
    myConstructors = constructors;
  }

  @Nonnull
  @Override
  public List<InternalConcreteGlobalReferable> getConstructors() {
    return myConstructors;
  }
}
