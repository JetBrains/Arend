package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassViewGroup implements ChildGroup {
  private final GlobalReference myReference;
  private final List<GlobalReference> myFields;
  private final ChildGroup myParent;

  public ClassViewGroup(GlobalReference reference, List<GlobalReference> fields, ChildGroup parent) {
    myReference = reference;
    myFields = fields;
    myParent = parent;
  }

  @Nonnull
  @Override
  public GlobalReferable getReferable() {
    return myReference;
  }

  @Nonnull
  @Override
  public Collection<? extends Group> getSubgroups() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends NamespaceCommand> getNamespaceCommands() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends GlobalReferable> getConstructors() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends Group> getDynamicSubgroups() {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Collection<? extends GlobalReferable> getFields() {
    return myFields;
  }

  @Nullable
  @Override
  public ChildGroup getParentGroup() {
    return myParent;
  }
}
