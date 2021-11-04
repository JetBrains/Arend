package org.arend.ext.error;

import org.arend.ext.reference.DataContainer;

public interface SourceInfo {
  String moduleTextRepresentation();
  String positionTextRepresentation();

  static SourceInfo getSourceInfo(Object data) {
    while (true) {
      if (data instanceof SourceInfo) return (SourceInfo) data;
      if (!(data instanceof DataContainer)) return null;
      data = ((DataContainer) data).getData();
    }
  }
}
