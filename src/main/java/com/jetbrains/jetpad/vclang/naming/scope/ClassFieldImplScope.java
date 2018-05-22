package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TypedReferable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class ClassFieldImplScope implements Scope {
  private final ClassReferable myReferable;
  private final boolean myWithSuperClasses;

  public ClassFieldImplScope(ClassReferable referable, boolean withSuperClasses) {
    myReferable = referable;
    myWithSuperClasses = withSuperClasses;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    Set<GlobalReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(myReferable);
    while (!toVisit.isEmpty()) {
      ClassReferable classRef = toVisit.pop();
      if (!visitedClasses.add(classRef)) {
        continue;
      }

      for (GlobalReferable referable : classRef.getFieldReferables()) {
        if (pred.test(referable)) {
          return referable;
        }
      }

      Collection<? extends ClassReferable> superClasses = classRef.getSuperClassReferences();
      if (myWithSuperClasses) {
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
