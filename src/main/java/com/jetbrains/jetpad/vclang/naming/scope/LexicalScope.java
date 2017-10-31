package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.*;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.NameRenaming;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LexicalScope implements Scope {
  private final Scope myParent;
  private final Group myGroup;
  private final boolean myIgnoreOpens;

  LexicalScope(Scope parent, Group group, boolean ignoreOpens) {
    myParent = parent;
    myGroup = group;
    myIgnoreOpens = ignoreOpens;
  }

  ImportedScope getImportedScope() {
    return null;
  }

  public static LexicalScope insideOf(Group group, Scope parent) {
    return new LexicalScope(parent, group, false);
  }

  public static LexicalScope opened(Group group) {
    return new LexicalScope(EmptyScope.INSTANCE, group, true);
  }

  @Nonnull
  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>();

    for (Group subgroup : myGroup.getSubgroups()) {
      elements.addAll(subgroup.getConstructors());
      elements.addAll(subgroup.getFields());
      elements.add(subgroup.getReferable());
    }
    for (Group subgroup : myGroup.getDynamicSubgroups()) {
      elements.addAll(subgroup.getConstructors());
      elements.addAll(subgroup.getFields());
      elements.add(subgroup.getReferable());
    }

    CachingScope cachingScope = null;
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myIgnoreOpens) {
        break;
      }

      boolean isUsing = cmd.isUsing();
      Scope scope;
      if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        scope = getImportedScope();
      } else {
        if (cachingScope == null) {
          cachingScope = new CachingScope(new LexicalScope(myParent, myGroup, true));
        }
        scope = cachingScope;
      }
      scope = Scope.Utils.resolveNamespace(scope, cmd.getPath());
      Collection<? extends NameRenaming> opened = cmd.getOpenedReferences();
      if (scope == null || opened.isEmpty() && !isUsing) {
        continue;
      }

      for (NameRenaming renaming : opened) {
        Referable oldRef = renaming.getOldReference();
        if (oldRef instanceof UnresolvedReference) {
          oldRef = ((UnresolvedReference) oldRef).resolve(scope);
        }
        if (!(oldRef instanceof ErrorReference)) {
          Referable newRef = renaming.getNewReferable();
          elements.add(newRef != null ? new RedirectingReferableImpl(oldRef, renaming.getPrecedence(), newRef) : oldRef);
        }
      }

      if (isUsing) {
        Collection<? extends Referable> hidden = cmd.getHiddenReferences();
        Collection<? extends Referable> scopeElements = scope.getElements();
        elemLoop:
        for (Referable ref : scopeElements) {
          if (ref instanceof GlobalReferable && ((GlobalReferable) ref).isModule()) {
            continue;
          }

          for (Referable hiddenRef : hidden) {
            if (hiddenRef.textRepresentation().equals(ref.textRepresentation())) {
              continue elemLoop;
            }
          }

          for (NameRenaming renaming : opened) {
            if (renaming.getOldReference().textRepresentation().equals(ref.textRepresentation())) {
              continue elemLoop;
            }
          }

          elements.add(ref);
        }
      }
    }

    elements.addAll(myParent.getElements());
    return elements;
  }

  private static Object resolveSubgroup(Group group, String name, boolean resolveRef) {
    if (resolveRef) {
      for (GlobalReferable referable : group.getConstructors()) {
        if (referable.textRepresentation().equals(name)) {
          return referable;
        }
      }

      for (GlobalReferable referable : group.getFields()) {
        if (referable.textRepresentation().equals(name)) {
          return referable;
        }
      }
    }

    Referable ref = group.getReferable();
    if (ref.textRepresentation().equals(name)) {
      return resolveRef ? ref : LexicalScope.opened(group);
    }

    return null;
  }

  private Object resolve(String name, boolean resolveRef, boolean resolveModuleNames) {
    for (Group subgroup : myGroup.getSubgroups()) {
      Object result = resolveSubgroup(subgroup, name, resolveRef);
      if (result != null) {
        return result;
      }
    }
    for (Group subgroup : myGroup.getDynamicSubgroups()) {
      Object result = resolveSubgroup(subgroup, name, resolveRef);
      if (result != null) {
        return result;
      }
    }

    CachingScope cachingScope = null;
    cmdLoop:
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myIgnoreOpens) {
        break;
      }

      boolean isUsing = cmd.isUsing();
      Scope scope;
      if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        scope = getImportedScope();
      } else {
        if (cachingScope == null) {
          cachingScope = new CachingScope(new LexicalScope(myParent, myGroup, true));
        }
        scope = cachingScope;
      }
      scope = Scope.Utils.resolveNamespace(scope, cmd.getPath());
      Collection<? extends NameRenaming> opened = cmd.getOpenedReferences();
      if (scope == null || opened.isEmpty() && !isUsing) {
        continue;
      }

      for (NameRenaming renaming : opened) {
        Referable newRef = renaming.getNewReferable();
        Referable oldRef = renaming.getOldReference();
        if ((newRef != null ? newRef : oldRef).textRepresentation().equals(name)) {
          if (resolveRef) {
            if (oldRef instanceof UnresolvedReference) {
              oldRef = ((UnresolvedReference) oldRef).resolve(scope);
            }
            return newRef != null ? new RedirectingReferableImpl(oldRef, renaming.getPrecedence(), newRef) : oldRef;
          } else {
            return scope.resolveNamespace(name, true);
          }
        }
      }

      if (isUsing) {
        Collection<? extends Referable> hidden = cmd.getHiddenReferences();
        for (Referable hiddenRef : hidden) {
          if (hiddenRef.textRepresentation().equals(name)) {
            continue cmdLoop;
          }
        }

        for (NameRenaming renaming : opened) {
          if (renaming.getOldReference().textRepresentation().equals(name)) {
            continue cmdLoop;
          }
        }

        Object result = resolveRef ? scope.resolveName(name) : scope.resolveNamespace(name, false);
        if (result != null) {
          return result instanceof GlobalReferable && ((GlobalReferable) result).isModule() ? null : result;
        }
      }
    }

    return resolveRef ? myParent.resolveName(name) : myParent.resolveNamespace(name, resolveModuleNames);
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    Object result = resolve(name, true, true);
    return result instanceof Referable ? (Referable) result : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean resolveModuleNames) {
    Object result = resolve(name, false, resolveModuleNames);
    return result instanceof Scope ? (Scope) result : null;
  }
}
