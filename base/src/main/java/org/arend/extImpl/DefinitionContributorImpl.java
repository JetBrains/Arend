package org.arend.extImpl;

import org.arend.ext.DefinitionContributor;
import org.arend.ext.concrete.definition.ConcreteDefinition;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.MetaRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.MetaResolver;
import org.arend.library.Library;
import org.arend.library.error.LibraryError;
import org.arend.module.ModuleLocation;
import org.arend.module.scopeprovider.SimpleModuleScopeProvider;
import org.arend.naming.reference.*;
import org.arend.naming.scope.SimpleScope;
import org.arend.term.concrete.Concrete;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DefinitionContributorImpl extends Disableable implements DefinitionContributor {
  private final Library myLibrary;
  private final ErrorReporter myErrorReporter;
  private final SimpleModuleScopeProvider myModuleScopeProvider;

  public DefinitionContributorImpl(Library library, ErrorReporter errorReporter, SimpleModuleScopeProvider moduleScopeProvider) {
    myLibrary = library;
    myErrorReporter = errorReporter;
    myModuleScopeProvider = moduleScopeProvider;
  }

  private interface Cont<T> {
    T apply(LocatedReferable locationRef, Referable prevRef, String name);
  }

  private <T extends GlobalReferable> T declare(ModulePath module, LongName longName, String alias, Cont<T> cont) {
    checkEnabled();

    if (!FileUtils.isCorrectModulePath(module)) {
      myErrorReporter.report(FileUtils.illegalModuleName(module.toString()));
      return null;
    }

    if (!FileUtils.isCorrectDefinitionName(longName)) {
      myErrorReporter.report(FileUtils.illegalDefinitionName(longName.toString()));
      return null;
    }

    SimpleScope scope = (SimpleScope) myModuleScopeProvider.forModule(module);
    if (scope == null) {
      scope = new SimpleScope();
      myModuleScopeProvider.addModule(module, scope);
    }

    LocatedReferable locationRef = new FullModuleReferable(new ModuleLocation(myLibrary, ModuleLocation.LocationKind.GENERATED, module));
    Referable prevRef = null;
    List<String> list = longName.toList();
    for (int i = 0; i < list.size(); i++) {
      String name = list.get(i);
      if (i == list.size() - 1) {
        Referable ref = scope.resolveName(name);
        if (ref != null) {
          myErrorReporter.report(LibraryError.duplicateExtensionDefinition(myLibrary.getName(), module, longName));
          return null;
        }
        T curRef = cont.apply(locationRef, prevRef, name);
        scope.names.put(name, curRef);
        if (alias != null) {
          scope.names.putIfAbsent(alias, new AliasReferable(curRef));
          SimpleScope namespace = scope.namespaces.get(name);
          if (namespace != null) {
            scope.namespaces.putIfAbsent(alias, namespace);
          }
          namespace = scope.namespaces.get(alias);
          if (namespace != null) {
            scope.namespaces.putIfAbsent(name, namespace);
          }
        }
        return curRef;
      } else {
        prevRef = scope.names.putIfAbsent(name, new EmptyLocatedReferable(name, prevRef instanceof LocatedReferable ? (LocatedReferable) prevRef : locationRef));
        SimpleScope newScope = scope.namespaces.computeIfAbsent(name, k -> new SimpleScope());
        if (prevRef instanceof AliasReferable) {
          scope.namespaces.putIfAbsent(((AliasReferable) prevRef).getOriginalReferable().getRefName(), newScope);
        } else if (prevRef instanceof GlobalReferable) {
          String aliasName = ((GlobalReferable) prevRef).getAliasName();
          if (aliasName != null) {
            scope.namespaces.putIfAbsent(aliasName, newScope);
          }
        }
        scope = newScope;
      }
    }

    return null;
  }

  @Override
  public MetaRef declare(@NotNull ModulePath module, @NotNull LongName longName, @NotNull String description, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrec, @Nullable MetaDefinition meta, @Nullable MetaResolver resolver) {
    return declare(module, longName, alias, ((locationRef, prevRef, name) -> new MetaReferable(precedence, name, aliasPrec, alias, description, meta, resolver, prevRef instanceof LocatedReferable ? (LocatedReferable) prevRef : locationRef)));
  }

  @Override
  public void declare(@NotNull ConcreteDefinition definition) {
    if (!(definition instanceof Concrete.Definition)) {
      throw new IllegalArgumentException();
    }

    Concrete.Definition def = (Concrete.Definition) definition;
    List<String> longName = new ArrayList<>();
    ModulePath module = LocatedReferable.Helper.getLocation(def.getData(), longName).getModulePath();
    longName.add(def.getData().getRefName());
    declare(module, new LongName(longName), def.getData().getAliasName(), (locationRef, prevRef, name) -> def.getData());
  }
}
