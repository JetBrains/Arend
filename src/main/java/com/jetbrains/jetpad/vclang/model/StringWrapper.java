package com.jetbrains.jetpad.vclang.model;

import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.otmodel.node.*;
import jetbrains.jetpad.otmodel.node.Node;
import jetbrains.jetpad.otmodel.wrapper.NodeWrapper;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

import java.util.ArrayList;
import java.util.List;

public class StringWrapper extends NodeWrapper<NodeWrapper<?>> {
  public final Property<String> string = getStringProperty(new NodePropertyId("HVJ6d2b54An.Gtb244hvIci", "string"));

  protected StringWrapper(WrapperContext ctx) {
    super(ctx, new NodeConceptId("BKSIu4SoTdV.Gs5XJZasJcO", "StringWrapper"));
  }

  protected StringWrapper(WrapperContext ctx, Node node) {
    super(ctx, node);
  }

  public static StringWrapper wrap(WrapperContext ctx, String string) {
    StringWrapper wrapper = new StringWrapper(ctx);
    wrapper.string.set(string);
    return wrapper;
  }

  public static List<StringWrapper> wrap(WrapperContext ctx, List<String> strings) {
    List<StringWrapper> wrappers = new ArrayList<>();
    for (String string : strings) {
      wrappers.add(wrap(ctx, string));
    }
    return wrappers;
  }
}
