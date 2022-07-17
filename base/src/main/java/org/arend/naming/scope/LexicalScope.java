package org.arend.naming.scope;

import org.arend.ext.module.ModulePath;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.AliasReferable;
import org.arend.naming.reference.ClassReferable;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.Referable;
import org.arend.term.NamespaceCommand;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class LexicalScope implements Scope {
  private final Scope myParent;
  private final Group myGroup;
  private final ModulePath myModule;
  private final Kind myKind;
  private final Extent myExtent;
  private final Scope.Kind myScopeKind;

  private enum Kind { INSIDE, OPENED_WITH_IMPORTS, OPENED, OPENED_INTERNAL }

  public enum Extent { EVERYTHING, EXTERNAL_AND_FIELDS, ONLY_EXTERNAL }

  private LexicalScope(Scope parent, Group group, ModulePath module, Kind kind, Extent extent, Scope.Kind scopeKind) {
    myParent = parent;
    myGroup = group;
    myModule = module;
    myKind = kind;
    myExtent = extent;
    myScopeKind = scopeKind;
  }

  private boolean ignoreOpens() {
    return myKind == Kind.OPENED || myKind == Kind.OPENED_INTERNAL;
  }

  public static LexicalScope insideOf(Group group, Scope parent, Extent extent, Scope.Kind kind) {
    ModuleLocation moduleLocation = group.getReferable().getLocation();
    return new LexicalScope(parent, group, moduleLocation == null ? null : moduleLocation.getModulePath(), Kind.INSIDE, extent, kind);
  }

  public static LexicalScope insideOf(Group group, Scope parent, Scope.Kind kind) {
    return insideOf(group, parent, Extent.EVERYTHING, kind);
  }

  public static LexicalScope opened(Group group, Scope.Kind kind) {
    return new LexicalScope(EmptyScope.INSTANCE, group, null, Kind.OPENED, Extent.EVERYTHING, kind);
  }

  private static void addReferable(Referable referable, List<Referable> elements) {
    String name = referable.textRepresentation();
    if (!name.isEmpty() && !"_".equals(name)) {
      elements.add(referable);
    }
    if (referable instanceof GlobalReferable) {
      String alias = ((GlobalReferable) referable).getAliasName();
      if (alias != null && !alias.isEmpty() && !"_".equals(alias)) {
        elements.add(new AliasReferable((GlobalReferable) referable));
      }
    }
  }

  public static void addSubgroup(Group subgroup, List<Referable> elements) {
    addReferable(subgroup.getReferable(), elements);
    for (Group.InternalReferable internalRef : subgroup.getInternalReferables()) {
      if (internalRef.isVisible()) {
        addReferable(internalRef.getReferable(), elements);
      }
    }
  }

  @NotNull
  @Override
  public List<Referable> getElements() {
    List<Referable> elements = new ArrayList<>();

    for (Statement statement : myGroup.getStatements()) {
      statement.addReferables(elements, myScopeKind);
    }

    if (myScopeKind == Scope.Kind.EXPR) {
      if (myExtent == Extent.EVERYTHING) {
        for (Group subgroup : myGroup.getDynamicSubgroups()) {
          addSubgroup(subgroup, elements);
        }
      }

      if (myExtent != Extent.ONLY_EXTERNAL) {
        for (Group.InternalReferable constructor : myGroup.getConstructors()) {
          addReferable(constructor.getReferable(), elements);
        }
        GlobalReferable groupRef = myGroup.getReferable();
        if (myKind != Kind.OPENED_INTERNAL && groupRef instanceof ClassReferable) {
          elements.addAll(new ClassFieldImplScope((ClassReferable) groupRef, ClassFieldImplScope.Extent.WITH_SUPER_DYNAMIC).getElements());
        } else {
          for (Group.InternalReferable field : myGroup.getFields()) {
            addReferable(field.getReferable(), elements);
          }
        }
      }
    }

    if (!ignoreOpens()) {
      Scope cachingScope = null;
      for (Statement statement : myGroup.getStatements()) {
        NamespaceCommand cmd = statement.getNamespaceCommand();
        if (cmd == null || myKind == Kind.OPENED_WITH_IMPORTS && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
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
            cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, Kind.OPENED_WITH_IMPORTS, myExtent, myScopeKind));
          }
          scope = cachingScope;
        }
        elements.addAll(NamespaceCommandNamespace.resolveNamespace(scope, cmd).getElements());
      }
    }

    elements.addAll(myParent.getElements());
    return elements;
  }

  private static GlobalReferable checkReferable(GlobalReferable ref, String name) {
    if (ref.textRepresentation().equals(name)) {
      return ref;
    }
    String alias = ref.getAliasName();
    return alias != null && alias.equals(name) ? new AliasReferable(ref) : null;
  }

  private static GlobalReferable resolveInternal(Group group, String name, boolean onlyInternal) {
    for (Group.InternalReferable internalReferable : group.getConstructors()) {
      if (!onlyInternal || internalReferable.isVisible()) {
        GlobalReferable result = checkReferable(internalReferable.getReferable(), name);
        if (result != null) return result;
      }
    }

    if (onlyInternal || !(group.getReferable() instanceof ClassReferable)) {
      for (Group.InternalReferable internalReferable : group.getFields()) {
        if (!onlyInternal || internalReferable.isVisible()) {
          GlobalReferable result = checkReferable(internalReferable.getReferable(), name);
          if (result != null) return result;
        }
      }
    } else {
      Referable referable = new ClassFieldImplScope((ClassReferable) group.getReferable(), ClassFieldImplScope.Extent.WITH_SUPER_DYNAMIC).resolveName(name);
      return referable instanceof GlobalReferable ? (GlobalReferable) referable : null;
    }

    return null;
  }

  public static Referable resolveRef(Group group, String name) {
    GlobalReferable result = checkReferable(group.getReferable(), name);
    return result != null ? result : resolveInternal(group, name, true);
  }

  private Scope resolveNamespace(Group group, String name, boolean internal) {
    GlobalReferable ref = group.getReferable();
    boolean match = ref.textRepresentation().equals(name);
    if (!match) {
      String alias = ref.getAliasName();
      if (alias != null && alias.equals(name)) {
        match = true;
      }
    }
    return match ? new LexicalScope(EmptyScope.INSTANCE, group, null, internal ? Kind.OPENED_INTERNAL : Kind.OPENED, Extent.EVERYTHING, myScopeKind) : null;
  }

  private enum ResolveType { REF, SCOPE, INTERNAL_SCOPE }

  private Object resolve(String name, ResolveType resolveType) {
    if (name == null || name.isEmpty() || "_".equals(name)) {
      return null;
    }

    for (Statement statement : myGroup.getStatements()) {
      if (resolveType == ResolveType.REF) {
        Referable resolved = statement.resolveRef(name, myScopeKind);
        if (resolved != null) {
          return resolved;
        }
      } else {
        Group subgroup = statement.getGroup();
        if (subgroup != null) {
          Object result = resolveNamespace(subgroup, name, resolveType == ResolveType.INTERNAL_SCOPE);
          if (result != null) {
            return result;
          }
        }
      }
    }

    if (myScopeKind == Scope.Kind.EXPR) {
      if (myExtent == Extent.EVERYTHING) {
        for (Group subgroup : myGroup.getDynamicSubgroups()) {
          Object result = resolveType == ResolveType.REF ? resolveRef(subgroup, name) : resolveNamespace(subgroup, name, resolveType == ResolveType.INTERNAL_SCOPE);
          if (result != null) {
            return result;
          }
        }
      }

      if (resolveType == ResolveType.REF && myExtent != Extent.ONLY_EXTERNAL) {
        Object result = resolveInternal(myGroup, name, myKind == Kind.OPENED_INTERNAL);
        if (result != null) {
          return result;
        }
      }
    }

    if (!ignoreOpens()) {
      Scope cachingScope = null;
      for (Statement statement : myGroup.getStatements()) {
        NamespaceCommand cmd = statement.getNamespaceCommand();
        if (cmd == null || myKind == Kind.OPENED_WITH_IMPORTS && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
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
            cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, Kind.OPENED_WITH_IMPORTS, myExtent, myScopeKind));
          }
          scope = cachingScope;
        }

        scope = NamespaceCommandNamespace.resolveNamespace(scope, cmd);
        Object result = resolveType == ResolveType.REF ? scope.resolveName(name) : scope.resolveNamespace(name, resolveType == ResolveType.INTERNAL_SCOPE);
        if (result != null) {
          return result;
        }
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
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    return ignoreOpens() ? this : new LexicalScope(myParent, myGroup, null, withImports ? Kind.OPENED_WITH_IMPORTS : Kind.OPENED, myExtent, myScopeKind);
  }

  @Override
  public @Nullable ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
