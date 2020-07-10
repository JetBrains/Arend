package org.arend.naming.reference;

import org.arend.ext.module.LongName;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FieldListReference implements Referable {
  public final List<String> fieldNames;

  public FieldListReference(List<String> fieldNames) {
    this.fieldNames = fieldNames;
  }

  @Override
  public @NotNull String textRepresentation() {
    return new LongName(fieldNames).toString();
  }
}
