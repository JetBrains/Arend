package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nullable;

public class LocalReference implements Referable {
  private final String myName;

  public LocalReference(String name) {
    myName = name;
  }

  @Nullable
  @Override
  public String textRepresentation() {
    return myName;
  }
}
