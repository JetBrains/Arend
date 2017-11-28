package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class ClassFieldImplScope implements Scope {
  private final ClassReferable myReferable;

  public ClassFieldImplScope(ClassReferable referable) {
    myReferable = referable;
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

      for (GlobalReferable referable : classRef.getFields()) {
        if (pred.test(referable)) {
          return referable;
        }
      }

      Collection<? extends ClassReferable> superClasses = classRef.getSuperClassReferences();
      for (ClassReferable superClass : superClasses) {
        if (pred.test(superClass)) {
          return superClass;
        }
      }

      toVisit.addAll(superClasses);
    }

    return null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean resolveModuleNames) {
    Referable referable = resolveName(name);
    return referable instanceof ClassReferable ? new ClassFieldImplScope((ClassReferable) referable) : null;
  }
}
