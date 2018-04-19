package com.jetbrains.jetpad.vclang.term.group;

import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface Group {
  @Nonnull LocatedReferable getReferable();

  @Nonnull Collection<? extends Group> getSubgroups();
  @Nonnull Collection<? extends NamespaceCommand> getNamespaceCommands();

  @Nonnull Collection<? extends InternalReferable> getConstructors();

  @Nonnull Collection<? extends Group> getDynamicSubgroups();
  @Nonnull Collection<? extends InternalReferable> getFields();

  interface InternalReferable {
    LocatedReferable getReferable();
    boolean isVisible();
  }
}
