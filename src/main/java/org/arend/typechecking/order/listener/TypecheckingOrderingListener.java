package org.arend.typechecking.order.listener;

import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.Clause;
import org.arend.error.CompositeErrorReporter;
import org.arend.error.CountingErrorReporter;
import org.arend.error.ErrorReporter;
import org.arend.library.Library;
import org.arend.naming.reference.GlobalReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.typechecking.CancellationIndicator;
import org.arend.typechecking.DefinitionTypechecking;
import org.arend.typechecking.ThreadCancellationIndicator;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.CycleError;
import org.arend.typechecking.error.TerminationCheckError;
import org.arend.typechecking.error.local.ProxyErrorReporter;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.Ordering;
import org.arend.typechecking.order.SCC;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.dependency.DummyDependencyListener;
import org.arend.typechecking.termination.DefinitionCallGraph;
import org.arend.typechecking.termination.RecursiveBehavior;
import org.arend.typechecking.typecheckable.TypecheckingUnit;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.typechecking.visitor.DesugarVisitor;
import org.arend.util.ComputationInterruptedException;

import java.util.*;

public class TypecheckingOrderingListener implements OrderingListener {
  private final TypecheckerState myState;
  private final DependencyListener myDependencyListener;
  private final Map<GlobalReferable, CheckTypeVisitor> mySuspensions = new HashMap<>();
  private final ErrorReporter myErrorReporter;
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private boolean myTypecheckingHeaders = false;

  public static CancellationIndicator CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;

  public static void setDefaultCancellationIndicator() {
    CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;
  }

  public TypecheckingOrderingListener(InstanceProviderSet instanceProviderSet, TypecheckerState state, ConcreteProvider concreteProvider, ErrorReporter errorReporter, DependencyListener dependencyListener) {
    myState = state;
    myErrorReporter = errorReporter;
    myDependencyListener = dependencyListener;
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
  }

  public TypecheckingOrderingListener(InstanceProviderSet instanceProviderSet, TypecheckerState state, ConcreteProvider concreteProvider, ErrorReporter errorReporter) {
    this(instanceProviderSet, state, concreteProvider, errorReporter, DummyDependencyListener.INSTANCE);
  }

  public TypecheckingOrderingListener(Ordering ordering, ErrorReporter errorReporter) {
    myState = ordering.getTypecheckerState();
    myErrorReporter = errorReporter;
    myDependencyListener = ordering.getDependencyListener();
    myInstanceProviderSet = ordering.getInstanceProviderSet();
    myConcreteProvider = ordering.getConcreteProvider();
  }

  public boolean typecheckDefinitions(final Collection<? extends Concrete.Definition> definitions) {
    try {
      Ordering ordering = new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, IdReferableConverter.INSTANCE, myState, false);
      for (Concrete.Definition definition : definitions) {
        ordering.orderDefinition(definition);
      }
      return true;
    } catch (ComputationInterruptedException ignored) {
      return false;
    }
  }

  public boolean typecheckModules(final Collection<? extends Group> modules) {
    try {
      new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, IdReferableConverter.INSTANCE, myState, false).orderModules(modules);
      return true;
    } catch (ComputationInterruptedException ignored) {
      return false;
    }
  }

  public boolean typecheckLibrary(Library library) {
    try {
      return library.orderModules(new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, IdReferableConverter.INSTANCE, myState, false));
    } catch (ComputationInterruptedException ignored) {
      return false;
    }
  }

  public boolean typecheckCollected(CollectingOrderingListener collector) {
    try {
      collector.feed(this);
      return true;
    } catch (ComputationInterruptedException ignored) {
      return false;
    }
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

  @Override
  public void sccFound(SCC scc) {
    for (TypecheckingUnit unit : scc.getUnits()) {
      if (!TypecheckingUnit.hasHeader(unit.getDefinition())) {
        List<Concrete.Definition> cycle = new ArrayList<>();
        for (TypecheckingUnit unit1 : scc.getUnits()) {
          Concrete.Definition definition = unit1.getDefinition();
          cycle.add(definition);

          Definition typechecked = myState.getTypechecked(definition.getData());
          if (typechecked == null) {
            typechecked = Definition.newDefinition(definition);
            myState.record(definition.getData(), typechecked);
          }
          if (typechecked.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
            typechecked.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          }

          if (!unit1.isHeader()) {
            typecheckingUnitStarted(definition.getData());
            if (TypecheckingUnit.hasHeader(definition)) {
              mySuspensions.remove(definition.getData());
            }
            typecheckingUnitFinished(definition.getData(), typechecked);
          }
        }
        myErrorReporter.report(new CycleError(cycle));
        return;
      }
    }

    boolean ok = typecheckHeaders(scc);
    List<Concrete.Definition> definitions = new ArrayList<>(scc.getUnits().size());
    for (TypecheckingUnit unit : scc.getUnits()) {
      if (!unit.isHeader()) {
        definitions.add(unit.getDefinition());
      }
    }
    if (!definitions.isEmpty()) {
      typecheckBodies(definitions, ok);
    }
  }

  @Override
  public void unitFound(TypecheckingUnit unit, Recursion recursion) {
    if (recursion == Recursion.IN_HEADER) {
      typecheckingUnitStarted(unit.getDefinition().getData());
      Definition typechecked = Definition.newDefinition(unit.getDefinition());
      myState.record(unit.getDefinition().getData(), typechecked);
      myErrorReporter.report(new CycleError(Collections.singletonList(unit.getDefinition())));
      typecheckingUnitFinished(unit.getDefinition().getData(), typechecked);
    } else {
      typecheck(unit, recursion == Recursion.IN_BODY);
    }
  }

  private boolean typecheckHeaders(SCC scc) {
    int numberOfHeaders = 0;
    TypecheckingUnit unit = null;
    for (TypecheckingUnit unit1 : scc.getUnits()) {
      if (unit1.isHeader()) {
        unit = unit1;
        numberOfHeaders++;
      }
    }

    if (numberOfHeaders == 0) {
      return true;
    }

    if (numberOfHeaders == 1) {
      typecheckingHeaderStarted(unit.getDefinition().getData());

      CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
      CheckTypeVisitor visitor = new CheckTypeVisitor(myState, new LinkedHashMap<>(), new ProxyErrorReporter(unit.getDefinition().getData(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter)), null);
      if (unit.getDefinition().hasErrors()) {
        visitor.setHasErrors();
      }
      DesugarVisitor.desugar(unit.getDefinition(), myConcreteProvider, visitor.getErrorReporter());
      Definition typechecked = new DefinitionTypechecking(visitor).typecheckHeader(new GlobalInstancePool(myState, myInstanceProviderSet.get(unit.getDefinition().getData()), visitor), unit.getDefinition());
      if (typechecked.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
        mySuspensions.put(unit.getDefinition().getData(), visitor);
      }

      typecheckingHeaderFinished(unit.getDefinition().getData(), typechecked);
      return typechecked.status().headerIsOK();
    }

    if (myTypecheckingHeaders) {
      List<Concrete.Definition> cycle = new ArrayList<>(scc.getUnits().size());
      for (TypecheckingUnit unit1 : scc.getUnits()) {
        cycle.add(unit1.getDefinition());
      }

      myErrorReporter.report(new CycleError(cycle));
      for (Concrete.Definition definition : cycle) {
        typecheckingHeaderStarted(definition.getData());
        Definition typechecked = Definition.newDefinition(definition);
        myState.record(definition.getData(), typechecked);
        typecheckingHeaderFinished(definition.getData(), typechecked);
      }
      return false;
    }

    myTypecheckingHeaders = true;
    Ordering ordering = new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, IdReferableConverter.INSTANCE, myState, true);
    boolean ok = true;
    for (TypecheckingUnit unit1 : scc.getUnits()) {
      if (unit1.isHeader()) {
        Concrete.Definition definition = unit1.getDefinition();
        ordering.orderDefinition(definition);
        if (ok && !myState.getTypechecked(definition.getData()).status().headerIsOK()) {
          ok = false;
        }
      }
    }
    myTypecheckingHeaders = false;
    return ok;
  }

  private void typecheckBodies(List<Concrete.Definition> definitions, boolean headersAreOK) {
    Set<FunctionDefinition> functionDefinitions = new HashSet<>();
    Map<FunctionDefinition, List<Clause>> clausesMap = new HashMap<>();
    Set<DataDefinition> dataDefinitions = new HashSet<>();
    for (Concrete.Definition definition : definitions) {
      Definition typechecked = myState.getTypechecked(definition.getData());
      if (typechecked instanceof DataDefinition) {
        dataDefinitions.add((DataDefinition) typechecked);
      }
    }

    DefinitionTypechecking typechecking = new DefinitionTypechecking(null);
    for (Concrete.Definition definition : definitions) {
      typecheckingBodyStarted(definition.getData());

      Definition def = myState.getTypechecked(definition.getData());
      CheckTypeVisitor visitor = mySuspensions.remove(definition.getData());
      if (headersAreOK && visitor != null) {
        typechecking.setVisitor(visitor);
        List<Clause> clauses = typechecking.typecheckBody(def, definition, dataDefinitions);
        if (clauses != null) {
          functionDefinitions.add((FunctionDefinition) def);
          clausesMap.put((FunctionDefinition) def, clauses);
        }
      }

      typecheckingBodyFinished(definition.getData(), def);
    }

    if (!functionDefinitions.isEmpty()) {
      DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
      for (FunctionDefinition fDef : functionDefinitions) {
        definitionCallGraph.add(fDef, clausesMap.get(fDef), functionDefinitions);
      }
      DefinitionCallGraph callCategory = new DefinitionCallGraph(definitionCallGraph);
      if (!callCategory.checkTermination()) {
        for (FunctionDefinition fDef : functionDefinitions) {
          fDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
        }
        for (Map.Entry<Definition, Set<RecursiveBehavior<Definition>>> entry : callCategory.myErrorInfo.entrySet()) {
          myErrorReporter.report(new TerminationCheckError(entry.getKey(), functionDefinitions, entry.getValue()));
        }
      }
    }
  }

  private void typecheck(TypecheckingUnit unit, boolean recursive) {
    typecheckingUnitStarted(unit.getDefinition().getData());

    CheckTypeVisitor checkTypeVisitor = new CheckTypeVisitor(myState, new LinkedHashMap<>(), new ProxyErrorReporter(unit.getDefinition().getData(), myErrorReporter), null);
    DesugarVisitor.desugar(unit.getDefinition(), myConcreteProvider, checkTypeVisitor.getErrorReporter());
    GlobalInstancePool pool = new GlobalInstancePool(myState, myInstanceProviderSet.get(unit.getDefinition().getData()), checkTypeVisitor);
    checkTypeVisitor.setInstancePool(pool);
    List<Clause> clauses = unit.getDefinition().accept(new DefinitionTypechecking(checkTypeVisitor), recursive);
    Definition typechecked = myState.getTypechecked(unit.getDefinition().getData());

    if (recursive && clauses != null) {
      DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
      definitionCallGraph.add((FunctionDefinition) typechecked, clauses, Collections.singleton(typechecked));
      DefinitionCallGraph callCategory = new DefinitionCallGraph(definitionCallGraph);
      if (!callCategory.checkTermination()) {
        typechecked.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
        for (Map.Entry<Definition, Set<RecursiveBehavior<Definition>>> entry : callCategory.myErrorInfo.entrySet()) {
          myErrorReporter.report(new TerminationCheckError(entry.getKey(), Collections.singleton(entry.getKey()), entry.getValue()));
        }
      }
    }

    typecheckingUnitFinished(unit.getDefinition().getData(), typechecked);
  }
}
