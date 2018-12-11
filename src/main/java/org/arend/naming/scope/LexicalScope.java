package org.arend.naming.scope;

import org.arend.module.ModulePath;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.Group;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("Duplicates")
public class LexicalScope implements Scope {
  private final Scope myParent;
  private final Group myGroup;
  private final ModulePath myModule;
  private final boolean myIgnoreOpens;

  private LexicalScope(Scope parent, Group group, ModulePath module, boolean ignoreOpens) {
    myParent = parent;
    myGroup = group;
    myModule = module;
    myIgnoreOpens = ignoreOpens;
  }

  private boolean ignoreOpens() {
    return myIgnoreOpens;
  }

  public static LexicalScope insideOf(Group group, Scope parent) {
    return new LexicalScope(parent, group, group.getReferable().getLocation(), false);
  }

  public static LexicalScope opened(Group group) {
    return new LexicalScope(EmptyScope.INSTANCE, group, null, true);
  }

  private void addReferable(Referable referable, List<Referable> elements) {
    String name = referable.textRepresentation();
    if (!name.isEmpty() && !"_".equals(name)) {
      elements.add(referable);
    }
  }

  private void addSubgroups(Collection<? extends Group> subgroups, List<Referable> elements) {
    for (Group subgroup : subgroups) {
      addReferable(subgroup.getReferable(), elements);
      for (Group.InternalReferable constructor : subgroup.getConstructors()) {
        if (constructor.isVisible()) {
          addReferable(constructor.getReferable(), elements);
        }
      }
      for (Group.InternalReferable field : subgroup.getFields()) {
        if (field.isVisible()) {
          addReferable(field.getReferable(), elements);
        }
      }
    }
  }

  @Nonnull
  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>();
    for (Group.InternalReferable constructor : myGroup.getConstructors()) {
      addReferable(constructor.getReferable(), elements);
    }
    GlobalReferable groupRef = myGroup.getReferable();
    if (groupRef instanceof ClassReferable) {
      elements.addAll(new ClassFieldImplScope((ClassReferable) groupRef, false).getElements());
    } else {
      for (Group.InternalReferable field : myGroup.getFields()) {
        addReferable(field.getReferable(), elements);
      }
    }

    addSubgroups(myGroup.getSubgroups(), elements);
    addSubgroups(myGroup.getDynamicSubgroups(), elements);

    Scope cachingScope = null;
    for (NamespaceCommand cmd : myGroup.getNamespaceCommands()) {
      if (ignoreOpens() && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
        continue;
      }

      Scope scope;
      if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        if (myModule != null && cmd.getPath().equals(myModule.toList())) {
          continue;
        }
        scope = getImportedSubscope();
      } else {
        if (cachingScope == null) {
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, true));
        }
        scope = cachingScope;
      }
      elements.addAll(NamespaceCommandNamespace.resolveNamespace(scope, cmd).getElements());
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
    Referable ref = group.getReferable();
    if (ref.textRepresentation().equals(name)) {
      return resolveRef ? ref : LexicalScope.opened(group);
    }

    if (resolveRef) {
      GlobalReferable result = resolveInternal(group, name, true);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  private Object resolve(String name, boolean resolveRef) {
    if (name == null || name.isEmpty() || "_".equals(name)) {
      return null;
    }

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
      if (ignoreOpens() && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
        continue;
      }

      Scope scope;
      if (cmd.getKind() == NamespaceCommand.Kind.IMPORT) {
        if (myModule != null && cmd.getPath().equals(myModule.toList())) {
          continue;
        }
        scope = getImportedSubscope();
      } else {
        if (cachingScope == null) {
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, true));
        }
        scope = cachingScope;
      }

      scope = NamespaceCommandNamespace.resolveNamespace(scope, cmd);
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
    return ignoreOpens() ? this : new LexicalScope(myParent, myGroup, null, true);
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
