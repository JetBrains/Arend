package com.jetbrains.jetpad.vclang.naming.reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public interface ClassReferable extends LocatedReferable {
  @Nonnull Collection<? extends ClassReferable> getSuperClassReferences();
  @Nonnull Collection<? extends Reference> getUnresolvedSuperClassReferences();
  @Nonnull Collection<? extends LocatedReferable> getFieldReferables();
  @Override @Nullable ClassReferable getUnderlyingReference();

  default @Nonnull ClassReferable getUnderlyingTypecheckable() {
    ClassReferable underlyingRef = getUnderlyingReference();
    return underlyingRef != null ? underlyingRef : this;
  }

  @Override
  default boolean isFieldSynonym() {
    return false;
  }

  default boolean isSubClassOf(ClassReferable classRef) {
    if (this == classRef) {
      return true;
    }

    Set<ClassReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(this);
    while (!toVisit.isEmpty()) {
      ClassReferable ref = toVisit.pop();
      if (classRef == ref) {
        return true;
      }
      if (visitedClasses.add(ref)) {
        toVisit.addAll(ref.getSuperClassReferences());
      }
    }

    return false;
  }
}
