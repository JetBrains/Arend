package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import java.util.List;

public class ClassGroup extends StaticGroup {
  private final List<Referable> mySuperClassReferences;
  private final List<Group> myDynamicGroups;
  private final List<GlobalReference> myFields;

  public ClassGroup(GlobalReference reference, List<Referable> superClassReferences, List<Group> dynamicGroups, List<GlobalReference> fields, List<Group> staticGroups, List<SimpleNamespaceCommand> namespaceCommands) {
    super(reference, staticGroups, namespaceCommands);
    mySuperClassReferences = superClassReferences;
    myDynamicGroups = dynamicGroups;
    myFields = fields;
  }

  @Nonnull
  @Override
  public List<Referable> getSuperClassReferences() {
    return mySuperClassReferences;
  }

  @Nonnull
  @Override
  public List<Group> getDynamicSubgroups() {
    return myDynamicGroups;
  }

  @Nonnull
  @Override
  public List<GlobalReference> getFields() {
    return myFields;
  }
}
