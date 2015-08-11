package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public interface NamespaceMember {
  Namespace getParent();
  Utils.Name getName();
}
