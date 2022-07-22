package org.arend.term.group;

import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;

public interface Statement {
  default Group getGroup() {
    return null;
  }

  default NamespaceCommand getNamespaceCommand() {
    return null;
  }

  default Abstract.LevelParameters getPLevelsDefinition() {
    return null;
  }

  default Abstract.LevelParameters getHLevelsDefinition() {
    return null;
  }
}
