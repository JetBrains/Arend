package com.jetbrains.jetpad.vclang.model.definition;

import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;
import com.jetbrains.jetpad.vclang.model.Node;

public abstract class Definition extends Node {
  public final Property<String> name = new ValueProperty<>();
  public final ObservableList<Argument> arguments = new ObservableArrayList<>();
}
