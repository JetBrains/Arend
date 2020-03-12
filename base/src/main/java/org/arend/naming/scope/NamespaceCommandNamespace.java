package org.arend.naming.scope;

import org.arend.naming.reference.ErrorReference;
import org.arend.naming.reference.RedirectingReferableImpl;
import org.arend.naming.reference.Referable;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NamespaceCommandNamespace implements Scope {
  private final Scope myModuleNamespace;
  private final NamespaceCommand myNamespaceCommand;

  private NamespaceCommandNamespace(Scope moduleNamespace, NamespaceCommand namespaceCommand) {
    myNamespaceCommand = namespaceCommand;
    myModuleNamespace = moduleNamespace;
  }

  public static @NotNull Scope makeNamespace(Scope moduleNamespace, NamespaceCommand namespaceCommand) {
    return moduleNamespace == null || namespaceCommand.getOpenedReferences().isEmpty() && !namespaceCommand.isUsing() ? EmptyScope.INSTANCE : new NamespaceCommandNamespace(moduleNamespace, namespaceCommand);
  }

  public static @NotNull Scope resolveNamespace(Scope parentScope, NamespaceCommand cmd) {
    if (cmd.getOpenedReferences().isEmpty() && !cmd.isUsing()) {
      return EmptyScope.INSTANCE;
    }
    List<String> path = cmd.getPath();
    if (path.isEmpty()) {
      return EmptyScope.INSTANCE;
    }
    parentScope = parentScope == null ? null : parentScope.resolveNamespace(path);
    return parentScope == null ? EmptyScope.INSTANCE : new NamespaceCommandNamespace(parentScope, cmd);
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getElements() {
    Set<String> hidden = new HashSet<>();
    for (Referable hiddenElement : myNamespaceCommand.getHiddenReferences()) {
      hidden.add(hiddenElement.textRepresentation());
    }

    List<Referable> elements = new ArrayList<>();
    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      Referable oldRef = ExpressionResolveNameVisitor.resolve(renaming.getOldReference(), myModuleNamespace);
      if (!(oldRef == null || oldRef instanceof ErrorReference)) {
        String newName = renaming.getName();
        String name = newName != null ? newName : oldRef.textRepresentation();
        if (!hidden.contains(name)) {
          elements.add(newName != null ? new RedirectingReferableImpl(oldRef, renaming.getPrecedence(), newName) : oldRef);
        }
        hidden.add(oldRef.textRepresentation());
      }
    }

    if (myNamespaceCommand.isUsing()) {
      elemLoop:
      for (Referable ref : myModuleNamespace.getElements()) {
        if (hidden.contains(ref.textRepresentation())) {
          continue;
        }

        for (NameRenaming renaming : opened) {
          if (renaming.getOldReference().textRepresentation().equals(ref.textRepresentation())) {
            continue elemLoop;
          }
        }

        elements.add(ref);
      }
    }

    return elements;
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    for (Referable hiddenRef : myNamespaceCommand.getHiddenReferences()) {
      if (hiddenRef.textRepresentation().equals(name)) {
        return null;
      }
    }

    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      String newName = renaming.getName();
      Referable oldRef = renaming.getOldReference();
      if ((newName != null ? newName : oldRef.textRepresentation()).equals(name)) {
        oldRef = ExpressionResolveNameVisitor.resolve(oldRef, myModuleNamespace);
        return oldRef == null || oldRef instanceof ErrorReference ? null : newName != null ? new RedirectingReferableImpl(oldRef, renaming.getPrecedence(), newName) : oldRef;
      }
    }

    if (!myNamespaceCommand.isUsing()) {
      return null;
    }

    for (NameRenaming renaming : opened) {
      if (renaming.getOldReference().textRepresentation().equals(name)) {
        return null;
      }
    }

    return myModuleNamespace.resolveName(name);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    for (Referable hiddenRef : myNamespaceCommand.getHiddenReferences()) {
      if (hiddenRef.textRepresentation().equals(name)) {
        return null;
      }
    }

    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      String newName = renaming.getName();
      Referable oldRef = renaming.getOldReference();
      if ((newName != null ? newName : oldRef.textRepresentation()).equals(name)) {
        return myModuleNamespace.resolveNamespace(oldRef.textRepresentation(), onlyInternal);
      }
    }

    if (!myNamespaceCommand.isUsing()) {
      return null;
    }

    for (NameRenaming renaming : opened) {
      if (renaming.getOldReference().textRepresentation().equals(name)) {
        return null;
      }
    }

    return myModuleNamespace.resolveNamespace(name, onlyInternal);
  }
}
