package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.Node;
import jetbrains.jetpad.otmodel.node.NodeConceptId;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

public abstract class Definition extends Node {
  protected Definition(WrapperContext ctx, NodeConceptId conceptId) {
    super(ctx, conceptId);
  }

  protected Definition(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
    super(ctx, node);
  }
}
