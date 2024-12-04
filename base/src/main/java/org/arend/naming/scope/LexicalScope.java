package org.arend.naming.scope;

import org.arend.ext.module.ModulePath;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.*;
import org.arend.term.NamespaceCommand;
import org.arend.term.abs.Abstract;
import org.arend.term.group.AccessModifier;
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
  private final boolean myDynamicContext;
  private final boolean myWithAdditionalContent; // with external parameters and content of \open

  public LexicalScope(Scope parent, Group group, ModulePath module, boolean isDynamicContext, boolean withAdditionalContent) {
    myParent = parent;
    myGroup = group;
    myModule = module;
    myDynamicContext = isDynamicContext;
    myWithAdditionalContent = withAdditionalContent;
  }

  public static LexicalScope insideOf(Group group, Scope parent, boolean isDynamicContext) {
    ModuleLocation moduleLocation = group.getReferable().getLocation();
    return new LexicalScope(parent, group, moduleLocation == null ? null : moduleLocation.getModulePath(), isDynamicContext, true);
  }

  public static LexicalScope opened(Group group) {
    return new LexicalScope(EmptyScope.INSTANCE, group, null, true, false);
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
  public Referable find(Predicate<Referable> pred, @Nullable ScopeContext context) {
    for (Statement statement : myGroup.getStatements()) {
      Group subgroup = statement.getGroup();
      if (subgroup != null) {
        if (context == null || context == ScopeContext.STATIC) {
          Referable ref = checkSubgroup(subgroup, pred);
          if (ref != null) return ref;
        }
        if (context == null || context == ScopeContext.DYNAMIC) {
          for (Group dynamicGroup : subgroup.getDynamicSubgroups()) {
            if (dynamicGroup.getReferable().getAccessModifier() == AccessModifier.PUBLIC) {
              Referable ref = checkSubgroup(dynamicGroup, pred);
              if (ref != null) return ref;
            }
          }
          for (Group.InternalReferable field : subgroup.getFields()) {
            if (field.isVisible() && field.getReferable().getAccessModifier() == AccessModifier.PUBLIC) {
              checkReferable(field.getReferable(), pred);
            }
          }
        }
      }
      Abstract.LevelParameters pDef = statement.getPLevelsDefinition();
      if (pDef != null && (context == null || context == ScopeContext.PLEVEL)) {
        for (Referable referable : pDef.getReferables()) {
          if (pred.test(referable)) return referable;
        }
      }
      Abstract.LevelParameters hDef = statement.getHLevelsDefinition();
      if (hDef != null && (context == null || context == ScopeContext.HLEVEL)) {
        for (Referable referable : hDef.getReferables()) {
          if (pred.test(referable)) return referable;
        }
      }
    }

    if (myDynamicContext && (context == null || context == ScopeContext.STATIC)) {
      for (Group subgroup : myGroup.getDynamicSubgroups()) {
        Referable ref = checkSubgroup(subgroup, pred);
        if (ref != null) return ref;
      }
    }

    if (context == null || context == ScopeContext.STATIC) {
      for (Group.InternalReferable constructor : myGroup.getConstructors()) {
        checkReferable(constructor.getReferable(), pred);
      }
      for (Group.InternalReferable field : myGroup.getFields()) {
        checkReferable(field.getReferable(), pred);
      }
    }

    Scope cachingScope = null;
    for (Statement statement : myGroup.getStatements()) {
      NamespaceCommand cmd = statement.getNamespaceCommand();
      if (cmd == null || !(myWithAdditionalContent || cmd.getKind() == NamespaceCommand.Kind.IMPORT)) {
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
          cachingScope = myDynamicContext && !myWithAdditionalContent ? this : CachingScope.make(new LexicalScope(myParent, myGroup, null, true, false));
        }
        scope = cachingScope;
      }
      scope = NamespaceCommandNamespace.resolveNamespace(scope, cmd);
      Referable ref = scope.find(pred, context);
      if (ref != null) return ref;
    }

    if (myWithAdditionalContent && (context == null || context == ScopeContext.STATIC)) {
      for (ParameterReferable ref : myGroup.getExternalParameters()) {
        if (pred.test(ref)) return ref;
      }
    }

    return myParent.find(pred, context);
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
      return resolveType == ResolveType.REF ? ref : LexicalScope.opened(group);
    }

    if (resolveType == ResolveType.REF) {
      return resolveInternal(group, name, true);
    }

    return null;
  }

  private enum ResolveType { REF, SCOPE }

  private Object resolve(String name, ResolveType resolveType, ScopeContext context) {
    if (name.isEmpty() || "_".equals(name)) {
      return null;
    }

    for (Statement statement : myGroup.getStatements()) {
      Group subgroup = statement.getGroup();
      if (subgroup != null) {
        if (resolveType != ResolveType.REF || context == null || context == ScopeContext.STATIC) {
          Object result = resolveSubgroup(subgroup, name, resolveType);
          if (result != null) {
            return result;
          }
        }
        if (resolveType == ResolveType.REF && (context == null || context == ScopeContext.DYNAMIC)) {
          for (Group dynamicGgroup : subgroup.getDynamicSubgroups()) {
            if (dynamicGgroup.getReferable().getAccessModifier() == AccessModifier.PUBLIC) {
              Object result = resolveSubgroup(dynamicGgroup, name, resolveType);
              if (result != null) {
                return result;
              }
            }
          }
          GlobalReferable result = resolveInternal(subgroup, name, true);
          if (result != null && result.getAccessModifier() == AccessModifier.PUBLIC) {
            return result;
          }
        }
      }
      if (context == null || context == ScopeContext.PLEVEL) {
        Abstract.LevelParameters levelParams = statement.getPLevelsDefinition();
        if (levelParams != null) {
          for (Referable ref : levelParams.getReferables()) {
            if (name.equals(ref.getRefName())) {
              return ref;
            }
          }
        }
      }
      if (context == null || context == ScopeContext.HLEVEL) {
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

    if (myDynamicContext && (context == null || context == ScopeContext.STATIC)) {
      for (Group subgroup : myGroup.getDynamicSubgroups()) {
        Object result = resolveSubgroup(subgroup, name, resolveType);
        if (result != null) {
          return result;
        }
      }
    }

    if (resolveType == ResolveType.REF && (context == null || context == ScopeContext.STATIC)) {
      GlobalReferable result = resolveInternal(myGroup, name, false);
      if (result != null) {
        return result;
      }
    }

    Scope cachingScope = null;
    for (Statement statement : myGroup.getStatements()) {
      NamespaceCommand cmd = statement.getNamespaceCommand();
      if (cmd == null || !(myWithAdditionalContent || cmd.getKind() == NamespaceCommand.Kind.IMPORT)) {
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
          cachingScope = myDynamicContext && !myWithAdditionalContent ? this : CachingScope.make(new LexicalScope(myParent, myGroup, null, true, false));
        }
        scope = cachingScope;
      }

      scope = NamespaceCommandNamespace.resolveNamespace(scope, cmd);
      Object result = resolveType == ResolveType.REF ? scope.resolveName(name, context) : scope.resolveNamespace(name);
      if (result != null) {
        return result;
      }
    }

    if (myWithAdditionalContent && resolveType == ResolveType.REF) {
      List<? extends Referable> refs = myGroup.getExternalParameters();
      for (int i = refs.size() - 1; i >= 0; i--) {
        Referable ref = refs.get(i);
        if (ref != null && ref.getRefName().equals(name)) {
          return ref;
        }
      }
    }

    return resolveType == ResolveType.REF ? myParent.resolveName(name, context) : myParent.resolveNamespace(name);
  }

  @Nullable
  @Override
  public Referable resolveName(@NotNull String name, @Nullable ScopeContext context) {
    Object result = resolve(name, ResolveType.REF, context);
    return result instanceof Referable ? (Referable) result : null;
  }

  @Nullable
  @Override
  public Scope resolveNamespace(@NotNull String name) {
    Object result = resolve(name, ResolveType.SCOPE, null);
    return result instanceof Scope ? (Scope) result : null;
  }

  @NotNull
  @Override
  public Scope getGlobalSubscopeWithoutOpens(boolean withImports) {
    return myWithAdditionalContent ? new LexicalScope(myParent, myGroup, null, myDynamicContext, false) : this;
  }

  @Override
  public @Nullable ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
