package com.jetbrains.jetpad.vclang.model;

import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
import com.jetbrains.jetpad.vclang.model.definition.Definition;

public class Module extends Node {
  public final ObservableList<Definition> definitions = new ObservableArrayList<>();
}
