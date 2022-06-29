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

  private enum Kind { INSIDE, OPENED, OPENED_INTERNAL }

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

  private void addReferable(Referable referable, List<Referable> elements) {
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

  private void addSubgroup(Group subgroup, List<Referable> elements) {
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
      Group subgroup = statement.getGroup();
      if (subgroup != null) {
        addSubgroup(subgroup, elements);
      }
    }
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

    Scope cachingScope = null;
    for (Statement statement : myGroup.getStatements()) {
      NamespaceCommand cmd = statement.getNamespaceCommand();
      if (cmd == null || ignoreOpens() && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
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
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, Kind.OPENED, myExtent));
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

  private Object resolve(String name, ResolveType resolveType) {
    if (name == null || name.isEmpty() || "_".equals(name)) {
      return null;
    }

    for (Statement statement : myGroup.getStatements()) {
      Group subgroup = statement.getGroup();
      if (subgroup != null) {
        Object result = resolveSubgroup(subgroup, name, resolveType);
        if (result != null) {
          return result;
        }
      }
    }
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

    Scope cachingScope = null;
    for (Statement statement : myGroup.getStatements()) {
      NamespaceCommand cmd = statement.getNamespaceCommand();
      if (cmd == null || ignoreOpens() && cmd.getKind() == NamespaceCommand.Kind.OPEN) {
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
          cachingScope = CachingScope.make(new LexicalScope(myParent, myGroup, null, Kind.OPENED, myExtent));
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
    return ignoreOpens() ? this : new LexicalScope(myParent, myGroup, null, Kind.OPENED, myExtent);
  }

  @Nullable
  @Override
  public ImportedScope getImportedSubscope() {
    return myParent.getImportedSubscope();
  }
}
