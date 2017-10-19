package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.naming.scope.EmptyScope;
import com.jetbrains.jetpad.vclang.naming.scope.LexicalScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;

import javax.annotation.Nullable;

public interface ChildGroup extends Group {
  @Nullable ChildGroup getParentGroup();

  default Scope getLexicalScope() {
    ChildGroup parent = getParentGroup();
    return new LexicalScope(parent == null ? EmptyScope.INSTANCE : parent.getLexicalScope(), this); // TODO[abstract]: Should be Prelude namespace instead of empty
  }
}
