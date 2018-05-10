package com.jetbrains.jetpad.vclang.error;

import com.jetbrains.jetpad.vclang.naming.reference.DataContainer;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;

public class SourceInfoReference implements SourceInfo, Referable, DataContainer {
  private final SourceInfo mySourceInfo;

  public SourceInfoReference(SourceInfo sourceInfo) {
    mySourceInfo = sourceInfo;
  }

  @Override
  public Object getData() {
    return mySourceInfo instanceof DataContainer ? ((DataContainer) mySourceInfo).getData() : mySourceInfo;
  }

  @Override
  public String moduleTextRepresentation() {
    return mySourceInfo.moduleTextRepresentation();
  }

  @Override
  public String positionTextRepresentation() {
    return mySourceInfo.positionTextRepresentation();
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    String module = mySourceInfo.moduleTextRepresentation();
    String position = mySourceInfo.positionTextRepresentation();
    if (module == null) {
      return position == null ? "" : position;
    } else {
      return position == null ? module : module + ":" + position;
    }
  }
}
