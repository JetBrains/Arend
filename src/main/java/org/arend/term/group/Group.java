package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.NamespaceCommand;

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
