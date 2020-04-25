package org.arend.naming.reference;

import org.arend.naming.renamer.Renamer;
import org.arend.typechecking.result.TypecheckingResult;
import org.jetbrains.annotations.NotNull;

public class CoreReferable implements Referable {
  private final String myName;
  public final TypecheckingResult result;

  public CoreReferable(String name, TypecheckingResult result) {
    myName = name;
    this.result = result;
  }

  public boolean printExpression() {
    return myName == null;
  }

  @NotNull
  @Override
  public String textRepresentation() {
    return myName == null ? Renamer.UNNAMED : myName;
  }
}
