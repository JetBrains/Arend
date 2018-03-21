package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

// Minimal definition: (find or getElements) and resolveNamespace
public interface Scope {
  default @Nullable Referable find(Predicate<Referable> pred) {
    for (Referable referable : getElements()) {
      if (pred.test(referable)) {
        return referable;
      }
    }
    return null;
  }

  default @Nonnull Collection<? extends Referable> getElements() {
    List<Referable> result = new ArrayList<>();
    find(ref -> { result.add(ref); return false; });
    return result;
  }

  default @Nullable Referable resolveName(String name) {
    return find(ref -> Objects.equals(name, ref.textRepresentation()));
  }

  default @Nullable Scope resolveNamespace(String name, boolean resolveModuleNames) {
    return null;
  }

  default @Nonnull Scope getGlobalSubscope() {
    return this;
  }

  default @Nonnull Scope getGlobalSubscopeWithoutOpens() {
    return this;
  }

  default @Nullable ImportedScope getImportedSubscope() {
    return null;
  }

  class Utils {
    public static Referable resolveName(Scope scope, List<? extends String> path) {
      for (int i = 0; i < path.size(); i++) {
        if (scope == null) {
          return null;
        }
        if (i == path.size() - 1) {
          return scope.resolveName(path.get(i));
        } else {
          scope = scope.resolveNamespace(path.get(i), true);
        }
      }
      return null;
    }

    public static Scope resolveNamespace(Scope scope, List<? extends String> path) {
      for (String name : path) {
        if (scope == null) {
          return null;
        }
        scope = scope.resolveNamespace(name, true);
      }
      return scope;
    }
  }
}
