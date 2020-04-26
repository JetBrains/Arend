package org.arend.extImpl;

import org.arend.ext.variable.VariableRenamer;
import org.arend.ext.variable.VariableRenamerFactory;
import org.arend.naming.renamer.StringRenamer;
import org.jetbrains.annotations.NotNull;

public class VariableRenamerFactoryImpl implements VariableRenamerFactory {
  public static final VariableRenamerFactory INSTANCE = new VariableRenamerFactoryImpl();

  private VariableRenamerFactoryImpl() {}

  @Override
  public @NotNull VariableRenamer variableRenamer() {
    return new StringRenamer();
  }
}
