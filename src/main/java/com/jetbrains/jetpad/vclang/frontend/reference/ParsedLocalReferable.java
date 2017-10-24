package com.jetbrains.jetpad.vclang.frontend.reference;

import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;

import javax.annotation.Nonnull;

public class ParsedLocalReferable implements Referable, SourceInfo {
  private final Position myPosition;
  private final String myName;

  public ParsedLocalReferable(Position position, String name) {
    myPosition = position;
    myName = name;
  }

  public Position getPosition() {
    return myPosition;
  }

  @Nonnull
  @Override
  public String textRepresentation() {
    return myName == null ? "_" : myName;
  }

  @Override
  public String moduleTextRepresentation() {
    return myPosition.moduleTextRepresentation();
  }

  @Override
  public String positionTextRepresentation() {
    return myPosition.positionTextRepresentation();
  }
}
