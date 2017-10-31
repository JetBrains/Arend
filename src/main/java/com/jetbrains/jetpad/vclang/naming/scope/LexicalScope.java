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
  private final IgnoreFlag myIgnoreFlag;
  private final NamespaceCommand myCommand;

  enum IgnoreFlag {
    EXPORTS {
      @Override
      boolean ignoreExports() {
        return true;
      }

      @Override
      boolean ignoreOpens() {
        return false;
      }
    },

    ALL {
      @Override
      boolean ignoreExports() {
        return true;
      }

      @Override
      boolean ignoreOpens() {
        return true;
      }
    },

    OPENS {
      @Override
      boolean ignoreExports() {
        return false;
      }

      @Override
      boolean ignoreOpens() {
        return true;
      }
    };

    abstract boolean ignoreExports();
    abstract boolean ignoreOpens();
  }

  LexicalScope(Scope parent, Group group, IgnoreFlag ignoreFlag, NamespaceCommand cmd) {
    myParent = parent;
    myGroup = group;
    myCommand = cmd;
    myIgnoreFlag = ignoreFlag;
  }

  ImportedScope getImportedScope() {
    return null;
  }

  public static LexicalScope insideOf(Group group, Scope parent) {
    return new LexicalScope(parent, group, IgnoreFlag.EXPORTS, null);
  }

  public static LexicalScope opened(Group group) {
    return new LexicalScope(EmptyScope.INSTANCE, group, IgnoreFlag.OPENS, null);
  }

  public static LexicalScope exported(Group group) {
    return new LexicalScope(EmptyScope.INSTANCE, group, IgnoreFlag.ALL, null);
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

    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myCommand == cmd) {
        break;
      }
      NamespaceCommand.Kind kind = cmd.getKind();
      if (kind == NamespaceCommand.Kind.EXPORT && myIgnoreFlag.ignoreExports() || kind != NamespaceCommand.Kind.EXPORT && myIgnoreFlag.ignoreOpens()) {
        continue;
      }

      ImportedScope importedScope = getImportedScope();
      if ((kind == NamespaceCommand.Kind.IMPORT || kind == NamespaceCommand.Kind.EXPORT) && importedScope == null) {
        continue;
      }

      boolean isUsing = cmd.isUsing();
      Collection<? extends NameRenaming> opened = cmd.getOpenedReferences();
      Scope scope = Scope.Utils.resolveNamespace(kind == NamespaceCommand.Kind.IMPORT ? importedScope : new LexicalScope(kind == NamespaceCommand.Kind.EXPORT ? importedScope : myParent, myGroup, kind == NamespaceCommand.Kind.EXPORT ? IgnoreFlag.ALL : myIgnoreFlag, cmd), cmd.getPath(), kind != NamespaceCommand.Kind.EXPORT);
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

    if (myParent != null) {
      elements.addAll(myParent.getElements());
    }

    return elements;
  }

  private static Object resolveSubgroup(Group group, String name, boolean resolveRef, boolean includeExports) {
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
      return resolveRef ? ref : includeExports ? LexicalScope.opened(group) : LexicalScope.exported(group);
    }

    return null;
  }

  private Object resolve(String name, boolean resolveRef, boolean resolveModuleNames, boolean includeExports) {
    for (Group subgroup : myGroup.getSubgroups()) {
      Object result = resolveSubgroup(subgroup, name, resolveRef, includeExports);
      if (result != null) {
        return result;
      }
    }
    for (Group subgroup : myGroup.getDynamicSubgroups()) {
      Object result = resolveSubgroup(subgroup, name, resolveRef, includeExports);
      if (result != null) {
        return result;
      }
    }

    cmdLoop:
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myCommand == cmd) {
        break;
      }
      NamespaceCommand.Kind kind = cmd.getKind();
      if (kind == NamespaceCommand.Kind.EXPORT && myIgnoreFlag.ignoreExports() || kind != NamespaceCommand.Kind.EXPORT && myIgnoreFlag.ignoreOpens()) {
        continue;
      }

      ImportedScope importedScope = getImportedScope();
      if ((kind == NamespaceCommand.Kind.IMPORT || kind == NamespaceCommand.Kind.EXPORT) && importedScope == null) {
        continue;
      }

      boolean isUsing = cmd.isUsing();
      Collection<? extends NameRenaming> opened = cmd.getOpenedReferences();
      Scope scope = Scope.Utils.resolveNamespace(kind == NamespaceCommand.Kind.IMPORT ? importedScope : new LexicalScope(kind == NamespaceCommand.Kind.EXPORT ? importedScope : myParent, myGroup, kind == NamespaceCommand.Kind.EXPORT ? IgnoreFlag.ALL : myIgnoreFlag, cmd), cmd.getPath(), kind != NamespaceCommand.Kind.EXPORT);
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
            return scope.resolveNamespace(name, true, includeExports);
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

        Object result = resolveRef ? scope.resolveName(name) : scope.resolveNamespace(name, false, includeExports);
        if (result != null) {
          return result instanceof GlobalReferable && ((GlobalReferable) result).isModule() ? null : result;
        }
      }
    }

    return myParent == null ? null : resolveRef ? myParent.resolveName(name) : myParent.resolveNamespace(name, resolveModuleNames, includeExports);
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    Object result = resolve(name, true, true, true);
    return result instanceof Referable ? (Referable) result : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean resolveModuleNames, boolean includeExports) {
    Object result = resolve(name, false, resolveModuleNames, includeExports);
    return result instanceof Scope ? (Scope) result : null;
  }
}
