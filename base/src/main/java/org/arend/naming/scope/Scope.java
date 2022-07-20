package org.arend.naming.scope;

import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Minimal definition: (find or getAllElements) and resolveNamespace
public interface Scope {
  default @Nullable Referable find(Predicate<Referable> pred) {
    for (Referable referable : getAllElements()) {
      if (pred.test(referable)) {
        return referable;
      }
    }
    return null;
  }

  @NotNull
  default Collection<? extends Referable> getAllElements() {
    List<Referable> result = new ArrayList<>();
    find(ref -> { result.add(ref); return false; });
    return result;
  }

  @NotNull
  default Collection<? extends Referable> getElements(Referable.RefKind kind) {
    List<Referable> result = new ArrayList<>();
    find(ref -> { if (ref.getRefKind() == kind) result.add(ref); return false; });
    return result;
  }

  @NotNull
  default Collection<? extends Referable> getElements() {
    return getElements(Referable.RefKind.EXPR);
  }

  @Nullable
  default Referable resolveName(@NotNull String name, @Nullable Referable.RefKind kind) {
    return find(ref -> (kind == null || ref.getRefKind() == kind) && Objects.equals(name, ref.textRepresentation()));
  }

  @Nullable
  default Referable resolveName(@NotNull String name) {
    return resolveName(name, Referable.RefKind.EXPR);
  }

  default @Nullable Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    return null;
  }

  default @NotNull Scope getGlobalSubscope() {
    return this;
  }

  default @NotNull Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    return this;
  }

  default @Nullable ImportedScope getImportedSubscope() {
    return null;
  }

  default @Nullable Scope resolveNamespace(List<? extends String> path) {
    Scope scope = this;
    for (String name : path) {
      scope = scope.resolveNamespace(name, true);
      if (scope == null) {
        return null;
      }
    }
    return scope;
  }

  static Referable resolveName(Scope scope, List<? extends String> path) {
    return resolveName(scope, path, false);
  }

  static Referable resolveName(Scope scope, List<? extends String> path, boolean withSuperClasses) {
    for (int i = 0; i < path.size(); i++) {
      if (scope == null) {
        return null;
      }
      if (withSuperClasses && i == path.size() - 2) {
        Referable parentRef = scope.resolveName(path.get(i));
        if (parentRef instanceof ClassReferable) {
          Referable result = new ClassFieldImplScope((ClassReferable) parentRef, false).resolveName(path.get(i + 1));
          if (result != null) {
            return result;
          }
        }
      }
      if (i == path.size() - 1) {
        return scope.resolveName(path.get(i));
      } else {
        scope = scope.resolveNamespace(path.get(i), i < path.size() - 2);
      }
    }
    return null;
  }

  static void traverse(Scope scope, Consumer<Referable> consumer) {
    if (scope == null) return;
    for (Referable ref : scope.getElements()) {
      consumer.accept(ref);
      traverse(scope.resolveNamespace(ref.getRefName(), false), consumer);
    }
  }
}
