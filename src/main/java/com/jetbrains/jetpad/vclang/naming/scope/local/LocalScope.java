package com.jetbrains.jetpad.vclang.naming.scope.local;

import com.jetbrains.jetpad.vclang.naming.scope.Scope;

public interface LocalScope extends Scope {
  Scope getGlobalScope();
}
