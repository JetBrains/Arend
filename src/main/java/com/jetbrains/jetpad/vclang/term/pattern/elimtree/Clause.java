package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.expr.Substitution;

public interface Clause {
  ElimTreeNode getChild();
  void setChild(ElimTreeNode child);
  Substitution getSubst();
}
