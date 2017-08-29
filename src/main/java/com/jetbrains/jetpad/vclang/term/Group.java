package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface Group {
  @Nonnull GlobalReferable getReferable();
  @Nonnull Collection<? extends Group> getSubgroups();
  @Nonnull Collection<? extends Group> getDynamicSubgroups();
  @Nonnull Collection<? extends NamespaceCommand> getNamespaceCommands();
}
