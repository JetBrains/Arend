package com.jetbrains.jetpad.vclang.core.definition;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;

public interface Function {
  ElimTreeNode getElimTree();
  DependentLink getParameters();
  int getNumberOfRequiredArguments();
}
