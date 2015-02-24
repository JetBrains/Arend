package com.jetbrains.jetpad.model.definition;

import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;
import com.jetbrains.jetpad.model.Node;

public abstract class Definition extends Node {
    public final Property<String> name = new ValueProperty<String>();
    public final ObservableList<Argument> arguments = new ObservableArrayList<Argument>();
}
