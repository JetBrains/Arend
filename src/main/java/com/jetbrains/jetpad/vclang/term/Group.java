package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface Group {
  @Nonnull GlobalReferable getReferable();

  @Nonnull Collection<? extends Group> getSubgroups();
  @Nonnull Collection<? extends NamespaceCommand> getNamespaceCommands();

  @Nonnull Collection<? extends GlobalReferable> getConstructors();

  @Nonnull Collection<? extends Referable> getSuperClassReferences();
  @Nonnull Collection<? extends Group> getDynamicSubgroups();
  @Nonnull Collection<? extends GlobalReferable> getFields();
}
