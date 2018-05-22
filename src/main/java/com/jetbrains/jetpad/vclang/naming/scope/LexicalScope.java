package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;
import com.jetbrains.jetpad.vclang.term.group.Group;

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
      elements.addAll(new ClassFieldImplScope((ClassReferable) groupRef, false).getElements());
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

      Scope scope;
      if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        scope = getImportedSubscope();
      } else {
        if (cachingScope == null) {
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, true));
        }
        scope = cachingScope;
      }
      elements.addAll(NamespaceCommandNamespace.makeNamespace(Scope.Utils.resolveNamespace(scope, cmd.getPath()), cmd).getElements());
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
      Referable referable = new ClassFieldImplScope((ClassReferable) group.getReferable(), false).resolveName(name);
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

  private Object resolve(String name, boolean resolveRef) {
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
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (myIgnoreOpens && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
        break;
      }

      Scope scope;
      if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        scope = getImportedSubscope();
      } else {
        if (cachingScope == null) {
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, true));
        }
        scope = cachingScope;
      }

      scope = NamespaceCommandNamespace.makeNamespace(Scope.Utils.resolveNamespace(scope, cmd.getPath()), cmd);
      Object result = resolveRef ? scope.resolveName(name) : scope.resolveNamespace(name);
      if (result != null) {
        return result;
      }
    }

    return resolveRef ? myParent.resolveName(name) : myParent.resolveNamespace(name);
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    Object result = resolve(name, true);
    return result instanceof Referable ? (Referable) result : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name) {
    Object result = resolve(name, false);
    return result instanceof Scope ? (Scope) result : null;
  }

  @Nonnull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    return myIgnoreOpens ? this : new LexicalScope(myParent, myGroup, true);
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
