package com.jetbrains.jetpad.vclang.model;

import com.jetbrains.jetpad.vclang.model.definition.Definition;
import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;

public class Module extends Node {
  public final ObservableList<Definition> definitions = new ObservableArrayList<>();

  @Override
  public Node[] children() {
    return definitions.toArray(new Node[definitions.size()]);
  }
}
