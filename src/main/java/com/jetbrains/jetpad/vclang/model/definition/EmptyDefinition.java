package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.Node;
import jetbrains.jetpad.otmodel.node.NodeConceptId;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

public class EmptyDefinition extends Definition {
  public EmptyDefinition(WrapperContext ctx) {
    super(ctx, new NodeConceptId("GQ31sDHt3Ep.ERVqHouXfF2", "EmptyDefinition"));
  }

  protected EmptyDefinition(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
    super(ctx, node);
  }

  @Override
  public Node[] children() {
    return new Node[0];
  }
}
