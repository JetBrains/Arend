package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.naming.scope.EmptyModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.naming.scope.ScopeFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ChildGroup extends Group {
  @Nullable ChildGroup getParentGroup();

  @Nonnull
  default Scope getGroupScope() {
    ChildGroup parent = getParentGroup();
    return parent == null ? ScopeFactory.forGroup(this, EmptyModuleScopeProvider.INSTANCE) : LexicalScope.insideOf(this, parent.getGroupScope());
  }
}
