package com.jetbrains.jetpad.vclang.frontend.term.group;

import com.jetbrains.jetpad.vclang.error.SourceInfo;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.ChildNamespaceCommand;
import com.jetbrains.jetpad.vclang.term.NameRenaming;
import com.jetbrains.jetpad.vclang.term.Precedence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class SimpleNamespaceCommand implements ChildNamespaceCommand, SourceInfo {
  private final Position myPosition;
  private final Kind myKind;
  private final Referable myReferable;
  private final boolean myUsing;
  private final List<SimpleNameRenaming> myOpenedReferences;
  private final List<Referable> myHiddenReferences;
  private final ChildGroup myParent;

  public SimpleNamespaceCommand(Position position, Kind kind, Referable referable, boolean isUsing, List<SimpleNameRenaming> openedReferences, List<Referable> hiddenReferences, ChildGroup parent) {
    myPosition = position;
    myKind = kind;
    myReferable = referable;
    myUsing = isUsing;
    myOpenedReferences = openedReferences;
    myHiddenReferences = hiddenReferences;
    myParent = parent;
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
  public boolean isUsing() {
    return myUsing;
  }

  @Nonnull
  @Override
  public Collection<? extends SimpleNameRenaming> getOpenedReferences() {
    return myOpenedReferences;
  }

  @Nonnull
  @Override
  public Collection<? extends Referable> getHiddenReferences() {
    return myHiddenReferences;
  }

  @Override
  public String moduleTextRepresentation() {
    return myPosition.moduleTextRepresentation();
  }

  @Override
  public String positionTextRepresentation() {
    return myPosition.positionTextRepresentation();
  }

  @Nullable
  @Override
  public ChildGroup getParentGroup() {
    return myParent;
  }

  public static class SimpleNameRenaming implements NameRenaming {
    private final Referable myReference;
    private final GlobalReference myReferable;

    public SimpleNameRenaming(Referable reference, GlobalReference referable) {
      myReference = reference;
      myReferable = referable;
    }

    @Nonnull
    @Override
    public Referable getOldReference() {
      return myReference;
    }

    @Nullable
    @Override
    public GlobalReferable getNewReferable() {
      return myReferable;
    }
  }
}
