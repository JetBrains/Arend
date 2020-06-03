package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.term.NamespaceCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public interface Group {
  @NotNull LocatedReferable getReferable();

  @NotNull Collection<? extends Group> getSubgroups();
  @NotNull Collection<? extends NamespaceCommand> getNamespaceCommands();

  @NotNull Collection<? extends InternalReferable> getInternalReferables();

  default @NotNull Collection<? extends InternalReferable> getConstructors() {
    return Collections.emptyList();
  }

  default @NotNull Collection<? extends InternalReferable> getFields() {
    return Collections.emptyList();
  }

  default @NotNull Collection<? extends Group> getDynamicSubgroups() {
    return Collections.emptyList();
  }

  interface InternalReferable {
    LocatedReferable getReferable();
    boolean isVisible();
  }

  default void traverseGroup(Consumer<Group> consumer) {
    consumer.accept(this);
    for (Group subgroup : getSubgroups()) {
      subgroup.traverseGroup(consumer);
    }
    for (Group subgroup : getDynamicSubgroups()) {
      subgroup.traverseGroup(consumer);
    }
  }
}
