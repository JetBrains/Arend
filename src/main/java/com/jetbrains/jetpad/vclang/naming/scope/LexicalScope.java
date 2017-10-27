package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.ErrorReference;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference;
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
  private boolean myIgnoreExports;
  private final NamespaceCommand myCommand;

  private LexicalScope(Scope parent, Group group, boolean ignoreExports, NamespaceCommand cmd) {
    myParent = parent;
    myGroup = group;
    myCommand = cmd;
    myIgnoreExports = ignoreExports;
  }

  public static LexicalScope insideOf(Group group, Scope parent) {
    return new LexicalScope(parent, group, true, null);
  }

  public static LexicalScope opened(Group group) {
    return new LexicalScope(null, group, false, null);
  }

  public static LexicalScope exported(Group group) {
    return new LexicalScope(null, group, true, null);
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
      if (myIgnoreExports && kind == NamespaceCommand.Kind.EXPORT || myParent == null && kind != NamespaceCommand.Kind.EXPORT) {
        continue;
      }

      boolean isUsing = cmd.isUsing();
      Collection<? extends NameRenaming> opened = cmd.getOpenedReferences();
      Scope scope = Scope.Utils.resolveNamespace(new LexicalScope(myParent, myGroup, myIgnoreExports, cmd), cmd.getPath());
      if (scope == null || opened.isEmpty() && !isUsing) {
        continue;
      }

      for (NameRenaming renaming : opened) {
        Referable resolvedRef = renaming.getNewReferable();
        if (resolvedRef != null) {
          elements.add(resolvedRef);
        } else {
          resolvedRef = renaming.getOldReference();
          if (resolvedRef instanceof UnresolvedReference) {
            resolvedRef = ((UnresolvedReference) resolvedRef).resolve(scope);
          }
          if (!(resolvedRef instanceof ErrorReference)) {
            elements.add(resolvedRef);
          }
        }
      }

      if (isUsing) {
        Collection<? extends Referable> hidden = cmd.getHiddenReferences();
        Collection<? extends Referable> scopeElements = scope.getElements();
        elemLoop:
        for (Referable ref : scopeElements) {
          for (NameRenaming renaming : opened) {
            if (renaming.getOldReference().textRepresentation().equals(ref.textRepresentation())) {
              continue elemLoop;
            }
          }

          if (!(ref instanceof GlobalReferable && ((GlobalReferable) ref).isModule())) {
            for (Referable hiddenRef : hidden) {
              if (hiddenRef.textRepresentation().equals(ref.textRepresentation())) {
                continue elemLoop;
              }
            }

            for (NameRenaming renaming : opened) {
              if (renaming.getNewReferable() != null) {
                if (renaming.getOldReference().textRepresentation().equals(ref.textRepresentation())) {
                  continue elemLoop;
                }
              }
            }

            elements.add(ref);
          }
        }
      }
    }

    if (myParent != null) {
      elements.addAll(myParent.getElements());
    }

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

  private Object resolve(String name, boolean resolveRef, boolean includeModules) {
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

    cmdLoop:
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myCommand == cmd) {
        break;
      }
      NamespaceCommand.Kind kind = cmd.getKind();
      if (myIgnoreExports && kind == NamespaceCommand.Kind.EXPORT || myParent == null && kind != NamespaceCommand.Kind.EXPORT) {
        continue;
      }

      boolean isUsing = cmd.isUsing();
      Collection<? extends NameRenaming> opened = cmd.getOpenedReferences();
      Scope scope = Scope.Utils.resolveNamespace(new LexicalScope(myParent, myGroup, myIgnoreExports, cmd), cmd.getPath());
      if (scope == null || opened.isEmpty() && !isUsing) {
        continue;
      }

      for (NameRenaming renaming : opened) {
        Referable resolvedRef = renaming.getNewReferable();
        if (resolvedRef == null) {
          resolvedRef = renaming.getOldReference();
        }
        if (resolvedRef.textRepresentation().equals(name)) {
          if (resolveRef) {
            if (resolvedRef instanceof UnresolvedReference) {
              resolvedRef = ((UnresolvedReference) resolvedRef).resolve(scope);
            }
            return resolvedRef;
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
          if (renaming.getNewReferable() != null && renaming.getOldReference().textRepresentation().equals(name)) {
            continue cmdLoop;
          }
        }

        Object result = resolveRef ? scope.resolveName(name) : scope.resolveNamespace(name, false);
        if (result != null) {
          return result;
        }
      }
    }

    return myParent == null ? null : resolveRef ? myParent.resolveName(name) : myParent.resolveNamespace(name, includeModules);
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    Object result = resolve(name, true, true);
    return result instanceof Referable ? (Referable) result : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean includeModules) {
    Object result = resolve(name, false, includeModules);
    return result instanceof Scope ? (Scope) result : null;
  }
}
