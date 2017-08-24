package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public interface Group {
  @Nonnull GlobalReferable getReferable();
  @Nonnull Collection<? extends Group> getStaticSubgroups();
  @Nonnull Collection<? extends Group> getDynamicSubgroups();
  @Nonnull Collection<? extends NamespaceCommand> getNamespaceCommands();

  interface NamespaceCommand {
    enum Kind { OPEN, EXPORT }
    @Nonnull Kind getKind();
    @Nonnull Referable getGroupReference();
    @Nullable Collection<? extends Referable> getSubgroupReferences();
  }
}
