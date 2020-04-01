package org.arend.ext.error;

import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.DataContainer;
import org.jetbrains.annotations.NotNull;

public class SourceInfoReference implements SourceInfo, ArendRef, DataContainer {
  private final SourceInfo sourceInfo;
  private String module;
  private String position;

  public SourceInfoReference(SourceInfo sourceInfo) {
    this.sourceInfo = sourceInfo;
  }

  @Override
  public Object getData() {
    return sourceInfo instanceof DataContainer ? ((DataContainer) sourceInfo).getData() : sourceInfo;
  }

  @Override
  public String moduleTextRepresentation() {
    if (module != null) {
      return module;
    }
    module = sourceInfo.moduleTextRepresentation();
    if (module == null) {
      module = "";
    }
    return module;
  }

  @Override
  public String positionTextRepresentation() {
    if (position != null) {
      return position;
    }
    position = sourceInfo.positionTextRepresentation();
    if (position == null) {
      position = "";
    }
    return position;
  }

  @NotNull
  @Override
  public String getRefName() {
    String module = moduleTextRepresentation();
    String position = positionTextRepresentation();
    if (module.isEmpty()) {
      return position.isEmpty() ? "" : position;
    } else {
      return position.isEmpty() ? module : module + ":" + position;
    }
  }
}
