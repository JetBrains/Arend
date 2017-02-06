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
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.BaseAbstractVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.CycleError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TerminationCheckError;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.order.SCC;
import com.jetbrains.jetpad.vclang.typechecking.termination.DefinitionCallGraph;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.ClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.DefinitionResolveInstanceVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.SimpleClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.*;

public class Typechecking {
  private final TypecheckerState myState;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final ErrorReporter myErrorReporter;
  private final TypecheckedReporter myTypecheckedReporter;
  private final DependencyListener myDependencyListener;
  private final Map<Abstract.Definition, Suspension> mySuspensions = new HashMap<>();

  public Typechecking(TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, DependencyListener dependencyListener) {
    myState = state;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myErrorReporter = errorReporter;
    myTypecheckedReporter = typecheckedReporter;
    myDependencyListener = dependencyListener;
  }

  private static class Suspension {
    public CheckTypeVisitor visitor;
    public CountingErrorReporter countingErrorReporter;

    public Suspension(CheckTypeVisitor visitor, CountingErrorReporter countingErrorReporter) {
      this.visitor = visitor;
      this.countingErrorReporter = countingErrorReporter;
    }
  }

  private void typecheck(SCC scc, GlobalInstancePool instancePool) {
    if (scc.getUnits().size() > 1) {
      for (TypecheckingUnit unit : scc.getUnits()) {
        if (unit.isHeader() || !(unit.getDefinition() instanceof Abstract.FunctionDefinition)) {
          throw new Ordering.SCCException(scc);
        }
      }
    }

    Set<Definition> cycleDefs = new HashSet<>();
    for (TypecheckingUnit unit : scc.getUnits()) {
      CountingErrorReporter countingErrorReporter = null;
      boolean doReport;

      if (Typecheckable.hasHeader(unit.getDefinition())) {
        if (unit.isHeader()) {
          if (myState.getTypechecked(unit.getDefinition()) == null) {
            countingErrorReporter = new CountingErrorReporter();
            LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
            CheckTypeVisitor visitor = new CheckTypeVisitor(myState, myStaticNsProvider, myDynamicNsProvider, null, null, new ArrayList<Binding>(), new ArrayList<LevelBinding>(), localErrorReporter, null);
            Definition typechecked = DefinitionCheckType.typeCheckHeader(visitor, instancePool, unit.getDefinition(), unit.getEnclosingClass());
            if (typechecked.hasErrors() == Definition.TypeCheckingStatus.TYPE_CHECKING) {
              mySuspensions.put(unit.getDefinition(), new Suspension(visitor, countingErrorReporter));
              doReport = false;
            } else {
              doReport = true;
            }
          } else {
            doReport = true;
          }
        } else {
          Suspension suspension = mySuspensions.get(unit.getDefinition());
          if (suspension != null) {
            Definition def = myState.getTypechecked(unit.getDefinition());
            DefinitionCheckType.typeCheckBody(def, suspension.visitor);
            if (def instanceof FunctionDefinition) {
              cycleDefs.add(def);
            }
            countingErrorReporter = suspension.countingErrorReporter;
            mySuspensions.remove(unit.getDefinition());
            doReport = true;
          } else {
            doReport = false;
          }
        }
      } else {
        doReport = true;
        if (myState.getTypechecked(unit.getDefinition()) == null) {
          countingErrorReporter = new CountingErrorReporter();
          LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
          DefinitionCheckType.typeCheck(myState, instancePool, myStaticNsProvider, myDynamicNsProvider, unit, localErrorReporter);
        }
      }

      if (doReport) {
        if (countingErrorReporter == null || countingErrorReporter.getErrorsNumber() == 0) {
          myTypecheckedReporter.typecheckingSucceeded(unit.getDefinition());
        } else {
          myTypecheckedReporter.typecheckingFailed(unit.getDefinition());
        }
      }
    }

    if (!cycleDefs.isEmpty()) {
      DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
      for (Definition fDef : cycleDefs) definitionCallGraph.add(fDef, cycleDefs);
      DefinitionCallGraph callCategory = new DefinitionCallGraph(definitionCallGraph);
      if (!callCategory.checkTermination()) {
        for (Definition fDef : cycleDefs) {
          fDef.hasErrors(Definition.TypeCheckingStatus.HAS_ERRORS);
        }
        for (Definition d : callCategory.myErrorInfo.keySet()) {
          myErrorReporter.report(new TerminationCheckError(d, callCategory.myErrorInfo.get(d)));
        }
      }
    }
  }

  private void reportCycleError(SCC scc) {
    List<Abstract.Definition> cycle = new ArrayList<>(scc.getUnits().size());
    for (TypecheckingUnit unit : scc.getUnits()) {
      cycle.add(unit.getDefinition());
    }
    myErrorReporter.report(new TypeCheckingError(cycle.get(0), new CycleError(cycle)));
  }

  private void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions, ClassViewInstanceProvider instanceProvider) {
    Ordering ordering = new Ordering(instanceProvider, new TypecheckingDependencyListener(instanceProvider));
    try {
      for (Abstract.Definition definition : definitions) {
        ordering.doOrder(definition);
      }
    } catch (Ordering.SCCException e) {
      reportCycleError(e.scc);
    }
  }

  private Scope getDefinitionScope(Abstract.Definition definition) {
    if (definition == null) {
      return new EmptyScope();
    }

    return definition.accept(new BaseAbstractVisitor<Scope, Scope>() {
      @Override
      public Scope visitFunction(Abstract.FunctionDefinition def, Scope parentScope) {
        return new FunctionScope(parentScope, myStaticNsProvider.forDefinition(def));
      }

      @Override
      public Scope visitData(Abstract.DataDefinition def, Scope parentScope) {
        return new DataScope(parentScope, myStaticNsProvider.forDefinition(def));
      }

      @Override
      public Scope visitClass(Abstract.ClassDefinition def, Scope parentScope) {
        return new StaticClassScope(parentScope, myStaticNsProvider.forDefinition(def));
      }
    }, getDefinitionScope(definition.getParent()));
  }

  public void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    for (Abstract.Definition definition : definitions) {
      definition.accept(new DefinitionResolveInstanceVisitor(myStaticNsProvider, instanceProvider, myErrorReporter), getDefinitionScope(definition));
    }
    typecheckDefinitions(definitions, instanceProvider);
  }

  public void typecheckDefinitions(final Collection<? extends Abstract.Definition> definitions, Scope scope) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    DefinitionResolveInstanceVisitor visitor = new DefinitionResolveInstanceVisitor(myStaticNsProvider, instanceProvider, myErrorReporter);
    for (Abstract.Definition definition : definitions) {
      definition.accept(visitor, scope);
    }
    typecheckDefinitions(definitions, instanceProvider);
  }

  public void typecheckModules(final Collection<? extends Abstract.ClassDefinition> classDefs) {
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider();
    for (Abstract.ClassDefinition classDef : classDefs) {
      classDef.accept(new DefinitionResolveInstanceVisitor(myStaticNsProvider, instanceProvider, myErrorReporter), new EmptyScope());
    }

    Ordering ordering = new Ordering(instanceProvider, new TypecheckingDependencyListener(instanceProvider));
    try {
      for (Abstract.ClassDefinition classDef : classDefs) {
        new OrderDefinitionVisitor(ordering).orderDefinition(classDef);
      }
    } catch (Ordering.SCCException e) {
      reportCycleError(e.scc);
    }
  }

  private class TypecheckingDependencyListener implements DependencyListener {
    private final GlobalInstancePool myInstancePool;

    private TypecheckingDependencyListener(ClassViewInstanceProvider instanceProvider) {
      myInstancePool = new GlobalInstancePool(instanceProvider);
    }

    @Override
    public void sccFound(SCC scc) {
      typecheck(scc, myInstancePool);
      myDependencyListener.sccFound(scc);
    }

    @Override
    public void dependsOn(Typecheckable unit, Abstract.Definition def) {
      myDependencyListener.dependsOn(unit, def);
    }
  }

  private static class OrderDefinitionVisitor extends BaseAbstractVisitor<Void, Void> {
    public final Ordering ordering;

    private OrderDefinitionVisitor(Ordering ordering) {
      this.ordering = ordering;
    }

    @Override
    public Void visitFunction(Abstract.FunctionDefinition def, Void params) {
      for (Abstract.Statement statement : def.getStatements()) {
        statement.accept(this, null);
      }
      return null;
    }

    @Override
    public Void visitClass(Abstract.ClassDefinition def, Void params) {
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        statement.accept(this, null);
      }
      for (Abstract.Definition definition : def.getInstanceDefinitions()) {
        orderDefinition(definition);
      }
      return null;
    }

    @Override
    public Void visitDefine(Abstract.DefineStatement stat, Void params) {
      orderDefinition(stat.getDefinition());
      return null;
    }

    public void orderDefinition(Abstract.Definition definition) {
      ordering.doOrder(definition);
      definition.accept(this, null);
    }
  }
}
