package com.jetbrains.jetpad.vclang.model;

import jetbrains.jetpad.otmodel.node.Node;
import jetbrains.jetpad.otmodel.wrapper.NodeWrapper;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

public class NodeWrapperFactory implements jetbrains.jetpad.otmodel.wrapper.NodeWrapperFactory {
  @Override
  public NodeWrapper<?> createWrapperFor(WrapperContext context, Node node) {
    throw new RuntimeException("Not implemented yet.");
  }
}
