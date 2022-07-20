package org.arend.naming.scope;

import org.arend.naming.reference.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class ClassFieldImplScope implements Scope {
  private final ClassReferable myReferable;
  private final Extent myExtent;

  public enum Extent { WITH_SUPER_CLASSES, WITH_DYNAMIC, WITH_SUPER_DYNAMIC, ONLY_FIELDS }

  public ClassReferable getClassReference() {
    return myReferable;
  }

  public boolean withSuperClasses() {
    return myExtent == Extent.WITH_SUPER_CLASSES;
  }

  public ClassFieldImplScope(ClassReferable referable, Extent extent) {
    myReferable = referable;
    myExtent = extent;
  }

  public ClassFieldImplScope(ClassReferable referable, boolean withSuperClasses) {
    myReferable = referable;
    myExtent = withSuperClasses ? Extent.WITH_SUPER_CLASSES : Extent.ONLY_FIELDS;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    Set<ClassReferable> visitedClasses = new HashSet<>();
    Deque<ClassReferable> toVisit = new ArrayDeque<>();
    toVisit.add(myReferable);
    Extent extent = myExtent;

    while (!toVisit.isEmpty()) {
      ClassReferable classRef = toVisit.removeLast();
      if (!visitedClasses.add(classRef)) {
        continue;
      }

      for (LocatedReferable referable : classRef.getFieldReferables()) {
        if (pred.test(referable)) {
          return referable;
        }
        if (referable.hasAlias()) {
          AliasReferable aliasRef = new AliasReferable(referable);
          if (pred.test(aliasRef)) {
            return aliasRef;
          }
        }
      }

      if (extent == Extent.WITH_DYNAMIC) {
        for (GlobalReferable referable : classRef.getDynamicReferables()) {
          if (pred.test(referable)) {
            return referable;
          }
          if (referable.hasAlias()) {
            AliasReferable aliasRef = new AliasReferable(referable);
            if (pred.test(aliasRef)) {
              return aliasRef;
            }
          }
        }
      } else if (extent == Extent.WITH_SUPER_DYNAMIC) {
        extent = Extent.WITH_DYNAMIC;
      }

      List<? extends ClassReferable> superClasses = classRef.getSuperClassReferences();
      if (myExtent == Extent.WITH_SUPER_CLASSES) {
        for (ClassReferable superClass : superClasses) {
          if (pred.test(superClass)) {
            return superClass;
          }
          if (superClass.hasAlias()) {
            AliasReferable aliasRef = new AliasReferable(superClass);
            if (pred.test(aliasRef)) {
              return aliasRef;
            }
          }
        }
      }

      for (int i = superClasses.size() - 1; i >= 0; i--) {
        toVisit.add(superClasses.get(i));
      }
    }

    return null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    Referable referable = resolveName(name);
    if (myExtent == Extent.WITH_SUPER_CLASSES && referable instanceof ClassReferable) {
      return new ClassFieldImplScope((ClassReferable) referable, Extent.WITH_SUPER_CLASSES);
    }
    if (referable instanceof TypedReferable) {
      ClassReferable classRef = ((TypedReferable) referable).getTypeClassReference();
      if (classRef != null) {
        return new ClassFieldImplScope(classRef, onlyInternal ? Extent.ONLY_FIELDS : Extent.WITH_DYNAMIC);
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscope() {
    return EmptyScope.INSTANCE;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    return EmptyScope.INSTANCE;
  }
}
