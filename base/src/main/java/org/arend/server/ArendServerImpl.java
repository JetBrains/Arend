package org.arend.server;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.GeneralError;
import org.arend.ext.error.ListErrorReporter;
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
import org.arend.typechecking.LibraryArendExtensionProvider;
import org.arend.typechecking.computation.BooleanComputationRunner;
import org.arend.typechecking.computation.CancellationIndicator;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.ConcreteIndexComparator;
import org.arend.typechecking.order.Ordering;
import org.arend.typechecking.order.dependency.DependencyCollector;
import org.arend.typechecking.order.listener.CollectingOrderingListener;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
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

    Set<TCDefReferable> cleared = new HashSet<>();
    if (oldGroup != null) {
      oldGroup.proj2.traverseGroup(subgroup -> {
        if (subgroup.getReferable() instanceof TCDefReferable ref) {
          cleared.add(ref);
        }
      });
    }
    InstanceProviderSet instanceProviderSet = new InstanceProviderSet();
    instanceProviderSet.collectInstances(group, CachingScope.make(ScopeFactory.parentScopeForGroup(group, moduleScopeProvider, true)), IdReferableConverter.INSTANCE);
    cleared.removeIf(ref -> instanceProviderSet.get(ref) != null);

    List<TCDefReferable> updatedRefs = new ArrayList<>();
    for (Map.Entry<ConcreteLocatedReferable, Concrete.Definition> entry : concreteUpdated.entrySet()) {
      if (entry.getKey().getTypechecked() != null && !(InstanceProvider.compare(instanceProviderSet.get(entry.getKey()), myLibraryManager.getInstanceProviderSet().get(entry.getKey())) && ConcreteCompareVisitor.compare(entry.getKey().getDefinition(), entry.getValue()))) {
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
      myLibraryManager.getInstanceProviderSet().getProviders().keySet().removeAll(cleared);
      for (Map.Entry<TCDefReferable, ? extends InstanceProvider> entry : instanceProviderSet.getProviders().entrySet()) {
        myLibraryManager.getInstanceProviderSet().put(entry.getKey(), entry.getValue());
      }

      int[] index = new int[1];
      group.traverseGroup(subgroup -> {
        if (subgroup.getReferable() instanceof ConcreteLocatedReferable ref) {
          ref.index = index[0];
        }
        index[0]++;
      });

      return new Pair<>(modificationStamp, group);
    });
  }

  @Override
  public void deleteModule(@NotNull ModuleLocation module) {
    myGroups.remove(module);
  }

  @Override
  public void scheduleTask(@NotNull Collection<? extends TCDefReferable> definitions, @NotNull ArendTaskListener listener, @NotNull CancellationIndicator cancellationIndicator) {
    CollectingOrderingListener collector = new CollectingOrderingListener();
    Ordering ordering = new Ordering(myLibraryManager.getInstanceProviderSet(), ConcreteReferableProvider.INSTANCE, collector, myDependencyCollector, IdReferableConverter.INSTANCE, ConcreteIndexComparator.INSTANCE);
    for (TCDefReferable ref : definitions) {
      if (ref instanceof ConcreteLocatedReferable && ((ConcreteLocatedReferable) ref).getDefinition() instanceof Concrete.ResolvableDefinition def) {
        ordering.order(def);
      }
    }

    boolean ok = new BooleanComputationRunner().run(cancellationIndicator, () -> {
      cancellationIndicator.checkCanceled();
      listener.taskScheduled(collector.getAllReferables());
      List<GeneralError> errors = new ArrayList<>();
      collector.feed(new TypecheckingOrderingListener(myLibraryManager.getInstanceProviderSet(), ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, new ListErrorReporter(errors), ConcreteIndexComparator.INSTANCE, new LibraryArendExtensionProvider(myLibraryManager)) {
        @Override
        public void unitFound(Concrete.ResolvableDefinition definition, boolean recursive) {
          super.unitFound(definition, recursive);
          cancellationIndicator.checkCanceled();
          listener.definitionChecked(definition.getData(), new ArrayList<>(errors));
          errors.clear();
        }

        @Override
        public void bodiesFound(List<Concrete.ResolvableDefinition> definitions) {
          super.bodiesFound(definitions);
          cancellationIndicator.checkCanceled();
          List<GeneralError> errors1 = new ArrayList<>(errors);
          errors.clear();
          for (Concrete.ResolvableDefinition definition : definitions) {
            listener.definitionChecked(definition.getData(), errors1);
          }
        }
      });
      return true;
    });

    if (ok) {
      listener.taskFinished();
    } else {
      listener.taskCancelled();
    }
  }
}
