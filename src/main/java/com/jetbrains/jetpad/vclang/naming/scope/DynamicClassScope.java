package com.jetbrains.jetpad.vclang.naming.scope;

import com.google.common.collect.Iterables;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.MergingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiConsumer;

public class DynamicClassScope extends OverridingScope implements MergingScope {
  private final MergeScope myMerging;

  public DynamicClassScope(Scope parent, Scope staticNsScope, Scope dynamicNsScope) {
    this(parent, new MergeScope(Arrays.asList(dynamicNsScope, staticNsScope)));
  }

  public DynamicClassScope(Scope parent, Scope staticNsScope, Scope dynamicNsScope, Iterable<Scope> extra) {
    this(parent, new MergeScope(Arrays.asList(dynamicNsScope, new MergeScope(Iterables.concat(Collections.singleton(staticNsScope), extra)))));
  }

  private DynamicClassScope(Scope parent, MergeScope mergeScope) {
    super(parent, mergeScope);
    myMerging = mergeScope;
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
