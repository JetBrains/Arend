package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.NamespaceCommand;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;

public interface Group {
  @Nonnull LocatedReferable getReferable();

  @Nonnull Collection<? extends Group> getSubgroups();
  @Nonnull Collection<? extends NamespaceCommand> getNamespaceCommands();

  @Nonnull Collection<? extends InternalReferable> getInternalReferables();

  default @Nonnull Collection<? extends InternalReferable> getConstructors() {
    return Collections.emptyList();
  }

  default @Nonnull Collection<? extends InternalReferable> getFields() {
    return Collections.emptyList();
  }

  default @Nonnull Collection<? extends Group> getDynamicSubgroups() {
    return Collections.emptyList();
  }

  interface InternalReferable {
    LocatedReferable getReferable();
    boolean isVisible();
  }
}
