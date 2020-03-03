package org.arend.naming.scope;

import org.arend.ext.module.ModulePath;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.Group;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("Duplicates")
public class LexicalScope implements Scope {
  private final Scope myParent;
  private final Group myGroup;
  private final ModulePath myModule;
  private final Kind myKind;
  private final boolean myOnlyExternal;

  private enum Kind { INSIDE, OPENED, OPENED_INTERNAL }

  private LexicalScope(Scope parent, Group group, ModulePath module, Kind kind, boolean onlyExternal) {
    myParent = parent;
    myGroup = group;
    myModule = module;
    myKind = kind;
    myOnlyExternal = onlyExternal;
  }

  private boolean ignoreOpens() {
    return myKind == Kind.OPENED || myKind == Kind.OPENED_INTERNAL;
  }

  public static LexicalScope insideOf(Group group, Scope parent, boolean onlyExternal) {
    return new LexicalScope(parent, group, group.getReferable().getLocation(), Kind.INSIDE, onlyExternal);
  }

  public static LexicalScope insideOf(Group group, Scope parent) {
    return insideOf(group, parent, false);
  }

  private static LexicalScope opened(Group group, boolean onlyInternal) {
    return new LexicalScope(EmptyScope.INSTANCE, group, null, onlyInternal ? Kind.OPENED_INTERNAL : Kind.OPENED, false);
  }

  public static LexicalScope opened(Group group) {
    return opened(group, false);
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
      for (Group.InternalReferable internalRef : subgroup.getInternalReferables()) {
        if (internalRef.isVisible()) {
          addReferable(internalRef.getReferable(), elements);
        }
      }
    }
  }

  @NotNull
  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>();
    if (!myOnlyExternal) {
      for (Group.InternalReferable constructor : myGroup.getConstructors()) {
        addReferable(constructor.getReferable(), elements);
      }
      GlobalReferable groupRef = myGroup.getReferable();
      if (myKind != Kind.OPENED_INTERNAL && groupRef instanceof ClassReferable) {
        elements.addAll(new ClassFieldImplScope((ClassReferable) groupRef, false).getElements());
      } else {
        for (Group.InternalReferable field : myGroup.getFields()) {
          addReferable(field.getReferable(), elements);
        }
      }
    }

    addSubgroups(myGroup.getSubgroups(), elements);
    if (!myOnlyExternal) {
      addSubgroups(myGroup.getDynamicSubgroups(), elements);
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
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, Kind.OPENED, myOnlyExternal));
        }
        scope = cachingScope;
      }
      elements.addAll(NamespaceCommandNamespace.resolveNamespace(scope, cmd).getElements());
    }

    elements.addAll(myParent.getElements());
    return elements;
  }

  private static GlobalReferable resolveInternal(Group group, String name, boolean onlyInternal) {
    for (Group.InternalReferable internalReferable : group.getConstructors()) {
      if (!onlyInternal || internalReferable.isVisible()) {
        GlobalReferable constructor = internalReferable.getReferable();
        if (constructor.textRepresentation().equals(name)) {
          return constructor;
        }
      }
    }

    if (onlyInternal || !(group.getReferable() instanceof ClassReferable)) {
      for (Group.InternalReferable internalReferable : group.getFields()) {
        if (!onlyInternal || internalReferable.isVisible()) {
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

  private static Object resolveSubgroup(Group group, String name, ResolveType resolveType) {
    Referable ref = group.getReferable();
    if (ref.textRepresentation().equals(name)) {
      return resolveType == ResolveType.REF ? ref : LexicalScope.opened(group, resolveType == ResolveType.INTERNAL_SCOPE);
    }

    if (resolveType == ResolveType.REF) {
      return resolveInternal(group, name, true);
    }

    return null;
  }

  private enum ResolveType { REF, SCOPE, INTERNAL_SCOPE }

  private Object resolve(String name, ResolveType resolveType) {
    if (name == null || name.isEmpty() || "_".equals(name)) {
      return null;
    }

    if (resolveType == ResolveType.REF && !myOnlyExternal) {
      Object result = resolveInternal(myGroup, name, myKind == Kind.OPENED_INTERNAL);
      if (result != null) {
        return result;
      }
    }

    for (Group subgroup : myGroup.getSubgroups()) {
      Object result = resolveSubgroup(subgroup, name, resolveType);
      if (result != null) {
        return result;
      }
    }
    if (!myOnlyExternal) {
      for (Group subgroup : myGroup.getDynamicSubgroups()) {
        Object result = resolveSubgroup(subgroup, name, resolveType);
        if (result != null) {
          return result;
        }
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
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, Kind.OPENED, myOnlyExternal));
        }
        scope = cachingScope;
      }

      scope = NamespaceCommandNamespace.resolveNamespace(scope, cmd);
      Object result = resolveType == ResolveType.REF ? scope.resolveName(name) : scope.resolveNamespace(name, resolveType == ResolveType.INTERNAL_SCOPE);
      if (result != null) {
        return result;
      }
    }

    return resolveType == ResolveType.REF ? myParent.resolveName(name) : myParent.resolveNamespace(name, resolveType == ResolveType.INTERNAL_SCOPE);
  }

  @Nullable
  @Override
  public Referable resolveName(String name) {
    Object result = resolve(name, ResolveType.REF);
    return result instanceof Referable ? (Referable) result : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(String name, boolean onlyInternal) {
    Object result = resolve(name, onlyInternal ? ResolveType.INTERNAL_SCOPE : ResolveType.SCOPE);
    return result instanceof Scope ? (Scope) result : null;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens() {
    return ignoreOpens() ? this : new LexicalScope(myParent, myGroup, null, Kind.OPENED, myOnlyExternal);
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
