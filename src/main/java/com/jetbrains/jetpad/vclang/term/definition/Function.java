package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;

import java.util.List;

public interface Function extends Abstract.Function {
  ElimTreeNode getElimTree();
  List<? extends Argument> getArguments();
  Expression getResultType();
  ClassDefinition getThisClass();
}
