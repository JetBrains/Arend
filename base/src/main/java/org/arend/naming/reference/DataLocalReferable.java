package org.arend.naming.reference;

import org.arend.ext.error.SourceInfo;
import org.arend.ext.reference.DataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataLocalReferable extends LocalReferable implements DataContainer, SourceInfo {
  private final Object myData;

  public DataLocalReferable(Object data, String name) {
    super(name);
    myData = data;
  }

  public static DataLocalReferable make(Referable referable) {
    return referable == null ? null : new DataLocalReferable(referable, referable.getRefName());
  }

  @Override
  public @Nullable Object getData() {
    return myData;
  }

  @Override
  public @NotNull Referable getUnderlyingReferable() {
    return myData instanceof Referable ? (Referable) myData : this;
  }

  @Override
  public String moduleTextRepresentation() {
    SourceInfo sourceInfo = SourceInfo.getSourceInfo(myData);
    return sourceInfo != null ? sourceInfo.moduleTextRepresentation() : null;
  }

  @Override
  public String positionTextRepresentation() {
    SourceInfo sourceInfo = SourceInfo.getSourceInfo(myData);
    return sourceInfo != null ? sourceInfo.positionTextRepresentation() : null;
  }
}
