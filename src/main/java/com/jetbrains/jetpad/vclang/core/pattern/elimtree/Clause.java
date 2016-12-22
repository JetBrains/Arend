package com.jetbrains.jetpad.vclang.core.pattern.elimtree;

import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;

public interface Clause {
  ElimTreeNode getChild();
  void setChild(ElimTreeNode child);
  ExprSubstitution getSubst();
}
