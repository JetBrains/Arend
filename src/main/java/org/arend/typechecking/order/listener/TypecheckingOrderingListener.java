package org.arend.typechecking.order.listener;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.ExtClause;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.ErrorExpression;
import org.arend.core.expr.PiExpression;
import org.arend.core.sort.Sort;
import org.arend.error.CompositeErrorReporter;
import org.arend.error.CountingErrorReporter;
import org.arend.error.ErrorReporter;
import org.arend.library.Library;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.typechecking.CancellationIndicator;
import org.arend.typechecking.ThreadCancellationIndicator;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.UseTypechecking;
import org.arend.typechecking.error.CycleError;
import org.arend.typechecking.error.TerminationCheckError;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.arend.typechecking.error.local.TypecheckingError;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.Ordering;
import org.arend.typechecking.order.PartialComparator;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.dependency.DummyDependencyListener;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.termination.DefinitionCallGraph;
import org.arend.typechecking.termination.RecursiveBehavior;
import org.arend.typechecking.visitor.*;
import org.arend.util.ComputationInterruptedException;
import org.arend.util.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BooleanSupplier;

public class TypecheckingOrderingListener implements OrderingListener {
  private final TypecheckerState myState;
  private final DependencyListener myDependencyListener;
  private final Map<GlobalReferable, Pair<CheckTypeVisitor,Boolean>> mySuspensions = new HashMap<>();
  private final ErrorReporter myErrorReporter;
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private final ReferableConverter myReferableConverter;
  private final PartialComparator<TCReferable> myComparator;
  private List<TCReferable> myCurrentDefinitions = Collections.emptyList();
  private boolean myHeadersAreOK = true;

  private static CancellationIndicator CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;

  public TypecheckingOrderingListener(InstanceProviderSet instanceProviderSet, TypecheckerState state, ConcreteProvider concreteProvider, ReferableConverter referableConverter, ErrorReporter errorReporter, DependencyListener dependencyListener, PartialComparator<TCReferable> comparator) {
    myState = state;
    myErrorReporter = errorReporter;
    myDependencyListener = dependencyListener;
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myReferableConverter = referableConverter;
    myComparator = comparator;
  }

  public TypecheckingOrderingListener(InstanceProviderSet instanceProviderSet, TypecheckerState state, ConcreteProvider concreteProvider, ReferableConverter referableConverter, ErrorReporter errorReporter, PartialComparator<TCReferable> comparator) {
    this(instanceProviderSet, state, concreteProvider, referableConverter, errorReporter, DummyDependencyListener.INSTANCE, comparator);
  }

  public static void checkCanceled() throws ComputationInterruptedException {
    CANCELLATION_INDICATOR.checkCanceled();
  }

  public static CancellationIndicator getCancellationIndicator() {
    return CANCELLATION_INDICATOR;
  }

  public static void resetCancellationIndicator() {
    CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;
  }

  public ConcreteProvider getConcreteProvider() {
    return myConcreteProvider;
  }

  public ReferableConverter getReferableConverter() {
    return myReferableConverter;
  }

  public boolean runTypechecking(CancellationIndicator cancellationIndicator, BooleanSupplier runnable) {
    synchronized (TypecheckingOrderingListener.class) {
      if (cancellationIndicator != null) {
        CANCELLATION_INDICATOR = cancellationIndicator;
      }

      try {
        return runnable.getAsBoolean();
      } catch (ComputationInterruptedException ignored) {
        for (TCReferable currentDefinition : myCurrentDefinitions) {
          typecheckingInterrupted(currentDefinition, myState.reset(currentDefinition));
        }
        myCurrentDefinitions = Collections.emptyList();
        return false;
      } finally {
        if (cancellationIndicator != null) {
          CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;
        }
      }
    }
  }

  public boolean typecheckDefinitions(final Collection<? extends Concrete.Definition> definitions, CancellationIndicator cancellationIndicator) {
    return runTypechecking(cancellationIndicator, () -> {
      Ordering ordering = new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myState, myComparator);
      for (Concrete.Definition definition : definitions) {
        ordering.order(definition);
      }
      return true;
    });
  }

  public boolean typecheckModules(final Collection<? extends Group> modules, CancellationIndicator cancellationIndicator) {
    return runTypechecking(cancellationIndicator, () -> {
      new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myState, myComparator).orderModules(modules);
      return true;
    });
  }

  public boolean typecheckLibrary(Library library, CancellationIndicator cancellationIndicator) {
    return runTypechecking(cancellationIndicator, () -> library.orderModules(new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myState, myComparator)));
  }

  public boolean typecheckLibrary(Library library) {
    return typecheckLibrary(library, null);
  }

  public boolean typecheckCollected(CollectingOrderingListener collector, CancellationIndicator cancellationIndicator) {
    return runTypechecking(cancellationIndicator, () -> {
      collector.feed(this);
      return true;
    });
  }

  public void typecheckingHeaderStarted(TCReferable definition) {

  }

  public void typecheckingBodyStarted(TCReferable definition) {

  }

  public void typecheckingUnitStarted(TCReferable definition) {

  }

  public void typecheckingHeaderFinished(TCReferable referable, Definition definition) {

  }

  public void typecheckingBodyFinished(TCReferable referable, Definition definition) {

  }

  public void typecheckingUnitFinished(TCReferable referable, Definition definition) {

  }

  public void typecheckingInterrupted(TCReferable definition, @Nullable Definition typechecked) {

  }

  private Definition newDefinition(Concrete.Definition definition) {
    Definition typechecked;
    if (definition instanceof Concrete.DataDefinition) {
      typechecked = new DataDefinition(definition.getData());
      ((DataDefinition) typechecked).setSort(Sort.SET0);
      for (Concrete.ConstructorClause constructorClause : ((Concrete.DataDefinition) definition).getConstructorClauses()) {
        for (Concrete.Constructor constructor : constructorClause.getConstructors()) {
          Constructor tcConstructor = new Constructor(constructor.getData(), (DataDefinition) typechecked);
          tcConstructor.setParameters(EmptyDependentLink.getInstance());
          tcConstructor.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          ((DataDefinition) typechecked).addConstructor(tcConstructor);
          myState.record(constructor.getData(), tcConstructor);
        }
      }
    } else if (definition instanceof Concrete.FunctionDefinition) {
      typechecked = ((Concrete.FunctionDefinition) definition).getKind() == FunctionKind.CONS ? new DConstructor(definition.getData()) : new FunctionDefinition(definition.getData());
      ((FunctionDefinition) typechecked).setResultType(new ErrorExpression(null, null));
    } else if (definition instanceof Concrete.ClassDefinition) {
      typechecked = new ClassDefinition((TCClassReferable) definition.getData());
      for (Concrete.ClassField field : ((Concrete.ClassDefinition) definition).getFields()) {
        ClassField classField = new ClassField(field.getData(), (ClassDefinition) typechecked);
        classField.setType(new PiExpression(Sort.PROP, new TypedSingleDependentLink(false, "this", new ClassCallExpression((ClassDefinition) typechecked, Sort.STD), true), new ErrorExpression(null, null)));
        classField.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        ((ClassDefinition) typechecked).addPersonalField(classField);
        myState.record(classField.getReferable(), classField);
      }
    } else {
      throw new IllegalStateException();
    }
    typechecked.setStatus(Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
    myState.record(definition.getData(), typechecked);
    return typechecked;
  }

  @Override
  public void unitFound(Concrete.Definition definition, boolean recursive) {
    myHeadersAreOK = true;

    if (recursive) {
      Set<TCReferable> dependencies = new HashSet<>();
      definition.accept(new CollectDefCallsVisitor(myConcreteProvider, myInstanceProviderSet.get(definition.getData()), dependencies, false), null);
      if (dependencies.contains(definition.getData())) {
        typecheckingUnitStarted(definition.getData());
        myErrorReporter.report(new CycleError(Collections.singletonList(definition.getData())));
        typecheckingUnitFinished(definition.getData(), newDefinition(definition));
        return;
      }
    }

    definition.setRecursive(recursive);

    List<ExtClause> clauses;
    Definition typechecked;
    CheckTypeVisitor checkTypeVisitor = new CheckTypeVisitor(myState, new LinkedHashMap<>(), new LocalErrorReporter(definition.getData(), myErrorReporter), null);
    checkTypeVisitor.setInstancePool(new GlobalInstancePool(myState, myInstanceProviderSet.get(definition.getData()), checkTypeVisitor));
    DesugarVisitor.desugar(definition, myConcreteProvider, checkTypeVisitor.getErrorReporter());
    myCurrentDefinitions = Collections.singletonList(definition.getData());
    typecheckingUnitStarted(definition.getData());
    clauses = definition.accept(new DefinitionTypechecker(checkTypeVisitor), null);
    typechecked = myState.getTypechecked(definition.getData());

    if (definition.isRecursive() && typechecked instanceof FunctionDefinition && clauses != null) {
      checkRecursiveFunctions(Collections.singletonMap((FunctionDefinition) typechecked, definition), Collections.singletonMap((FunctionDefinition) typechecked, clauses));
    }

    typecheckingUnitFinished(definition.getData(), typechecked);
    myCurrentDefinitions = Collections.emptyList();
  }

  @Override
  public void cycleFound(List<Concrete.Definition> definitions) {
    List<TCReferable> cycle = new ArrayList<>();
    for (Concrete.Definition definition : definitions) {
      if (cycle.isEmpty() || cycle.get(cycle.size() - 1) != definition.getData()) {
        cycle.add(definition.getData());
      }

      Definition typechecked = myState.getTypechecked(definition.getData());
      if (typechecked == null) {
        typechecked = newDefinition(definition);
      }
      typechecked.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);

      typecheckingUnitStarted(definition.getData());
      mySuspensions.remove(definition.getData());
      typecheckingUnitFinished(definition.getData(), typechecked);
    }
    myErrorReporter.report(new CycleError(cycle));
  }

  @Override
  public void headerFound(Concrete.Definition definition) {
    myCurrentDefinitions = Collections.singletonList(definition.getData());
    typecheckingHeaderStarted(definition.getData());

    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    CheckTypeVisitor visitor = new CheckTypeVisitor(myState, new LinkedHashMap<>(), new LocalErrorReporter(definition.getData(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter)), null);
    if (definition.hasErrors()) {
      visitor.setHasErrors();
    }
    DesugarVisitor.desugar(definition, myConcreteProvider, visitor.getErrorReporter());
    Definition oldTypechecked = visitor.getTypecheckingState().getTypechecked(definition.getData());
    definition.setRecursive(true);
    Definition typechecked = new DefinitionTypechecker(visitor).typecheckHeader(oldTypechecked, new GlobalInstancePool(myState, myInstanceProviderSet.get(definition.getData()), visitor), definition);
    if (typechecked.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
      mySuspensions.put(definition.getData(), new Pair<>(visitor, oldTypechecked == null));
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
    Map<FunctionDefinition, List<ExtClause>> clausesMap = new HashMap<>();
    Set<DataDefinition> dataDefinitions = new HashSet<>();
    List<Concrete.Definition> orderedDefinitions = new ArrayList<>(definitions.size());
    List<Concrete.Definition> otherDefs = new ArrayList<>();
    for (Concrete.Definition definition : definitions) {
      Definition typechecked = myState.getTypechecked(definition.getData());
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
    for (Concrete.Definition definition : orderedDefinitions) {
      typecheckingBodyStarted(definition.getData());

      Definition def = myState.getTypechecked(definition.getData());
      Pair<CheckTypeVisitor, Boolean> pair = mySuspensions.remove(definition.getData());
      if (myHeadersAreOK && pair != null) {
        typechecking.setTypechecker(pair.proj1);
        List<ExtClause> clauses = typechecking.typecheckBody(def, definition, dataDefinitions, pair.proj2);
        if (clauses != null) {
          functionDefinitions.put((FunctionDefinition) def, definition);
          clausesMap.put((FunctionDefinition) def, clauses);
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
        visitor.findDefinition(entry.getKey().getBody());
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

    for (Concrete.Definition definition : orderedDefinitions) {
      typecheckingBodyFinished(definition.getData(), myState.getTypechecked(definition.getData()));
    }
  }

  @Override
  public void useFound(List<Concrete.UseDefinition> definitions) {
    myCurrentDefinitions = new ArrayList<>();
    for (Concrete.UseDefinition definition : definitions) {
      myCurrentDefinitions.add(definition.getData());
      myCurrentDefinitions.add(definition.getUseParent());
    }
    UseTypechecking.typecheck(definitions, myState, myErrorReporter);
    myCurrentDefinitions = Collections.emptyList();
  }

  private void checkRecursiveFunctions(Map<FunctionDefinition,Concrete.Definition> definitions, Map<FunctionDefinition,List<ExtClause>> clauses) {
    DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
    for (Map.Entry<FunctionDefinition, Concrete.Definition> entry : definitions.entrySet()) {
      List<ExtClause> functionClauses = clauses.get(entry.getKey());
      if (functionClauses != null) {
        definitionCallGraph.add(entry.getKey(), functionClauses, definitions.keySet());
      }
      for (DependentLink link = entry.getKey().getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (FindDefCallVisitor.findDefinition(link.getTypeExpr(), definitions.keySet()) != null) {
          myErrorReporter.report(new TypecheckingError("Mutually recursive functions are not allowed in parameters", entry.getValue()).withDefinition(entry.getKey().getReferable()));
        }
      }
    }

    DefinitionCallGraph callCategory = new DefinitionCallGraph(definitionCallGraph);
    if (!callCategory.checkTermination()) {
      for (FunctionDefinition definition : definitions.keySet()) {
        definition.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        definition.setBody(null);
      }
      for (Map.Entry<Definition, Set<RecursiveBehavior<Definition>>> entry : callCategory.myErrorInfo.entrySet()) {
        myErrorReporter.report(new TerminationCheckError(entry.getKey(), entry.getValue()));
      }
    }
  }
}
