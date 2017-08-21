package com.jetbrains.jetpad.vclang.naming.scope;

import com.google.common.collect.Iterables;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.MergingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.function.BiConsumer;

public class StaticClassScope extends OverridingScope implements MergingScope {
  private final MergeScope myMerging;

  public StaticClassScope(Scope parent, Scope staticNsScope) {
    super(parent, staticNsScope);
    myMerging = null;
  }

  public StaticClassScope(Scope parent, Scope staticNsScope, Iterable<Scope> extra) {
    this(parent, new MergeScope(Iterables.concat(Collections.singleton(staticNsScope), extra)));
  }

  private StaticClassScope(Scope parent, MergeScope merging) {
    super(parent, merging);
    myMerging = merging;
  }

  @Override
  public void findIntroducedDuplicateNames(BiConsumer<Referable, Referable> reporter) {
    myMerging.findIntroducedDuplicateNames(reporter);
  }

  @Override
  public void findIntroducedDuplicateInstances(BiConsumer<Abstract.ClassViewInstance, Abstract.ClassViewInstance> reporter) {
    myMerging.findIntroducedDuplicateInstances(reporter);
  }
}
