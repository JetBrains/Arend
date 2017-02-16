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
  private ClassViewInstanceProvider myInstanceProvider;
  private boolean myTypecheckingHeaders = false;

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
    myInstanceProvider = instanceProvider;
  }


  @Override
  public void sccFound(SCC scc) {
    for (TypecheckingUnit unit : scc.getUnits()) {
      if (!Typecheckable.hasHeader(unit.getDefinition())) {
        List<Abstract.Definition> cycle = new ArrayList<>();
        for (TypecheckingUnit unit1 : scc.getUnits()) {
          cycle.add(unit1.getDefinition());
          boolean doReport = true;
          if (!unit1.isHeader() && Typecheckable.hasHeader(unit1.getDefinition())) {
            Suspension suspension = mySuspensions.remove(unit1.getDefinition());
            if (suspension == null) {
              doReport = false;
            }
          }
          if (doReport) {
            myTypecheckedReporter.typecheckingFailed(unit1.getDefinition());
          }
        }
        myErrorReporter.report(new CycleError(cycle));
        return;
      }
    }

    boolean ok = typecheckHeaders(scc);
    typecheckBodies(scc, ok);

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

    if (numberOfHeaders > 1) {
      if (myTypecheckingHeaders) {
        List<Abstract.Definition> cycle = new ArrayList<>(scc.getUnits().size());
        for (TypecheckingUnit unit1 : scc.getUnits()) {
          cycle.add(unit1.getDefinition());
        }

        myErrorReporter.report(new CycleError(cycle));
        for (Abstract.Definition definition : cycle) {
          myState.record(definition, Definition.newDefinition(definition));
          myTypecheckedReporter.typecheckingFailed(definition);
        }
        return false;
      }

      myTypecheckingHeaders = true;
      Ordering ordering = new Ordering(myInstanceProvider, this, myState, true);
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
    } else {
      CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
      LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
      CheckTypeVisitor visitor = new CheckTypeVisitor(myState, myStaticNsProvider, myDynamicNsProvider, null, null, new ArrayList<Binding>(), new ArrayList<LevelBinding>(), localErrorReporter, null);
      Definition typechecked = DefinitionCheckType.typeCheckHeader(visitor, new GlobalInstancePool(myState, myInstanceProvider), unit.getDefinition(), unit.getEnclosingClass());
      if (typechecked.status() == Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING) {
        mySuspensions.put(unit.getDefinition(), new Suspension(visitor, countingErrorReporter));
      } else {
        if (countingErrorReporter.getErrorsNumber() == 0) {
          myTypecheckedReporter.typecheckingSucceeded(unit.getDefinition());
        } else {
          myTypecheckedReporter.typecheckingFailed(unit.getDefinition());
        }
      }
      return typechecked.status().headerIsOK();
    }
  }

  private void typecheckBodies(SCC scc, boolean headersAreOK) {
    GlobalInstancePool instancePool = new GlobalInstancePool(myState, myInstanceProvider);
    Set<FunctionDefinition> cycleDefs = new HashSet<>();
    List<Pair<Abstract.Definition, Boolean>> results = new ArrayList<>(scc.getUnits().size());
    for (TypecheckingUnit unit : scc.getUnits()) {
      if (unit.isHeader()) {
        continue;
      }

      CountingErrorReporter countingErrorReporter = null;
      boolean doReport = true;
      if (Typecheckable.hasHeader(unit.getDefinition())) {
        Suspension suspension = mySuspensions.remove(unit.getDefinition());
        if (suspension != null) {
          if (headersAreOK) {
            Definition def = myState.getTypechecked(unit.getDefinition());
            DefinitionCheckType.typeCheckBody(def, suspension.visitor);
            if (def instanceof FunctionDefinition) {
              cycleDefs.add((FunctionDefinition) def);
            }
            countingErrorReporter = suspension.countingErrorReporter;
          }
        } else {
          doReport = false;
        }
      } else {
        if (headersAreOK) {
          countingErrorReporter = new CountingErrorReporter();
          LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
          DefinitionCheckType.typeCheck(myState, instancePool, myStaticNsProvider, myDynamicNsProvider, unit, true, localErrorReporter);
        }
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
    Definition typechecked = DefinitionCheckType.typeCheck(myState, new GlobalInstancePool(myState, myInstanceProvider), myStaticNsProvider, myDynamicNsProvider, unit, recursive, localErrorReporter);

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
