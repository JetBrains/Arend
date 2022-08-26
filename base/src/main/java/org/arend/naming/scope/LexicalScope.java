package org.arend.naming.scope;

import org.arend.ext.module.ModulePath;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.*;
import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("Duplicates")
public class LexicalScope implements Scope {
  private final Scope myParent;
  private final Group myGroup;
  private final ModulePath myModule;
  private final Kind myKind;
  private final Extent myExtent;

  private enum Kind { INSIDE, OPENED_WITH_IMPORTS, OPENED, OPENED_INTERNAL }

  public enum Extent { EVERYTHING, EXTERNAL_AND_FIELDS, ONLY_EXTERNAL }

  private LexicalScope(Scope parent, Group group, ModulePath module, Kind kind, Extent extent) {
    myParent = parent;
    myGroup = group;
    myModule = module;
    myKind = kind;
    myExtent = extent;
  }

  private boolean ignoreOpens() {
    return myKind == Kind.OPENED || myKind == Kind.OPENED_INTERNAL;
  }

  public static LexicalScope insideOf(Group group, Scope parent, Extent extent) {
    ModuleLocation moduleLocation = group.getReferable().getLocation();
    return new LexicalScope(parent, group, moduleLocation == null ? null : moduleLocation.getModulePath(), Kind.INSIDE, extent);
  }

  public static LexicalScope insideOf(Group group, Scope parent) {
    return insideOf(group, parent, Extent.EVERYTHING);
  }

  private static LexicalScope opened(Group group, boolean onlyInternal) {
    return new LexicalScope(EmptyScope.INSTANCE, group, null, onlyInternal ? Kind.OPENED_INTERNAL : Kind.OPENED, Extent.EVERYTHING);
  }

  public static LexicalScope opened(Group group) {
    return opened(group, false);
  }

  private Referable checkReferable(Referable referable, Predicate<Referable> pred) {
    String name = referable.textRepresentation();
    if (!name.isEmpty() && !"_".equals(name)) {
      if (pred.test(referable)) return referable;
    }
    if (referable instanceof GlobalReferable) {
      String alias = ((GlobalReferable) referable).getAliasName();
      if (alias != null && !alias.isEmpty() && !"_".equals(alias)) {
        Referable aliasRef = new AliasReferable((GlobalReferable) referable);
        if (pred.test(aliasRef)) return aliasRef;
      }
    }
    return null;
  }

  private Referable checkSubgroup(Group subgroup, Predicate<Referable> pred) {
    Referable ref = checkReferable(subgroup.getReferable(), pred);
    if (ref != null) return ref;
    for (Group.InternalReferable internalRef : subgroup.getInternalReferables()) {
      if (internalRef.isVisible()) {
        ref = checkReferable(internalRef.getReferable(), pred);
        if (ref != null) return ref;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Referable find(Predicate<Referable> pred) {
    for (Statement statement : myGroup.getStatements()) {
      Group subgroup = statement.getGroup();
      if (subgroup != null) {
        Referable ref = checkSubgroup(subgroup, pred);
        if (ref != null) return ref;
      }
      Abstract.LevelParameters pDef = statement.getPLevelsDefinition();
      if (pDef != null) {
        for (Referable referable : pDef.getReferables()) {
          if (pred.test(referable)) return referable;
        }
      }
      Abstract.LevelParameters hDef = statement.getHLevelsDefinition();
      if (hDef != null) {
        for (Referable referable : hDef.getReferables()) {
          if (pred.test(referable)) return referable;
        }
      }
    }

    if (myExtent == Extent.EVERYTHING) {
      for (Group subgroup : myGroup.getDynamicSubgroups()) {
        checkSubgroup(subgroup, pred);
      }
    }

    if (myExtent != Extent.ONLY_EXTERNAL) {
      for (Group.InternalReferable constructor : myGroup.getConstructors()) {
        checkReferable(constructor.getReferable(), pred);
      }
      GlobalReferable groupRef = myGroup.getReferable();
      if (myKind != Kind.OPENED_INTERNAL && groupRef instanceof ClassReferable) {
        Referable ref = new ClassFieldImplScope((ClassReferable) groupRef, ClassFieldImplScope.Extent.WITH_SUPER_DYNAMIC).find(pred);
        if (ref != null) return ref;
      } else {
        for (Group.InternalReferable field : myGroup.getFields()) {
          checkReferable(field.getReferable(), pred);
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
            cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, Kind.OPENED_WITH_IMPORTS, myExtent));
          }
          scope = cachingScope;
        }
        scope = NamespaceCommandNamespace.resolveNamespace(scope, cmd);
        Referable ref = scope.find(pred);
        if (ref != null) return ref;
      }
    }

    if (myKind == Kind.INSIDE) {
      for (ParameterReferable ref : myGroup.getExternalParameters()) {
        if (pred.test(ref)) return ref;
      }
    }

    return myParent.find(pred);
  }

  private static GlobalReferable resolveInternal(Group group, String name, boolean onlyInternal) {
    for (Group.InternalReferable internalReferable : group.getConstructors()) {
      if (!onlyInternal || internalReferable.isVisible()) {
        GlobalReferable constructor = internalReferable.getReferable();
        if (constructor.textRepresentation().equals(name)) {
          return constructor;
        }
        String alias = constructor.getAliasName();
        if (alias != null && alias.equals(name)) {
          return new AliasReferable(constructor);
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
          String alias = field.getAliasName();
          if (alias != null && alias.equals(name)) {
            return new AliasReferable(field);
          }
        }
      }
    } else {
      Referable referable = new ClassFieldImplScope((ClassReferable) group.getReferable(), ClassFieldImplScope.Extent.WITH_SUPER_DYNAMIC).resolveName(name);
      return referable instanceof GlobalReferable ? (GlobalReferable) referable : null;
    }

    return null;
  }

  private static Object resolveSubgroup(Group group, String name, ResolveType resolveType) {
    GlobalReferable ref = group.getReferable();
    boolean match = ref.textRepresentation().equals(name);
    if (!match) {
      String alias = ref.getAliasName();
      if (alias != null && alias.equals(name)) {
        if (resolveType == ResolveType.REF) {
          return new AliasReferable(ref);
        }
        match = true;
      }
    }
    if (match) {
      return resolveType == ResolveType.REF ? ref : LexicalScope.opened(group, resolveType == ResolveType.INTERNAL_SCOPE);
    }

    if (resolveType == ResolveType.REF) {
      return resolveInternal(group, name, true);
    }

    return null;
  }

  private enum ResolveType { REF, SCOPE, INTERNAL_SCOPE }

  private Object resolve(String name, ResolveType resolveType, Referable.RefKind refKind) {
    if (name.isEmpty() || "_".equals(name)) {
      return null;
    }

    for (Statement statement : myGroup.getStatements()) {
      if (resolveType != ResolveType.REF || refKind == null || refKind == Referable.RefKind.EXPR) {
        Group subgroup = statement.getGroup();
        if (subgroup != null) {
          Object result = resolveSubgroup(subgroup, name, resolveType);
          if (result != null) {
            return result;
          }
        }
      }
      if (refKind == null || refKind == Referable.RefKind.PLEVEL) {
        Abstract.LevelParameters levelParams = statement.getPLevelsDefinition();
        if (levelParams != null) {
          for (Referable ref : levelParams.getReferables()) {
            if (name.equals(ref.getRefName())) {
              return ref;
            }
          }
        }
      }
      if (refKind == null || refKind == Referable.RefKind.HLEVEL) {
        Abstract.LevelParameters levelParams = statement.getHLevelsDefinition();
        if (levelParams != null) {
          for (Referable ref : levelParams.getReferables()) {
            if (name.equals(ref.getRefName())) {
              return ref;
            }
          }
        }
      }
    }

    if (resolveType != ResolveType.REF || refKind == null || refKind == Referable.RefKind.EXPR) {
      if (myExtent == Extent.EVERYTHING) {
        for (Group subgroup : myGroup.getDynamicSubgroups()) {
          Object result = resolveSubgroup(subgroup, name, resolveType);
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
            cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, Kind.OPENED_WITH_IMPORTS, myExtent));
          }
          scope = cachingScope;
        }

        scope = NamespaceCommandNamespace.resolveNamespace(scope, cmd);
        Object result = resolveType == ResolveType.REF ? scope.resolveName(name, refKind) : scope.resolveNamespace(name, resolveType == ResolveType.INTERNAL_SCOPE);
        if (result != null) {
          return result;
        }
      }
    }

    if (myKind == Kind.INSIDE && resolveType == ResolveType.REF) {
      List<? extends Referable> refs = myGroup.getExternalParameters();
      for (int i = refs.size() - 1; i >= 0; i--) {
        Referable ref = refs.get(i);
        if (ref != null && ref.getRefName().equals(name)) {
          return ref;
        }
      }
    }

    return resolveType == ResolveType.REF ? myParent.resolveName(name, refKind) : myParent.resolveNamespace(name, resolveType == ResolveType.INTERNAL_SCOPE);
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, Referable.RefKind kind) {
    Object result = resolve(name, ResolveType.REF, kind);
    return result instanceof Referable ? (Referable) result : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name, boolean onlyInternal) {
    Object result = resolve(name, onlyInternal ? ResolveType.INTERNAL_SCOPE : ResolveType.SCOPE, null);
    return result instanceof Scope ? (Scope) result : null;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    return ignoreOpens() ? this : new LexicalScope(myParent, myGroup, null, withImports ? Kind.OPENED_WITH_IMPORTS : Kind.OPENED, myExtent);
  }

  @Override
  public @Nullable ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
