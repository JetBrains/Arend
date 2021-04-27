package org.arend.typechecking.order.listener;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.PiExpression;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.error.CountingErrorReporter;
import org.arend.ext.ArendExtension;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.library.Library;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.typechecking.*;
import org.arend.typechecking.computation.BooleanComputationRunner;
import org.arend.typechecking.computation.CancellationIndicator;
import org.arend.typechecking.error.CycleError;
import org.arend.typechecking.error.TerminationCheckError;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.Ordering;
import org.arend.typechecking.order.PartialComparator;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.dependency.DummyDependencyListener;
import org.arend.typechecking.patternmatching.ExtElimClause;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.termination.DefinitionCallGraph;
import org.arend.typechecking.termination.RecursiveBehavior;
import org.arend.typechecking.visitor.*;
import org.arend.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TypecheckingOrderingListener extends BooleanComputationRunner implements OrderingListener {
  private final DependencyListener myDependencyListener;
  private final Map<TCDefReferable, Pair<CheckTypeVisitor,Boolean>> mySuspensions = new HashMap<>();
  private final ErrorReporter myErrorReporter;
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private final ReferableConverter myReferableConverter;
  private final PartialComparator<TCDefReferable> myComparator;
  private final ArendExtensionProvider myExtensionProvider;
  private List<TCDefReferable> myCurrentDefinitions = Collections.emptyList();
  private boolean myHeadersAreOK = true;

  public TypecheckingOrderingListener(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, ReferableConverter referableConverter, ErrorReporter errorReporter, DependencyListener dependencyListener, PartialComparator<TCDefReferable> comparator, ArendExtensionProvider extensionProvider) {
    myErrorReporter = errorReporter;
    myDependencyListener = dependencyListener;
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myReferableConverter = referableConverter;
    myComparator = comparator;
    myExtensionProvider = extensionProvider;
  }

  public TypecheckingOrderingListener(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, ReferableConverter referableConverter, ErrorReporter errorReporter, PartialComparator<TCDefReferable> comparator, ArendExtensionProvider extensionProvider) {
    this(instanceProviderSet, concreteProvider, referableConverter, errorReporter, DummyDependencyListener.INSTANCE, comparator, extensionProvider);
  }

  public ConcreteProvider getConcreteProvider() {
    return myConcreteProvider;
  }

  public InstanceProviderSet getInstanceProviderSet() {
    return myInstanceProviderSet;
  }

  public ReferableConverter getReferableConverter() {
    return myReferableConverter;
  }

  @Override
  protected Boolean computationInterrupted() {
    for (TCDefReferable currentDefinition : myCurrentDefinitions) {
      Definition typechecked = currentDefinition.getTypechecked();
      currentDefinition.setTypechecked(null);
      typecheckingInterrupted(currentDefinition, typechecked);
    }
    myCurrentDefinitions = Collections.emptyList();
    return false;
  }

  public boolean typecheckDefinitions(final Collection<? extends Concrete.Definition> definitions, CancellationIndicator cancellationIndicator) {
    return run(cancellationIndicator, () -> {
      Ordering ordering = new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myComparator);
      for (Concrete.Definition definition : definitions) {
        ordering.order(definition);
      }
      return true;
    });
  }

  public boolean typecheckModules(final Collection<? extends Group> modules, CancellationIndicator cancellationIndicator) {
    return run(cancellationIndicator, () -> {
      new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myComparator).orderModules(modules);
      return true;
    });
  }

  public boolean typecheckLibrary(Library library, CancellationIndicator cancellationIndicator) {
    return run(cancellationIndicator, () -> library.orderModules(new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myComparator)));
  }

  public boolean typecheckLibrary(Library library) {
    return typecheckLibrary(library, null);
  }

  public boolean typecheckTests(Library library, CancellationIndicator cancellationIndicator) {
    return run(cancellationIndicator, () -> library.orderTestModules(new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myComparator)));
  }

  public boolean typecheckCollected(CollectingOrderingListener collector, CancellationIndicator cancellationIndicator) {
    if (collector.isEmpty()) return true;
    return run(cancellationIndicator, () -> {
      collector.feed(this);
      return true;
    });
  }

  public void typecheckingHeaderStarted(TCDefReferable definition) {

  }

  public void typecheckingBodyStarted(TCDefReferable definition) {

  }

  public void typecheckingUnitStarted(TCDefReferable definition) {

  }

  public void typecheckingHeaderFinished(TCDefReferable referable, Definition definition) {

  }

  public void typecheckingBodyFinished(TCDefReferable referable, Definition definition) {

  }

  public void typecheckingUnitFinished(TCDefReferable referable, Definition definition) {

  }

  public void typecheckingInterrupted(TCDefReferable definition, @Nullable Definition typechecked) {

  }

  private Definition newDefinition(Concrete.Definition definition) {
    Definition typechecked;
    if (definition instanceof Concrete.DataDefinition) {
      typechecked = new DataDefinition(definition.getData());
      for (Concrete.ConstructorClause constructorClause : ((Concrete.DataDefinition) definition).getConstructorClauses()) {
        for (Concrete.Constructor constructor : constructorClause.getConstructors()) {
          Constructor tcConstructor = new Constructor(constructor.getData(), (DataDefinition) typechecked);
          tcConstructor.setParameters(EmptyDependentLink.getInstance());
          tcConstructor.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          ((DataDefinition) typechecked).addConstructor(tcConstructor);
          constructor.getData().setTypecheckedIfAbsent(tcConstructor);
        }
      }
    } else if (definition instanceof Concrete.BaseFunctionDefinition) {
      typechecked = ((Concrete.BaseFunctionDefinition) definition).getKind() == FunctionKind.CONS ? new DConstructor(definition.getData()) : new FunctionDefinition(definition.getData());
      ((FunctionDefinition) typechecked).setResultType(new ErrorExpression());
    } else if (definition instanceof Concrete.ClassDefinition) {
      typechecked = new ClassDefinition(definition.getData());
      for (Concrete.ClassElement element : ((Concrete.ClassDefinition) definition).getElements()) {
        if (element instanceof Concrete.ClassField) {
          ClassField classField = new ClassField(((Concrete.ClassField) element).getData(), (ClassDefinition) typechecked, new PiExpression(Sort.PROP, new TypedSingleDependentLink(false, "this", new ClassCallExpression((ClassDefinition) typechecked, LevelPair.STD), true), new ErrorExpression()), null);
          classField.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          ((ClassDefinition) typechecked).addPersonalField(classField);
          classField.getReferable().setTypecheckedIfAbsent(classField);
        }
      }
    } else {
      throw new IllegalStateException();
    }
    typechecked.setStatus(Definition.TypeCheckingStatus.NEEDS_TYPE_CHECKING);
    definition.getData().setTypecheckedIfAbsent(typechecked);
    return typechecked;
  }

  @Override
  public void unitFound(Concrete.ResolvableDefinition resolvableDefinition, boolean recursive) {
    myHeadersAreOK = true;

    if (!(resolvableDefinition instanceof Concrete.Definition)) {
      return;
    }

    Concrete.Definition definition = (Concrete.Definition) resolvableDefinition;
    if (recursive) {
      Set<TCReferable> dependencies = new HashSet<>();
      definition.accept(new CollectDefCallsVisitor(dependencies, false), null);
      if (dependencies.contains(definition.getData())) {
        typecheckingUnitStarted(definition.getData());
        myErrorReporter.report(new CycleError(Collections.singletonList(definition.getData())));
        typecheckingUnitFinished(definition.getData(), newDefinition(definition));
        return;
      }
      definition.setRecursiveDefinitions(Collections.singleton(definition.getData()));
    }

    List<ExtElimClause> clauses;
    ArendExtension extension = myExtensionProvider.getArendExtension(definition.getData());
    CheckTypeVisitor checkTypeVisitor = new CheckTypeVisitor(new LocalErrorReporter(definition.getData(), myErrorReporter), null, extension);
    checkTypeVisitor.setInstancePool(new GlobalInstancePool(myInstanceProviderSet.get(definition.getData()), checkTypeVisitor));
    DesugarVisitor.desugar(definition, checkTypeVisitor.getErrorReporter());
    myCurrentDefinitions = Collections.singletonList(definition.getData());
    typecheckingUnitStarted(definition.getData());
    clauses = definition.accept(new DefinitionTypechecker(checkTypeVisitor), null);
    Definition typechecked = definition.getData().getTypechecked();
    if (typechecked == null) {
      typechecked = newDefinition(definition);
    }

    if (recursive && typechecked instanceof FunctionDefinition) {
      ((FunctionDefinition) typechecked).setRecursiveDefinitions(Collections.singleton(typechecked));
    }
    if (recursive && typechecked instanceof DataDefinition) {
      ((DataDefinition) typechecked).setRecursiveDefinitions(Collections.singleton(typechecked));
    }
    if (definition.isRecursive() && typechecked instanceof FunctionDefinition) {
      checkRecursiveFunctions(Collections.singletonMap((FunctionDefinition) typechecked, definition), clauses == null ? Collections.emptyMap() : Collections.singletonMap((FunctionDefinition) typechecked, clauses));
    }

    typecheckingUnitFinished(definition.getData(), typechecked);

    if (extension != null) {
      DefinitionListener listener = extension.getDefinitionListener();
      if (listener != null) {
        listener.typechecked(typechecked);
      }
    }

    myCurrentDefinitions = Collections.emptyList();
  }

  @Override
  public void cycleFound(List<Concrete.ResolvableDefinition> definitions) {
    List<TCReferable> cycle = new ArrayList<>();
    for (Concrete.ResolvableDefinition definition : definitions) {
      if (cycle.isEmpty() || cycle.get(cycle.size() - 1) != definition.getData()) {
        cycle.add(definition.getData());
      }

      if (definition instanceof Concrete.Definition) {
        Concrete.Definition def = (Concrete.Definition) definition;
        Definition typechecked = def.getData().getTypechecked();
        if (typechecked == null) {
          typechecked = newDefinition(def);
        }
        typechecked.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        typecheckingUnitStarted(def.getData());
        mySuspensions.remove(def.getData());
        typecheckingUnitFinished(def.getData(), typechecked);
      }
    }
    myErrorReporter.report(new CycleError(cycle));
  }

  @Override
  public void headerFound(Concrete.Definition definition) {
    myCurrentDefinitions = Collections.singletonList(definition.getData());
    typecheckingHeaderStarted(definition.getData());

    CountingErrorReporter countingErrorReporter = new CountingErrorReporter(myErrorReporter);
    CheckTypeVisitor visitor = new CheckTypeVisitor(new LocalErrorReporter(definition.getData(), countingErrorReporter), null, myExtensionProvider.getArendExtension(definition.getData()));
    visitor.setStatus(definition.getStatus().getTypecheckingStatus());
    DesugarVisitor.desugar(definition, visitor.getErrorReporter());
    Definition oldTypechecked = definition.getData().getTypechecked();
    boolean isNew = oldTypechecked == null || oldTypechecked.status().needsTypeChecking();
    Definition typechecked = new DefinitionTypechecker(visitor).typecheckHeader(oldTypechecked, new GlobalInstancePool(myInstanceProviderSet.get(definition.getData()), visitor), definition);
    typechecked.setUniverseKind(UniverseKind.WITH_UNIVERSES);
    if (typechecked.status() == Definition.TypeCheckingStatus.TYPE_CHECKING) {
      mySuspensions.put(definition.getData(), new Pair<>(visitor, isNew));
    }

    typecheckingHeaderFinished(definition.getData(), typechecked);
    myCurrentDefinitions = Collections.emptyList();
    if (!typechecked.status().headerIsOK()) {
      myHeadersAreOK = false;
    }
  }

  @Override
  public void bodiesFound(List<Concrete.Definition> definitions) {
    Map<FunctionDefinition,Concrete.Definition> functionDefinitions = new HashMap<>();
    Map<FunctionDefinition, List<? extends ElimClause<ExpressionPattern>>> clausesMap = new HashMap<>();
    Set<DataDefinition> dataDefinitions = new HashSet<>();
    List<Concrete.Definition> orderedDefinitions = new ArrayList<>(definitions.size());
    List<Concrete.Definition> otherDefs = new ArrayList<>();
    for (Concrete.Definition definition : definitions) {
      Definition typechecked = definition.getData().getTypechecked();
      if (typechecked instanceof DataDefinition) {
        dataDefinitions.add((DataDefinition) typechecked);
        orderedDefinitions.add(definition);
      } else {
        otherDefs.add(definition);
      }
    }
    orderedDefinitions.addAll(otherDefs);

    DefinitionTypechecker typechecking = new DefinitionTypechecker(null);
    myCurrentDefinitions = new ArrayList<>();
    for (Concrete.Definition definition : orderedDefinitions) {
      myCurrentDefinitions.add(definition.getData());
    }

    List<Pair<Definition, DefinitionListener>> listeners = new ArrayList<>();
    for (Concrete.Definition definition : orderedDefinitions) {
      typecheckingBodyStarted(definition.getData());

      Definition def = definition.getData().getTypechecked();
      Pair<CheckTypeVisitor, Boolean> pair = mySuspensions.remove(definition.getData());
      if (myHeadersAreOK && pair != null) {
        typechecking.setTypechecker(pair.proj1);
        typechecking.updateState(!pair.proj2);
        List<? extends ElimClause<ExpressionPattern>> clauses = typechecking.typecheckBody(def, definition, dataDefinitions);
        if (def instanceof FunctionDefinition) {
          functionDefinitions.put((FunctionDefinition) def, definition);
          if (clauses != null) {
            clausesMap.put((FunctionDefinition) def, clauses);
          }
        }

        ArendExtension extension = pair.proj1.getExtension();
        if (extension != null) {
          DefinitionListener listener = extension.getDefinitionListener();
          if (listener != null) {
            listeners.add(new Pair<>(def, listener));
          }
        }
      }
    }
    myCurrentDefinitions = Collections.emptyList();

    myHeadersAreOK = true;

    if (!functionDefinitions.isEmpty()) {
      FindDefCallVisitor<DataDefinition> visitor = new FindDefCallVisitor<>(dataDefinitions, false);
      Iterator<Map.Entry<FunctionDefinition, Concrete.Definition>> it = functionDefinitions.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<FunctionDefinition, Concrete.Definition> entry = it.next();
        visitor.visitBody(entry.getKey().getBody(), null);
        Definition found = visitor.getFoundDefinition();
        if (found != null) {
          entry.getKey().setBody(null);
          entry.getKey().addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          myErrorReporter.report(new TypecheckingError("Mutually recursive function refers to data type '" + found.getName() + "'", entry.getValue()).withDefinition(entry.getKey().getReferable()));
          it.remove();
          visitor.clear();
        }
      }

      if (!functionDefinitions.isEmpty()) {
        checkRecursiveFunctions(functionDefinitions, clausesMap);
      }
    }

    Set<Definition> allDefinitions = new HashSet<>();
    for (Concrete.Definition definition : orderedDefinitions) {
      Definition typechecked = definition.getData().getTypechecked();
      if (typechecked instanceof FunctionDefinition) {
        ((FunctionDefinition) typechecked).setRecursiveDefinitions(allDefinitions);
        allDefinitions.add(typechecked);
      } else if (typechecked instanceof DataDefinition) {
        ((DataDefinition) typechecked).setRecursiveDefinitions(allDefinitions);
        allDefinitions.add(typechecked);
      }
      typecheckingBodyFinished(definition.getData(), typechecked);
    }

    for (Pair<Definition, DefinitionListener> pair : listeners) {
      pair.proj2.typechecked(pair.proj1);
    }
  }

  @Override
  public void useFound(List<Concrete.UseDefinition> definitions) {
    myCurrentDefinitions = new ArrayList<>();
    for (Concrete.UseDefinition definition : definitions) {
      myCurrentDefinitions.add(definition.getData());
      myCurrentDefinitions.add(definition.getUseParent());
    }
    UseTypechecking.typecheck(definitions, myErrorReporter);
    myCurrentDefinitions = Collections.emptyList();
  }

  private void checkRecursiveFunctions(Map<FunctionDefinition,Concrete.Definition> definitions, Map<FunctionDefinition, ? extends List<? extends ElimClause<ExpressionPattern>>> clauses) {
    boolean ok = true;
    DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
    for (Map.Entry<FunctionDefinition, Concrete.Definition> entry : definitions.entrySet()) {
      List<? extends ElimClause<ExpressionPattern>> functionClauses = clauses.get(entry.getKey());
      definitionCallGraph.add(entry.getKey(), functionClauses == null ? Collections.emptyList() : functionClauses, definitions.keySet());
      for (DependentLink link = entry.getKey().getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (FindDefCallVisitor.findDefinition(link.getTypeExpr(), definitions.keySet()) != null) {
          myErrorReporter.report(new TypecheckingError("Mutually recursive functions are not allowed in parameters", entry.getValue()).withDefinition(entry.getKey().getReferable()));
          ok = false;
        }
      }
      if (entry.getValue() instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) entry.getValue()).getBody() instanceof Concrete.CoelimFunctionBody) {
        myErrorReporter.report(new TypecheckingError("Recursive functions cannot be defined by copattern matching", entry.getValue()).withDefinition(entry.getKey().getReferable()));
        ok = false;
      }
    }

    if (!definitionCallGraph.checkTermination()) {
      for (Map.Entry<Definition, Set<RecursiveBehavior<Definition>>> entry : definitionCallGraph.myErrorInfo.entrySet()) {
        myErrorReporter.report(new TerminationCheckError(entry.getKey(), entry.getValue()));
      }
      ok = false;
    }

    if (!ok) {
      for (FunctionDefinition definition : definitions.keySet()) {
        definition.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        definition.setBody(null);
      }
    }
  }
}
