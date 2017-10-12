package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

// Minimal definition: find or getElements
public interface Scope {
  default Referable find(Predicate<Referable> pred) {
    for (Referable referable : getElements()) {
      if (pred.test(referable)) {
        return referable;
      }
    }
    return null;
  }

  default Collection<? extends Referable> getElements() {
    List<Referable> result = new ArrayList<>();
    find(ref -> { result.add(ref); return false; });
    return result;
  }

  default Referable resolveName(String name) {
    return find(ref -> Objects.equals(name, ref.textRepresentation()));
  }

  default boolean isEmpty() {
    return find(ref -> true) == null;
  }
}
