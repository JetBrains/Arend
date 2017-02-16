package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.LevelBinding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
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
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.ClassViewInstanceProvider;
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
  private GlobalInstancePool myInstancePool;

  private static class Suspension {
    public CheckTypeVisitor visitor;
    public CountingErrorReporter countingErrorReporter;

    public Suspension(CheckTypeVisitor visitor, CountingErrorReporter countingErrorReporter) {
      this.visitor = visitor;
      this.countingErrorReporter = countingErrorReporter;
    }
  }

  TypecheckingDependencyListener(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener dependencyListener) {
    myState = state;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myErrorReporter = errorReporter;
    myTypecheckedReporter = typecheckedReporter;
    myDependencyListener = dependencyListener;
  }

  void setInstanceProvider(ClassViewInstanceProvider instanceProvider) {
    myInstancePool = new GlobalInstancePool(myState, instanceProvider);
  }


  @Override
  public void sccFound(SCC scc) {
    typecheck(scc);
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
  public void alreadyTypechecked(Definition definition) {
    myTypecheckedReporter.typecheckingSucceeded(definition.getAbstractDefinition());
    myDependencyListener.alreadyTypechecked(definition);
  }

  @Override
  public void dependsOn(Typecheckable unit, Abstract.Definition def) {
    myDependencyListener.dependsOn(unit, def);
  }


  private void typecheckHeaders(SCC scc, GlobalInstancePool instancePool) {
    if (scc.getUnits().size() > 1) {
      int numberOfHeaders = 0;
      for (TypecheckingUnit unit : scc.getUnits()) {
        if (unit.isHeader()) {
          numberOfHeaders++;
        }
      }

      if (numberOfHeaders > 1) {

      }
    }
  }

  private void typecheck(SCC scc) {
    if (scc.getUnits().size() > 1) {
      for (TypecheckingUnit unit : scc.getUnits()) {
        if (unit.isHeader() || !(unit.getDefinition() instanceof Abstract.FunctionDefinition)) {
          throw new Ordering.SCCException(scc);
        }
      }
    }

    Set<FunctionDefinition> cycleDefs = new HashSet<>();
    List<Pair<Abstract.Definition, Boolean>> results = new ArrayList<>(scc.getUnits().size());
    for (TypecheckingUnit unit : scc.getUnits()) {
      CountingErrorReporter countingErrorReporter = null;
      boolean doReport;

      if (Typecheckable.hasHeader(unit.getDefinition())) {
        if (unit.isHeader()) {
          countingErrorReporter = new CountingErrorReporter();
          LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
          CheckTypeVisitor visitor = new CheckTypeVisitor(myState, myStaticNsProvider, myDynamicNsProvider, null, null, new ArrayList<Binding>(), new ArrayList<LevelBinding>(), localErrorReporter, null);
          Definition typechecked = DefinitionCheckType.typeCheckHeader(visitor, myInstancePool, unit.getDefinition(), unit.getEnclosingClass());
          if (typechecked.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
            mySuspensions.put(unit.getDefinition(), new Suspension(visitor, countingErrorReporter));
            doReport = false;
          } else {
            doReport = true;
          }
        } else {
          Suspension suspension = mySuspensions.get(unit.getDefinition());
          if (suspension != null) {
            Definition def = myState.getTypechecked(unit.getDefinition());
            DefinitionCheckType.typeCheckBody(def, suspension.visitor);
            if (def instanceof FunctionDefinition) {
              cycleDefs.add((FunctionDefinition) def);
            }
            countingErrorReporter = suspension.countingErrorReporter;
            mySuspensions.remove(unit.getDefinition());
            doReport = true;
          } else {
            doReport = false;
          }
        }
      } else {
        countingErrorReporter = new CountingErrorReporter();
        LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
        DefinitionCheckType.typeCheck(myState, myInstancePool, myStaticNsProvider, myDynamicNsProvider, unit, true, localErrorReporter);
        doReport = true;
      }

      if (doReport) {
        results.add(new Pair<>(unit.getDefinition(), countingErrorReporter == null || countingErrorReporter.getErrorsNumber() == 0));
      }
    }

    boolean ok = true;
    if (!cycleDefs.isEmpty()) {
      DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
      for (FunctionDefinition fDef : cycleDefs) definitionCallGraph.add(fDef, cycleDefs);
      DefinitionCallGraph callCategory = new DefinitionCallGraph(definitionCallGraph);
      if (!callCategory.checkTermination()) {
        ok = false;
        for (FunctionDefinition fDef : cycleDefs) {
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
    Definition typechecked = DefinitionCheckType.typeCheck(myState, myInstancePool, myStaticNsProvider, myDynamicNsProvider, unit, recursive, localErrorReporter);

    if (recursive && typechecked instanceof FunctionDefinition) {
      DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
      definitionCallGraph.add((FunctionDefinition) typechecked, Collections.singleton(typechecked));
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
