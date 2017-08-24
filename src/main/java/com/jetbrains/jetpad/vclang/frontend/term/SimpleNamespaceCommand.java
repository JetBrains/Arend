package com.jetbrains.jetpad.vclang.frontend.term;

import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
import com.jetbrains.jetpad.vclang.term.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class SimpleNamespaceCommand implements Group.NamespaceCommand {
  private final Kind myKind;
  private final UnresolvedReference myReference;
  private final List<UnresolvedReference> myReferences;

  public SimpleNamespaceCommand(Kind kind, UnresolvedReference reference, List<UnresolvedReference> references) {
    myKind = kind;
    myReference = reference;
    myReferences = references;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myKind;
  }

  @Nonnull
  @Override
  public UnresolvedReference getGroupReference() {
    return myReference;
  }

  @Nullable
  @Override
  public List<UnresolvedReference> getSubgroupReferences() {
    return myReferences;
  }
}
