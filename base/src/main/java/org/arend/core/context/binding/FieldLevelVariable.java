package org.arend.core.context.binding;

public class FieldLevelVariable extends ParamLevelVariable {
  public static class LevelField {}

  private final LevelField myField;

  public FieldLevelVariable(LvlType levelType, String name, int size, LevelField field) {
    super(levelType, name, size);
    myField = field;
  }

  public LevelField getLevelField() {
    return myField;
  }
}
