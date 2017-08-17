package com.jetbrains.jetpad.vclang.naming.scope.primitive;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.function.BiConsumer;

public interface MergingScope extends Scope {
  void findIntroducedDuplicateNames(BiConsumer<Abstract.ReferableSourceNode, Abstract.ReferableSourceNode> reporter);
  void findIntroducedDuplicateInstances(BiConsumer<Abstract.ClassViewInstance, Abstract.ClassViewInstance> reporter);
}
