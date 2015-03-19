package com.jetbrains.jetpad.vclang.model.error;

import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class ErrorMessage {
  private final Property<String> myMessage = new ValueProperty<>();

  public Property<String> message() {
    return myMessage;
  }
}
