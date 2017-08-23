package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;

import java.util.function.BiConsumer;

public interface MergingScope extends Scope {
  void findIntroducedDuplicateNames(BiConsumer<Referable, Referable> reporter);
  void findIntroducedDuplicateInstances(BiConsumer<Concrete.Instance, Concrete.Instance> reporter);
}
