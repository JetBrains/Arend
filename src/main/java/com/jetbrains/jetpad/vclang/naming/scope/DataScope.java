package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.scope.primitive.OverridingScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;

public class DataScope extends OverridingScope {
  public DataScope(Scope parent, Scope staticNsScope) {
    super(parent, staticNsScope);
  }
}
