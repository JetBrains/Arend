package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class SimpleNamespaceCommand implements Group.NamespaceCommand {
  private final Kind myKind;
  private final Referable myReferable;
  private final boolean myHiding;
  private final List<Referable> myReferences;

  public SimpleNamespaceCommand(Kind kind, Referable referable, boolean isHiding, List<Referable> references) {
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
}
