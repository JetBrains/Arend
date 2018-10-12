package org.arend.naming.scope;

import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TypedReferable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class ClassFieldImplScope implements Scope {
  private final ClassReferable myReferable;
  private final boolean myWithSuperClasses;

  public ClassReferable getClassReference() {
    return myReferable;
  }

  public boolean withSuperClasses() {
    return myWithSuperClasses;
  }

  public ClassFieldImplScope(ClassReferable referable, boolean withSuperClasses) {
    myReferable = referable;
    myWithSuperClasses = withSuperClasses;
  }

  private Referable find(Predicate<Referable> pred, Deque<ClassReferable> toVisit, Set<ClassReferable> visitedClasses, Set<LocatedReferable> excludedFields, Set<ClassReferable> underlyingClasses) {
    while (!toVisit.isEmpty()) {
      ClassReferable classRef = toVisit.pop();
      if (!visitedClasses.add(classRef)) {
        continue;
      }

      if (underlyingClasses != null) {
        ClassReferable underlyingClass = classRef.getUnderlyingReference();
        if (underlyingClass != null && !underlyingClass.isSynonym()) {
          underlyingClasses.add(underlyingClass);
        }
      }

      for (LocatedReferable referable : classRef.getFieldReferables()) {
        if (underlyingClasses != null) {
          if (pred.test(referable)) {
            return referable;
          }

          LocatedReferable underlyingField = referable.getUnderlyingReference();
          if (underlyingField != null) {
            excludedFields.add(underlyingField);
          }
        } else {
          if (!excludedFields.contains(referable) && pred.test(referable)) {
            return referable;
          }
        }
      }

      Collection<? extends ClassReferable> superClasses = classRef.getSuperClassReferences();
      if (myWithSuperClasses && underlyingClasses != null) {
        for (ClassReferable superClass : superClasses) {
          if (pred.test(superClass)) {
            return superClass;
          }
        }
      }

      toVisit.addAll(superClasses);
    }

    return null;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    Set<LocatedReferable> excludedFields = new HashSet<>();
    Set<ClassReferable> underlyingClasses = new LinkedHashSet<>();
    Set<ClassReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(myReferable);

    Referable ref = find(pred, toVisit, visitedClasses, excludedFields, underlyingClasses);
    if (ref != null) {
      return ref;
    }

    if (!underlyingClasses.isEmpty()) {
      toVisit.addAll(underlyingClasses);
      return find(pred, toVisit, visitedClasses, excludedFields, null);
    } else {
      return null;
    }
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name) {
    Referable referable = resolveName(name);
    if (myWithSuperClasses && referable instanceof ClassReferable) {
      return new ClassFieldImplScope((ClassReferable) referable, true);
    }
    if (referable instanceof TypedReferable) {
      ClassReferable classRef = ((TypedReferable) referable).getTypeClassReference();
      if (classRef != null) {
        return new ClassFieldImplScope(classRef, false);
      }
    }
    return null;
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscope() {
    return EmptyScope.INSTANCE;
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    return EmptyScope.INSTANCE;
  }
}
