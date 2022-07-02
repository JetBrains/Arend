package org.arend.naming.scope;

import org.arend.naming.reference.Referable;
import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LevelLexicalScope implements Scope {
  private final Scope myParent;
  private final Group myGroup;
  private final boolean myPLevels;
  private final boolean myWithOpens;

  public LevelLexicalScope(Scope parent, Group group, boolean isPLevels, boolean withOpens) {
    myParent = parent;
    myGroup = group;
    myPLevels = isPLevels;
    myWithOpens = withOpens;
  }

  public static LevelLexicalScope insideOf(Group group, Scope parent, boolean isPLevels) {
    return new LevelLexicalScope(parent, group, isPLevels, true);
  }

  @NotNull
  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>();

    for (Statement statement : myGroup.getStatements()) {
      Abstract.LevelParameters params = myPLevels ? statement.getPLevelsDefinition() : statement.getHLevelsDefinition();
      if (params != null) {
        elements.addAll(params.getReferables());
      }
    }

    if (myWithOpens) {
      Scope cachingScope = null;
      for (Statement statement : myGroup.getStatements()) {
        NamespaceCommand cmd = statement.getNamespaceCommand();
        if (cmd == null) {
          continue;
        }

        Scope scope;
        if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
          scope = getImportedSubscope();
        } else {
          if (cachingScope == null) {
            cachingScope = CachingScope.make(new LevelLexicalScope(myParent, myGroup, myPLevels, false));
          }
          scope = cachingScope;
        }
        elements.addAll(NamespaceCommandNamespace.resolveNamespace(scope, cmd).getElements());
      }
    }

    elements.addAll(myParent.getElements());
    return elements;
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    if (name == null || name.isEmpty() || "_".equals(name)) {
      return null;
    }

    for (Statement statement : myGroup.getStatements()) {
      Abstract.LevelParameters params = myPLevels ? statement.getPLevelsDefinition() : statement.getHLevelsDefinition();
      if (params != null) {
        for (Referable ref : params.getReferables()) {
          if (ref.getRefName().equals(name)) {
            return ref;
          }
        }
      }
    }

    if (myWithOpens) {
      Scope cachingScope = null;
      for (Statement statement : myGroup.getStatements()) {
        NamespaceCommand cmd = statement.getNamespaceCommand();
        if (cmd == null) {
          continue;
        }

        Scope scope;
        if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
          scope = getImportedSubscope();
        } else {
          if (cachingScope == null) {
            cachingScope = CachingScope.make(new LevelLexicalScope(myParent, myGroup, myPLevels, false));
          }
          scope = cachingScope;
        }

        scope = NamespaceCommandNamespace.resolveNamespace(scope, cmd);
        Referable result = scope.resolveName(name);
        if (result != null) {
          return result;
        }
      }
    }

    return myParent.resolveName(name);
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    return myWithOpens ? new LevelLexicalScope(myParent, myGroup, myPLevels, false) : this;
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
