package com.jetbrains.jetpad.vclang.model;

import jetbrains.jetpad.model.children.HasParent;

public class Node extends HasParent<Node, Node> {
  public Position position;
}
