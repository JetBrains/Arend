package com.jetbrains.jetpad.vclang.term.pattern.elimtree;

import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;

public interface Clause {
  ElimTreeNode getChild();
  void setChild(ElimTreeNode child);
  ExprSubstitution getSubst();
}
