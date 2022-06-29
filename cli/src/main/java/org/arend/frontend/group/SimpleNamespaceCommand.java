package org.arend.frontend.group;

import org.arend.ext.error.SourceInfo;
import org.arend.ext.reference.DataContainer;
import org.arend.ext.reference.Precedence;
import org.arend.frontend.parser.Position;
import org.arend.naming.reference.Referable;
import org.arend.term.ChildNamespaceCommand;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class SimpleNamespaceCommand implements ChildNamespaceCommand, SourceInfo, DataContainer, Statement {
  private final Position myPosition;
  private final Kind myKind;
  private final List<String> myPath;
  private final boolean myUsing;
  private final List<SimpleNameRenaming> myOpenedReferences;
  private final List<Referable> myHiddenReferences;
  private final ChildGroup myParent;

  public SimpleNamespaceCommand(Position position, Kind kind, List<String> path, boolean isUsing, List<SimpleNameRenaming> openedReferences, List<Referable> hiddenReferences, ChildGroup parent) {
    myPosition = position;
    myKind = kind;
    myPath = path;
    myUsing = isUsing;
    myOpenedReferences = openedReferences;
    myHiddenReferences = hiddenReferences;
    myParent = parent;
  }

  @Override
  public @NotNull Position getData() {
    return myPosition;
  }

  @NotNull
  @Override
  public Kind getKind() {
    return myKind;
  }

  @NotNull
  @Override
  public List<String> getPath() {
    return myPath;
  }

  @Override
  public boolean isUsing() {
    return myUsing;
  }

  @NotNull
  @Override
  public Collection<? extends SimpleNameRenaming> getOpenedReferences() {
    return myOpenedReferences;
  }

  @NotNull
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

  @Override
  public Group getGroup() {
    return null;
  }

  @Override
  public NamespaceCommand getNamespaceCommand() {
    return this;
  }

  public static class SimpleNameRenaming implements NameRenaming, SourceInfo {
    private final Position myPosition;
    private final Referable myReference;
    private final Precedence myPrecedence;
    private final String myName;

    public SimpleNameRenaming(Position position, Referable reference, Precedence precedence, String name) {
      myPosition = position;
      myReference = reference;
      myPrecedence = precedence;
      myName = name;
    }

    @NotNull
    @Override
    public Referable getOldReference() {
      return myReference;
    }

    @Nullable
    @Override
    public String getName() {
      return myName;
    }

    @Nullable
    @Override
    public Precedence getPrecedence() {
      return myPrecedence;
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
}
