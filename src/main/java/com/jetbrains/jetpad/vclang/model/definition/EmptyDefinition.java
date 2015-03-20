package com.jetbrains.jetpad.vclang.model.definition;

import com.jetbrains.jetpad.vclang.model.Node;

public class EmptyDefinition extends Definition {
  @Override
  public Node[] children() {
    return new Node[0];
  }
}
