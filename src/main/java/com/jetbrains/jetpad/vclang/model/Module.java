package com.jetbrains.jetpad.vclang.model;

import com.jetbrains.jetpad.vclang.model.definition.Definition;
import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.otmodel.node.NodeConceptId;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

public class Module extends Node {
  public static final NodeConceptId CONCEPT_ID = new NodeConceptId("e-lVXYvvhR.HMthbUwSlw7", "Module");

  public final ObservableList<Definition> definitions = new ObservableArrayList<>();

  public Module(WrapperContext ctx) {
    super(ctx, CONCEPT_ID);
  }

  protected Module(WrapperContext ctx, jetbrains.jetpad.otmodel.node.Node node) {
    super(ctx, node);
  }

  @Override
  public Node[] children() {
    return definitions.toArray(new Node[definitions.size()]);
  }
}
