package org.arend.server;

import org.arend.naming.reference.TCDefReferable;
import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.jetbrains.annotations.Nullable;

public record ConcreteStatement(@Nullable ConcreteGroup group, @Nullable NamespaceCommand command, @Nullable Abstract.LevelParameters pLevelsDefinition, @Nullable Abstract.LevelParameters hLevelsDefinition) implements Statement {
  public @Nullable TCDefReferable getReferable() {
    if (group == null) return null;
    Concrete.Definition definition = group.definition();
    return definition == null ? null : definition.getData();
  }

  @Override
  public Group getGroup() {
    return group;
  }

  @Override
  public NamespaceCommand getNamespaceCommand() {
    return command;
  }

  @Override
  public Abstract.LevelParameters getPLevelsDefinition() {
    return pLevelsDefinition;
  }

  @Override
  public Abstract.LevelParameters getHLevelsDefinition() {
    return hLevelsDefinition;
  }
}
