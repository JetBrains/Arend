package org.arend.term.group;

import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.ParameterReferable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public interface Group {
  @NotNull LocatedReferable getReferable();

  @NotNull List<? extends Statement> getStatements();

  @NotNull List<? extends InternalReferable> getInternalReferables();

  default @NotNull List<? extends InternalReferable> getConstructors() {
    return Collections.emptyList();
  }

  default @NotNull List<? extends InternalReferable> getFields() {
    return Collections.emptyList();
  }

  default @NotNull List<? extends Group> getDynamicSubgroups() {
    return Collections.emptyList();
  }

  default @NotNull List<? extends ParameterReferable> getExternalParameters() {
    return Collections.emptyList();
  }

  interface InternalReferable {
    LocatedReferable getReferable();
    boolean isVisible();
  }

  default void traverseGroup(Consumer<Group> consumer) {
    consumer.accept(this);
    for (Statement statement : getStatements()) {
      Group subgroup = statement.getGroup();
      if (subgroup != null) {
        subgroup.traverseGroup(consumer);
      }
    }
    for (Group subgroup : getDynamicSubgroups()) {
      subgroup.traverseGroup(consumer);
    }
  }
}
