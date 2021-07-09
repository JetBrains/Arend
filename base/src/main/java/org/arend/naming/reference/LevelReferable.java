package org.arend.naming.reference;

import org.arend.core.context.binding.ParamLevelVariable;
import org.arend.ext.reference.DataContainer;

public interface LevelReferable extends Referable, DataContainer {
  ParamLevelVariable getLevelVariable();
  void setLevelVariable(ParamLevelVariable levelVariable);
}
