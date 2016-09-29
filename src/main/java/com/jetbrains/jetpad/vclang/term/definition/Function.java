package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

public interface Function {
  ElimTreeNode getElimTree();
  DependentLink getParameters();
  int getNumberOfRequiredArguments();
}
