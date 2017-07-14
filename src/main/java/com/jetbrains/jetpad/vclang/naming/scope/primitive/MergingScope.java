package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.function.BiConsumer;

public interface MergingScope extends Scope {
  void findIntroducedDuplicateNames(BiConsumer<Abstract.Definition, Abstract.Definition> reporter);
  void findIntroducedDuplicateInstances(BiConsumer<Abstract.ClassViewInstance, Abstract.ClassViewInstance> reporter);
}
