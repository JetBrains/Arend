package org.arend.naming.reference;

import org.arend.core.context.binding.FieldLevelVariable;
import org.arend.ext.reference.DataContainer;

public interface LevelReferable extends Referable, DataContainer {
  FieldLevelVariable.LevelField getLevelField();
  void setLevelField(FieldLevelVariable.LevelField levelField);
}
