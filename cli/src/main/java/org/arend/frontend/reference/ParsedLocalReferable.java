package org.arend.frontend.reference;

import org.arend.ext.error.SourceInfo;
import org.arend.frontend.parser.Position;
import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;

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

  @NotNull
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
