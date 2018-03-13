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

  private LexicalScope(Scope parent, Group group, boolean ignoreOpens) {
    myParent = parent;
    myGroup = group;
    myIgnoreOpens = ignoreOpens;
  }

  public static LexicalScope insideOf(Group group, Scope parent) {
    return new LexicalScope(parent, group, false);
  }

  public static LexicalScope opened(Group group) {
    return new LexicalScope(EmptyScope.INSTANCE, group, true);
  }

  private void addSubgroups(Collection<? extends Group> subgroups, List<Referable> elements) {
    for (Group subgroup : subgroups) {
      for (Group.InternalReferable constructor : subgroup.getConstructors()) {
        if (constructor.isVisible()) {
          elements.add(constructor.getReferable());
        }
      }
      for (Group.InternalReferable field : subgroup.getFields()) {
        if (field.isVisible()) {
          elements.add(field.getReferable());
        }
      }
      elements.add(subgroup.getReferable());
    }
  }

  @Nonnull
  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>();
    for (Group.InternalReferable constructor : myGroup.getConstructors()) {
      elements.add(constructor.getReferable());
    }
    GlobalReferable groupRef = myGroup.getReferable();
    if (groupRef instanceof ClassReferable) {
      elements.addAll(new ClassFieldImplScope((ClassReferable) groupRef).getElements());
    } else {
      for (Group.InternalReferable field : myGroup.getFields()) {
        elements.add(field.getReferable());
      }
    }

    addSubgroups(myGroup.getSubgroups(), elements);
    addSubgroups(myGroup.getDynamicSubgroups(), elements);

    Scope cachingScope = null;
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myIgnoreOpens && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
        break;
      }

      boolean isUsing = cmd.isUsing();
      Scope scope;
      if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        scope = getImportedSubscope();
      } else {
        if (cachingScope == null) {
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, true));
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

  private static GlobalReferable resolveInternal(Group group, String name, boolean onlyVisible) {
    for (Group.InternalReferable internalReferable : group.getConstructors()) {
      if (!onlyVisible || internalReferable.isVisible()) {
        GlobalReferable constructor = internalReferable.getReferable();
        if (constructor.textRepresentation().equals(name)) {
          return constructor;
        }
      }
    }

    if (onlyVisible || !(group.getReferable() instanceof ClassReferable)) {
      for (Group.InternalReferable internalReferable : group.getFields()) {
        if (!onlyVisible || internalReferable.isVisible()) {
          GlobalReferable field = internalReferable.getReferable();
          if (field.textRepresentation().equals(name)) {
            return field;
          }
        }
      }
    } else {
      Referable referable = new ClassFieldImplScope((ClassReferable) group.getReferable()).resolveName(name);
      return referable instanceof GlobalReferable ? (GlobalReferable) referable : null;
    }

    return null;
  }

  private static Object resolveSubgroup(Group group, String name, boolean resolveRef) {
    if (resolveRef) {
      GlobalReferable result = resolveInternal(group, name, true);
      if (result != null) {
        return result;
      }
    }

    Referable ref = group.getReferable();
    if (ref.textRepresentation().equals(name)) {
      return resolveRef ? ref : LexicalScope.opened(group);
    }

    return null;
  }

  private Object resolve(String name, boolean resolveRef, boolean resolveModuleNames) {
    if (resolveRef) {
      Object result = resolveInternal(myGroup, name, false);
      if (result != null) {
        return result;
      }
    }

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

    Scope cachingScope = null;
    cmdLoop:
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myIgnoreOpens && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
        break;
      }

      boolean isUsing = cmd.isUsing();
      Scope scope;
      if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        scope = getImportedSubscope();
      } else {
        if (cachingScope == null) {
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, true));
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

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
