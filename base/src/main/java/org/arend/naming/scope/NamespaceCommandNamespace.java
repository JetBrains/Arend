package org.arend.naming.scope;

import org.arend.naming.reference.*;
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

  @Override
  public @NotNull Collection<? extends Referable> getAllElements() {
    return getElements(null);
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getElements(Referable.RefKind refKind) {
    Set<String> hidden = new HashSet<>();
    for (Referable hiddenElement : myNamespaceCommand.getHiddenReferences()) {
      Referable oldRef = ExpressionResolveNameVisitor.resolve(hiddenElement, myModuleNamespace, null);
      hidden.add(oldRef.textRepresentation());
      String alias = oldRef instanceof GlobalReferable ? ((GlobalReferable) oldRef).getAliasName() : null;
      if (alias != null) {
        hidden.add(alias);
      }
    }

    List<Referable> elements = new ArrayList<>();
    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      Referable oldRef = ExpressionResolveNameVisitor.resolve(renaming.getOldReference(), myModuleNamespace, null);
      if (!(oldRef == null || oldRef instanceof ErrorReference)) {
        String newName = renaming.getName();
        String name = newName != null ? newName : oldRef.textRepresentation();
        String alias = oldRef instanceof GlobalReferable ? ((GlobalReferable) oldRef).getAliasName() : null;
        if (!hidden.contains(name) && (alias == null || !hidden.contains(alias))) {
          if (refKind == null || oldRef.getRefKind() == refKind) {
            elements.add(newName != null ? new RedirectingReferableImpl(oldRef, renaming.getPrecedence(), newName) : oldRef);
            if (alias != null && newName == null) {
              elements.add(new AliasReferable((GlobalReferable) oldRef));
            }
          }
        }
        hidden.add(oldRef.textRepresentation());
        if (alias != null) {
          hidden.add(alias);
        }
      }
    }

    if (myNamespaceCommand.isUsing()) {
      elemLoop:
      for (Referable ref : refKind == null ? myModuleNamespace.getAllElements() : myModuleNamespace.getElements(refKind)) {
        if (hidden.contains(ref.textRepresentation())) {
          continue;
        }

        for (NameRenaming renaming : opened) {
          if (renaming.getOldReference().textRepresentation().equals(ref.textRepresentation())) {
            continue elemLoop;
          }
        }

        if (refKind == null || ref.getRefKind() == refKind) {
          elements.add(ref);
        }
      }
    }

    return elements;
  }

  private boolean isHidden(String name, Referable.RefKind kind) {
    for (Referable hiddenRef : myNamespaceCommand.getHiddenReferences()) {
      Referable oldRef = ExpressionResolveNameVisitor.resolve(hiddenRef, myModuleNamespace, kind);
      if (oldRef.getRefName().equals(name)) {
        return true;
      }
      String alias = oldRef instanceof GlobalReferable ? ((GlobalReferable) oldRef).getAliasName() : null;
      if (alias != null && alias.equals(name)) {
        return true;
      }
    }
    return false;
  }

  private boolean isHiddenByUsing(String name, Referable.RefKind kind) {
    if (!myNamespaceCommand.isUsing()) {
      return true;
    }

    for (NameRenaming renaming : myNamespaceCommand.getOpenedReferences()) {
      Referable oldRef = ExpressionResolveNameVisitor.resolve(renaming.getOldReference(), myModuleNamespace, kind);
      if (oldRef.textRepresentation().equals(name)) {
        return true;
      }
      String alias = oldRef instanceof GlobalReferable ? ((GlobalReferable) oldRef).getAliasName() : null;
      if (alias != null && alias.equals(name)) {
        return true;
      }
    }

    return false;
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, Referable.RefKind kind) {
    if (isHidden(name, kind)) {
      return null;
    }

    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      String newName = renaming.getName();
      Referable oldRef = renaming.getOldReference();
      boolean ok;
      if (newName != null) {
        ok = newName.equals(name);
      } else {
        oldRef = ExpressionResolveNameVisitor.resolve(oldRef, myModuleNamespace, kind);
        ok = oldRef.getRefName().equals(name);
        if (!ok) {
          String alias = oldRef instanceof GlobalReferable ? ((GlobalReferable) oldRef).getAliasName() : null;
          ok = alias != null && alias.equals(name);
        }
      }
      if (ok) {
        oldRef = ExpressionResolveNameVisitor.resolve(oldRef, myModuleNamespace, kind);
        return oldRef == null || oldRef instanceof ErrorReference ? null : newName != null ? new RedirectingReferableImpl(oldRef, renaming.getPrecedence(), newName) : oldRef;
      }
    }

    return isHiddenByUsing(name, kind) ? null : myModuleNamespace.resolveName(name, kind);
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    if (isHidden(name, Referable.RefKind.EXPR)) {
      return null;
    }

    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      String newName = renaming.getName();
      Referable oldRef = renaming.getOldReference();
      boolean ok;
      if (newName != null) {
        ok = newName.equals(name);
      } else {
        oldRef = ExpressionResolveNameVisitor.resolve(oldRef, myModuleNamespace);
        ok = oldRef.getRefName().equals(name);
        if (!ok) {
          String alias = oldRef instanceof GlobalReferable ? ((GlobalReferable) oldRef).getAliasName() : null;
          ok = alias != null && alias.equals(name);
        }
      }
      if (ok) {
        return myModuleNamespace.resolveNamespace(oldRef.getRefName(), onlyInternal);
      }
    }

    return isHiddenByUsing(name, Referable.RefKind.EXPR) ? null : myModuleNamespace.resolveNamespace(name, onlyInternal);
  }
}
