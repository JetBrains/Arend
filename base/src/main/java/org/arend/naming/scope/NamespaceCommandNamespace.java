package org.arend.naming.scope;

import org.arend.naming.reference.*;
import org.arend.term.NameHiding;
import org.arend.term.NameRenaming;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.AccessModifier;
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

  private Referable resolve(NamedUnresolvedReference ref, ScopeContext context) {
    return ref.resolve(new PrivateFilteredScope(myModuleNamespace, true), null, context);
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getElements(@Nullable ScopeContext context) {
    Set<String> hidden = new HashSet<>();
    for (NameHiding hiddenElement : myNamespaceCommand.getHiddenReferences()) {
      if (context == null || hiddenElement.getScopeContext() == context) {
        hidden.add(hiddenElement.getHiddenReference().getRefName());
      }
    }

    List<Referable> elements = new ArrayList<>();
    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      ScopeContext renamingContext = renaming.getScopeContext();
      if (!(context == null || context == renamingContext)) {
        continue;
      }
      Referable oldRef = resolve(renaming.getOldReference(), renamingContext);
      if (!(oldRef instanceof ErrorReference || oldRef instanceof GlobalReferable && ((GlobalReferable) oldRef).getAccessModifier() == AccessModifier.PRIVATE)) {
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
      for (Referable ref : myModuleNamespace.getElements(context)) {
        if (hidden.contains(ref.textRepresentation()) || ref instanceof GlobalReferable && ((GlobalReferable) ref).getAccessModifier() != AccessModifier.PUBLIC) {
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

  private boolean isHidden(String name, ScopeContext context) {
    for (NameHiding hiddenRef : myNamespaceCommand.getHiddenReferences()) {
      if ((context == null || context == hiddenRef.getScopeContext()) && hiddenRef.getHiddenReference().getRefName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private boolean isHiddenByUsing(String name) {
    if (!myNamespaceCommand.isUsing()) {
      return true;
    }

    for (NameRenaming renaming : myNamespaceCommand.getOpenedReferences()) {
      if (renaming.getOldReference().textRepresentation().equals(name)) {
        return true;
      }
    }

    return false;
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, @Nullable ScopeContext context) {
    if (isHidden(name, context)) {
      return null;
    }

    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      ScopeContext renamingContext = renaming.getScopeContext();
      if (!(context == null || context == renamingContext)) {
        continue;
      }

      String newName = renaming.getName();
      NamedUnresolvedReference oldRef = renaming.getOldReference();
      if (name.equals(newName) || newName == null && oldRef.getRefName().equals(name)) {
        Referable ref = resolve(oldRef, renamingContext);
        return ref instanceof ErrorReference ? null : newName != null ? new RedirectingReferableImpl(ref, renaming.getPrecedence(), newName) : ref;
      }
    }

    if (isHiddenByUsing(name)) return null;
    Referable ref = myModuleNamespace.resolveName(name, context);
    return ref instanceof GlobalReferable && ((GlobalReferable) ref).getAccessModifier() != AccessModifier.PUBLIC ? null : ref;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name) {
    if (isHidden(name, ScopeContext.STATIC)) {
      return null;
    }

    Collection<? extends NameRenaming> opened = myNamespaceCommand.getOpenedReferences();
    for (NameRenaming renaming : opened) {
      if (renaming.getScopeContext() != ScopeContext.STATIC) {
        continue;
      }

      String newName = renaming.getName();
      NamedUnresolvedReference oldRef = renaming.getOldReference();
      if (newName == null) {
        newName = resolve(oldRef, ScopeContext.STATIC).getRefName();
      }
      if (newName.equals(name)) {
        return myModuleNamespace.resolveNamespace(oldRef.getRefName());
      }
    }

    return isHiddenByUsing(name) ? null : myModuleNamespace.resolveNamespace(name);
  }
}
