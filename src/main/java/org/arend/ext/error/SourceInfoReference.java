package org.arend.ext.error;

import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.DataContainer;
import org.jetbrains.annotations.NotNull;

public class SourceInfoReference implements SourceInfo, ArendRef, DataContainer {
  private final SourceInfo sourceInfo;

  public SourceInfoReference(SourceInfo sourceInfo) {
    this.sourceInfo = sourceInfo;
  }

  @Override
  public Object getData() {
    return sourceInfo instanceof DataContainer ? ((DataContainer) sourceInfo).getData() : sourceInfo;
  }

  @Override
  public String moduleTextRepresentation() {
    return sourceInfo.moduleTextRepresentation();
  }

  @Override
  public String positionTextRepresentation() {
    return sourceInfo.positionTextRepresentation();
  }

  @NotNull
  @Override
  public String getRefName() {
    String module = sourceInfo.moduleTextRepresentation();
    String position = sourceInfo.positionTextRepresentation();
    if (module == null) {
      return position == null ? "" : position;
    } else {
      return position == null ? module : module + ":" + position;
    }
  }
}
