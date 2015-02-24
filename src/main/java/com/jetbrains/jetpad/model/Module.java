package com.jetbrains.jetpad.model;

import jetbrains.jetpad.model.collections.list.ObservableArrayList;
import jetbrains.jetpad.model.collections.list.ObservableList;
import com.jetbrains.jetpad.model.definition.Definition;

public class Module extends Node {
    public final ObservableList<Definition> definitions = new ObservableArrayList<Definition>();
}
