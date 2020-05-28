package org.arend.extImpl;

import org.arend.ext.core.expr.UncheckedExpression;
import org.arend.ext.variable.VariableRenamer;
import org.arend.ext.variable.VariableRenamerFactory;
import org.arend.naming.renamer.Renamer;
import org.arend.naming.renamer.StringRenamer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VariableRenamerFactoryImpl implements VariableRenamerFactory {
  public static final VariableRenamerFactory INSTANCE = new VariableRenamerFactoryImpl();

  private VariableRenamerFactoryImpl() {}

  @Override
  public @NotNull VariableRenamer variableRenamer() {
    return new StringRenamer();
  }

  @Override
  public @NotNull String getNameFromType(@NotNull UncheckedExpression type, @Nullable String def) {
    return Renamer.getNameFromType(UncheckedExpressionImpl.extract(type), def);
  }
}
