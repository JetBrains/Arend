package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.Clause;
import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.CycleError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TerminationCheckError;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.order.SCC;
import com.jetbrains.jetpad.vclang.typechecking.termination.DefinitionCallGraph;
import com.jetbrains.jetpad.vclang.typechecking.termination.RecursiveBehavior;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.Typecheckable;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProviderSet;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

class TypecheckingDependencyListener implements DependencyListener {
  private final TypecheckerState myState;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final TypecheckedReporter myTypecheckedReporter;
  private final DependencyListener myDependencyListener;
  private final Map<GlobalReferable, Suspension> mySuspensions = new HashMap<>();
  private boolean myTypecheckingHeaders = false;

  final ErrorReporter errorReporter;
  final InstanceProviderSet instanceProviderSet;
  final ConcreteProvider concreteProvider;

  private static class Suspension {
    public final CheckTypeVisitor visitor;
    public final CountingErrorReporter countingErrorReporter;

    public Suspension(CheckTypeVisitor visitor, CountingErrorReporter countingErrorReporter) {
      this.visitor = visitor;
      this.countingErrorReporter = countingErrorReporter;
    }
  }

  TypecheckingDependencyListener(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ConcreteProvider concreteProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener dependencyListener) {
    myState = state;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    this.errorReporter = errorReporter;
    myTypecheckedReporter = typecheckedReporter;
    myDependencyListener = dependencyListener;
    instanceProviderSet = new InstanceProviderSet();
    this.concreteProvider = concreteProvider;
  }

  @Override
  public void sccFound(SCC scc) {
    for (TypecheckingUnit unit : scc.getUnits()) {
      if (!Typecheckable.hasHeader(unit.getDefinition())) {
        List<Concrete.Definition> cycle = new ArrayList<>();
        for (TypecheckingUnit unit1 : scc.getUnits()) {
          Concrete.Definition definition = unit1.getDefinition();
          cycle.add(definition);
          if (!unit1.isHeader()) {
            if (Typecheckable.hasHeader(definition)) {
              mySuspensions.remove(definition.getData());
            }
            myTypecheckedReporter.typecheckingFailed(definition);
          }
          if (myState.getTypechecked(definition.getData()) == null) {
            myState.record(definition.getData(), Definition.newDefinition(definition));
          }
        }
        errorReporter.report(new CycleError(cycle));
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

    myDependencyListener.sccFound(scc);
  }

  @Override
  public void unitFound(TypecheckingUnit unit, Recursion recursion) {
    if (recursion == Recursion.IN_HEADER) {
      myState.record(unit.getDefinition().getData(), Definition.newDefinition(unit.getDefinition()));
      errorReporter.report(new CycleError(Collections.singletonList(unit.getDefinition())));
      myTypecheckedReporter.typecheckingFailed(unit.getDefinition());
    } else {
      typecheck(unit, recursion == Recursion.IN_BODY);
    }

    myDependencyListener.unitFound(unit, recursion);
  }

  @Override
  public boolean needsOrdering(Concrete.Definition definition) {
    if (!myDependencyListener.needsOrdering(definition)) {
      return false;
    }

    Definition typechecked = myState.getTypechecked(definition.getData());
    return typechecked == null || typechecked.status().needsTypeChecking();
  }

  @Override
  public void alreadyTypechecked(Concrete.Definition definition) {
    myTypecheckedReporter.typecheckingSucceeded(definition);
    myDependencyListener.alreadyTypechecked(definition);
  }

  @Override
  public void dependsOn(Typecheckable unit, Concrete.Definition def) {
    myDependencyListener.dependsOn(unit, def);
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
      CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
      LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(errorReporter, countingErrorReporter));
      CheckTypeVisitor visitor = new CheckTypeVisitor(myState, myStaticNsProvider, myDynamicNsProvider, new LinkedHashMap<>(), localErrorReporter, null);
      Definition typechecked = DefinitionTypechecking.typecheckHeader(visitor, new GlobalInstancePool(myState, instanceProviderSet.getInstanceProvider(unit.getDefinition().getData())), unit.getDefinition(), unit.getEnclosingClass());
      if (typechecked.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
        mySuspensions.put(unit.getDefinition().getData(), new Suspension(visitor, countingErrorReporter));
      }
      return typechecked.status().headerIsOK();
    }

    if (myTypecheckingHeaders) {
      List<Concrete.Definition> cycle = new ArrayList<>(scc.getUnits().size());
      for (TypecheckingUnit unit1 : scc.getUnits()) {
        cycle.add(unit1.getDefinition());
      }

      errorReporter.report(new CycleError(cycle));
      for (Concrete.Definition definition : cycle) {
        myState.record(definition.getData(), Definition.newDefinition(definition));
      }
      return false;
    }

    myTypecheckingHeaders = true;
    Ordering ordering = new Ordering(instanceProviderSet, concreteProvider, this, true);
    boolean ok = true;
    for (TypecheckingUnit unit1 : scc.getUnits()) {
      if (unit1.isHeader()) {
        Concrete.Definition definition = unit1.getDefinition();
        ordering.doOrder(definition);
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

    List<Pair<Concrete.Definition, Boolean>> results = new ArrayList<>(definitions.size());
    for (Concrete.Definition definition : definitions) {
      Suspension suspension = mySuspensions.remove(definition.getData());
      if (headersAreOK && suspension != null) {
        Definition def = myState.getTypechecked(definition.getData());
        List<Clause> clauses = DefinitionTypechecking.typecheckBody(def, definition, suspension.visitor, dataDefinitions);
        if (clauses != null) {
          functionDefinitions.add((FunctionDefinition) def);
          clausesMap.put((FunctionDefinition) def, clauses);
        }
      }

      results.add(new Pair<>(definition, suspension != null && suspension.countingErrorReporter.getErrorsNumber() == 0));
    }

    boolean ok = true;
    if (!functionDefinitions.isEmpty()) {
      DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
      for (FunctionDefinition fDef : functionDefinitions) {
        definitionCallGraph.add(fDef, clausesMap.get(fDef), functionDefinitions);
      }
      DefinitionCallGraph callCategory = new DefinitionCallGraph(definitionCallGraph);
      if (!callCategory.checkTermination()) {
        ok = false;
        for (FunctionDefinition fDef : functionDefinitions) {
          fDef.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
        }
        for (Map.Entry<Definition, Set<RecursiveBehavior<Definition>>> entry : callCategory.myErrorInfo.entrySet()) {
          errorReporter.report(new TerminationCheckError(entry.getKey(), entry.getValue()));
        }
      }
    }

    for (Pair<Concrete.Definition, Boolean> result : results) {
      if (ok && result.proj2) {
        myTypecheckedReporter.typecheckingSucceeded(result.proj1);
      } else {
        Definition typechecked = myState.getTypechecked(result.proj1.getData());
        if (typechecked.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
          typechecked.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
        myTypecheckedReporter.typecheckingFailed(result.proj1);
      }
    }
  }

  private void typecheck(TypecheckingUnit unit, boolean recursive) {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    CompositeErrorReporter compositeErrorReporter = new CompositeErrorReporter(errorReporter, countingErrorReporter);
    LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), compositeErrorReporter);
    List<Clause> clauses = DefinitionTypechecking.typecheck(myState, new GlobalInstancePool(myState, instanceProviderSet.getInstanceProvider(unit.getDefinition().getData())), myStaticNsProvider, myDynamicNsProvider, unit, recursive, localErrorReporter);
    Definition typechecked = myState.getTypechecked(unit.getDefinition().getData());

    if (recursive && clauses != null) {
      DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
      definitionCallGraph.add((FunctionDefinition) typechecked, clauses, Collections.singleton(typechecked));
      DefinitionCallGraph callCategory = new DefinitionCallGraph(definitionCallGraph);
      if (!callCategory.checkTermination()) {
        typechecked.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
        for (Map.Entry<Definition, Set<RecursiveBehavior<Definition>>> entry : callCategory.myErrorInfo.entrySet()) {
          compositeErrorReporter.report(new TerminationCheckError(entry.getKey(), entry.getValue()));
        }
      }
    }

    if (countingErrorReporter.getErrorsNumber() == 0) {
      myTypecheckedReporter.typecheckingSucceeded(unit.getDefinition());
    } else {
      if (typechecked.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
        typechecked.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
      }
      myTypecheckedReporter.typecheckingFailed(unit.getDefinition());
    }
  }
}
