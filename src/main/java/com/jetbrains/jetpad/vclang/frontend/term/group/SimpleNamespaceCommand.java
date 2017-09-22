package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class SimpleNamespaceCommand implements NamespaceCommand, SourceInfo {
  private final Position myPosition;
  private final Kind myKind;
  private Referable myReferable;
  private final boolean myHiding;
  private final List<Referable> myReferences;

  public SimpleNamespaceCommand(Position position, Kind kind, Referable referable, boolean isHiding, List<Referable> references) {
    myPosition = position;
    myKind = kind;
    myReferable = referable;
    myHiding = isHiding;
    myReferences = references;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myKind;
  }

  @Nonnull
  @Override
  public Referable getGroupReference() {
    return myReferable;
  }

  @Override
  public boolean isHiding() {
    return myHiding;
  }

  @Nullable
  @Override
  public List<Referable> getSubgroupReferences() {
    return myReferences;
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
