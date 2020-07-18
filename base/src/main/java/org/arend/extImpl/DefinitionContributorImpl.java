package org.arend.extImpl;

import org.arend.ext.DefinitionContributor;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.module.LongName;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.MetaRef;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.MetaResolver;
import org.arend.library.Library;
import org.arend.library.error.LibraryError;
import org.arend.module.scopeprovider.SimpleModuleScopeProvider;
import org.arend.naming.reference.EmptyGlobalReferable;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.scope.SimpleScope;
import org.arend.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  @Override
  public MetaRef declare(@NotNull ModulePath module, @NotNull LongName longName, @NotNull String description, @NotNull Precedence precedence, @Nullable MetaDefinition meta) {
    return declare(module, longName, description, precedence, null, null, meta);
  }

  @Override
  public MetaRef declare(@NotNull ModulePath module, @NotNull LongName longName, @NotNull String description, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrecedence, @Nullable MetaDefinition meta) {
    return declare(module, longName, description, precedence, alias, aliasPrecedence, meta, meta instanceof MetaResolver ? (MetaResolver) meta : null);
  }

  @Override
  public MetaRef declare(@NotNull ModulePath module, @NotNull LongName longName, @NotNull String description, @NotNull Precedence precedence, @Nullable String alias, @Nullable Precedence aliasPrecedence, @Nullable MetaDefinition meta, @Nullable MetaResolver resolver) {
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

    List<String> list = longName.toList();
    for (int i = 0; i < list.size(); i++) {
      String name = list.get(i);
      if (i == list.size() - 1) {
        Referable ref = scope.resolveName(name);
        if (!(ref == null || ref instanceof EmptyGlobalReferable)) {
          myErrorReporter.report(LibraryError.duplicateExtensionDefinition(myLibrary.getName(), module, longName));
          return null;
        }
        MetaReferable metaRef = new MetaReferable(precedence, name, aliasPrecedence, alias, description, meta, resolver);
        scope.names.put(name, metaRef);
        return metaRef;
      } else {
        scope.names.put(name, new EmptyGlobalReferable(name));
        scope = scope.namespaces.computeIfAbsent(name, k -> new SimpleScope());
      }
    }

    return null;
  }
}
