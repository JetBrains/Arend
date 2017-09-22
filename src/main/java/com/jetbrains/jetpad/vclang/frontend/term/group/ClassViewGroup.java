package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassViewGroup implements Group {
  private final GlobalReference myReference;
  private final List<GlobalReference> myFields;

  public ClassViewGroup(GlobalReference reference, List<GlobalReference> fields) {
    myReference = reference;
    myFields = fields;
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
}
