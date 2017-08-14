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
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.CycleError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TerminationCheckError;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.order.SCC;
import com.jetbrains.jetpad.vclang.typechecking.termination.DefinitionCallGraph;
import com.jetbrains.jetpad.vclang.typechecking.termination.RecursiveBehavior;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProviderSet;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceNamespaceProvider;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

class TypecheckingDependencyListener implements DependencyListener {
  private final TypecheckerState myState;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final ErrorReporter myErrorReporter;
  private final TypecheckedReporter myTypecheckedReporter;
  private final DependencyListener myDependencyListener;
  private final Map<Abstract.Definition, Suspension> mySuspensions = new HashMap<>();
  private final InstanceProviderSet myInstanceProviderSet;
  private boolean myTypecheckingHeaders = false;

  private static class Suspension {
    public final CheckTypeVisitor visitor;
    public final CountingErrorReporter countingErrorReporter;

    public Suspension(CheckTypeVisitor visitor, CountingErrorReporter countingErrorReporter) {
      this.visitor = visitor;
      this.countingErrorReporter = countingErrorReporter;
    }
  }

  TypecheckingDependencyListener(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, InstanceNamespaceProvider instanceNamespaceProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener dependencyListener) {
    myState = state;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myErrorReporter = errorReporter;
    myTypecheckedReporter = typecheckedReporter;
    myDependencyListener = dependencyListener;
    myInstanceProviderSet = new InstanceProviderSet(instanceNamespaceProvider);
  }

  InstanceProviderSet getInstanceProviderProvider() {
    return myInstanceProviderSet;
  }

  ErrorReporter getErrorReporter() {
    return myErrorReporter;
  }

  @Override
  public void sccFound(SCC scc) {
    for (TypecheckingUnit unit : scc.getUnits()) {
      if (!Typecheckable.hasHeader(unit.getDefinition())) {
        List<Abstract.Definition> cycle = new ArrayList<>();
        for (TypecheckingUnit unit1 : scc.getUnits()) {
          Abstract.Definition definition = unit1.getDefinition();
          cycle.add(definition);
          if (!unit1.isHeader()) {
            if (Typecheckable.hasHeader(definition)) {
              mySuspensions.remove(definition);
            }
            myTypecheckedReporter.typecheckingFailed(definition);
          }
          if (myState.getTypechecked(definition) == null) {
            myState.record(definition, Definition.newDefinition(definition));
          }
        }
        myErrorReporter.report(new CycleError(cycle));
        return;
      }
    }

    boolean ok = typecheckHeaders(scc);
    List<Abstract.Definition> definitions = new ArrayList<>(scc.getUnits().size());
    for (TypecheckingUnit unit : scc.getUnits()) {
      if (!unit.isHeader()) {
        definitions.add(unit.getDefinition());
      }
    }
    typecheckBodies(definitions, ok);

    myDependencyListener.sccFound(scc);
  }

  @Override
  public void unitFound(TypecheckingUnit unit, Recursion recursion) {
    if (recursion == Recursion.IN_HEADER) {
      myState.record(unit.getDefinition(), Definition.newDefinition(unit.getDefinition()));
      myErrorReporter.report(new CycleError(Collections.singletonList(unit.getDefinition())));
      myTypecheckedReporter.typecheckingFailed(unit.getDefinition());
    } else {
      typecheck(unit, recursion == Recursion.IN_BODY);
    }

    myDependencyListener.unitFound(unit, recursion);
  }

  @Override
  public boolean needsOrdering(Abstract.Definition definition) {
    if (!myDependencyListener.needsOrdering(definition)) {
      return false;
    }

    Definition typechecked = myState.getTypechecked(definition);
    return typechecked == null || typechecked.status().needsTypeChecking();
  }

  @Override
  public void alreadyTypechecked(Abstract.Definition definition) {
    myTypecheckedReporter.typecheckingSucceeded(definition);
    myDependencyListener.alreadyTypechecked(definition);
  }

  @Override
  public void dependsOn(Typecheckable unit, Abstract.Definition def) {
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
      LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
      CheckTypeVisitor visitor = new CheckTypeVisitor(myState, myStaticNsProvider, myDynamicNsProvider, new LinkedHashMap<>(), localErrorReporter, null);
      Definition typechecked = DefinitionTypechecking.typecheckHeader(visitor, new GlobalInstancePool(myState, myInstanceProviderSet.getInstanceProvider(unit.getDefinition())), unit.getDefinition(), unit.getEnclosingClass());
      if (typechecked.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
        mySuspensions.put(unit.getDefinition(), new Suspension(visitor, countingErrorReporter));
      }
      return typechecked.status().headerIsOK();
    }

    if (myTypecheckingHeaders) {
      List<Abstract.Definition> cycle = new ArrayList<>(scc.getUnits().size());
      for (TypecheckingUnit unit1 : scc.getUnits()) {
        cycle.add(unit1.getDefinition());
      }

      myErrorReporter.report(new CycleError(cycle));
      for (Abstract.Definition definition : cycle) {
        myState.record(definition, Definition.newDefinition(definition));
      }
      return false;
    }

    myTypecheckingHeaders = true;
    Ordering ordering = new Ordering(myInstanceProviderSet, this, true);
    boolean ok = true;
    for (TypecheckingUnit unit1 : scc.getUnits()) {
      if (unit1.isHeader()) {
        Abstract.Definition definition = unit1.getDefinition();
        ordering.doOrder(definition);
        if (ok && !myState.getTypechecked(definition).status().headerIsOK()) {
          ok = false;
        }
      }
    }
    myTypecheckingHeaders = false;
    return ok;
  }

  private void typecheckBodies(List<Abstract.Definition> definitions, boolean headersAreOK) {
    Set<FunctionDefinition> functionDefinitions = new HashSet<>();
    Map<FunctionDefinition, List<Clause>> clausesMap = new HashMap<>();
    Set<DataDefinition> dataDefinitions = new HashSet<>();
    for (Abstract.Definition definition : definitions) {
      Definition typechecked = myState.getTypechecked(definition);
      if (typechecked instanceof DataDefinition) {
        dataDefinitions.add((DataDefinition) typechecked);
      }
    }

    List<Pair<Abstract.Definition, Boolean>> results = new ArrayList<>(definitions.size());
    for (Abstract.Definition definition : definitions) {
      Suspension suspension = mySuspensions.remove(definition);
      if (headersAreOK && suspension != null) {
        Definition def = myState.getTypechecked(definition);
        List<Clause> clauses = DefinitionTypechecking.typecheckBody(def, suspension.visitor, dataDefinitions);
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
          myErrorReporter.report(new TerminationCheckError(entry.getKey(), entry.getValue()));
        }
      }
    }

    for (Pair<Abstract.Definition, Boolean> result : results) {
      if (ok && result.proj2) {
        myTypecheckedReporter.typecheckingSucceeded(result.proj1);
      } else {
        Definition typechecked = myState.getTypechecked(result.proj1);
        if (typechecked.status() == Definition.TypeCheckingStatus.NO_ERRORS) {
          typechecked.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
        myTypecheckedReporter.typecheckingFailed(result.proj1);
      }
    }
  }

  private void typecheck(TypecheckingUnit unit, boolean recursive) {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    CompositeErrorReporter compositeErrorReporter = new CompositeErrorReporter(myErrorReporter, countingErrorReporter);
    LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), compositeErrorReporter);
    List<Clause> clauses = DefinitionTypechecking.typecheck(myState, new GlobalInstancePool(myState, myInstanceProviderSet.getInstanceProvider(unit.getDefinition())), myStaticNsProvider, myDynamicNsProvider, unit, recursive, localErrorReporter);
    Definition typechecked = myState.getTypechecked(unit.getDefinition());

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
