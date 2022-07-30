package org.arend.frontend.reference;

import org.arend.ext.error.SourceInfo;
import org.arend.frontend.parser.Position;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.Referable;

public class ParsedLocalReferable extends LocalReferable implements Referable, SourceInfo {
  private final Position myPosition;

  public ParsedLocalReferable(Position position, String name) {
    super(name);
    myPosition = position;
  }

  public Position getPosition() {
    return myPosition;
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
