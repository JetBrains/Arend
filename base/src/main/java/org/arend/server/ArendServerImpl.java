package org.arend.server;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.util.Pair;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.library.error.LibraryError;
import org.arend.module.ModuleLocation;
import org.arend.module.scopeprovider.ModuleScopeProvider;
import org.arend.naming.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.resolving.ResolverListener;
import org.arend.naming.resolving.visitor.DefinitionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteCompareVisitor;
import org.arend.typechecking.order.dependency.DependencyCollector;
import org.arend.typechecking.provider.ConcreteReferableProvider;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArendServerImpl implements ArendServer {
  private final LibraryManager myLibraryManager;
  private final Map<ModuleLocation, Pair<Long, ConcreteGroup>> myGroups = new ConcurrentHashMap<>();
  private final DependencyCollector myDependencyCollector = new DependencyCollector();

  public ArendServerImpl(LibraryManager libraryManager) {
    myLibraryManager = libraryManager;
  }

  @Override
  public void updateModule(long modificationStamp, @NotNull ModuleLocation module, @NotNull ConcreteGroup group, @NotNull ResolverListener resolverListener, @NotNull ErrorReporter errorReporter) {
    var oldGroup = myGroups.get(module);
    if (oldGroup != null && oldGroup.proj1 > modificationStamp) {
      return;
    }

    Library library = myLibraryManager.getRegisteredLibrary(module.getLibraryName());
    if (library == null) {
      errorReporter.report(LibraryError.notFound(module.getLibraryName()));
      return;
    }

    Map<ConcreteLocatedReferable, Concrete.Definition> concreteUpdated = new HashMap<>();
    if (oldGroup != null) group.copyReferablesFrom(oldGroup.proj2, concreteUpdated);

    ModuleScopeProvider globalModuleScopeProvider = myLibraryManager.getAvailableModuleScopeProvider(library);
    ModuleScopeProvider moduleScopeProvider = module.getLocationKind() != ModuleLocation.LocationKind.TEST ? globalModuleScopeProvider : modulePath -> {
      Scope scope = globalModuleScopeProvider.forModule(modulePath);
      return scope != null ? scope : library.getTestsModuleScopeProvider().forModule(modulePath);
    };
    new DefinitionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, errorReporter, resolverListener).resolveGroup(group, CachingScope.make(ScopeFactory.forGroup(group, moduleScopeProvider)));

    List<TCDefReferable> updatedRefs = new ArrayList<>();
    for (Map.Entry<ConcreteLocatedReferable, Concrete.Definition> entry : concreteUpdated.entrySet()) {
      if (entry.getKey().getTypechecked() != null && !ConcreteCompareVisitor.compare(entry.getKey().getDefinition(), entry.getValue())) {
        updatedRefs.add(entry.getKey());
      }
    }

    myGroups.compute(module, (k, oldValue) -> {
      if (oldValue != null && oldValue.proj1 > modificationStamp) {
        return oldValue;
      }

      for (TCDefReferable ref : updatedRefs) {
        myDependencyCollector.update(ref);
      }
      for (Map.Entry<ConcreteLocatedReferable, Concrete.Definition> entry : concreteUpdated.entrySet()) {
        entry.getKey().setDefinition(entry.getValue());
      }

      return new Pair<>(modificationStamp, group);
    });
  }

  @Override
  public void deleteModule(@NotNull ModuleLocation module) {

  }

  @Override
  public void scheduleTask(@NotNull Collection<? extends TCDefReferable> definitions, @NotNull ArendTaskListener listener) {

  }
}
