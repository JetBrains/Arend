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
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractStatementVisitor;
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
import com.jetbrains.jetpad.vclang.typechecking.typeclass.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.*;

public class Typechecking {
  private static class Suspension {
    public CheckTypeVisitor visitor;
    public CountingErrorReporter countingErrorReporter;

    public Suspension(CheckTypeVisitor visitor, CountingErrorReporter countingErrorReporter) {
      this.visitor = visitor;
      this.countingErrorReporter = countingErrorReporter;
    }
  }

  private static void typecheck(Map<Abstract.Definition, Suspension> suspensions, SCC scc, TypecheckerState state, GlobalInstancePool instancePool, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter) {
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
          if (state.getTypechecked(unit.getDefinition()) == null) {
            countingErrorReporter = new CountingErrorReporter();
            LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(errorReporter, countingErrorReporter));
            CheckTypeVisitor visitor = new CheckTypeVisitor(state, staticNsProvider, dynamicNsProvider, null, null, new ArrayList<Binding>(), new ArrayList<LevelBinding>(), localErrorReporter, null);
            Definition typechecked = DefinitionCheckType.typeCheckHeader(visitor, instancePool, unit.getDefinition(), unit.getEnclosingClass());
            if (typechecked.hasErrors() == Definition.TypeCheckingStatus.TYPE_CHECKING) {
              suspensions.put(unit.getDefinition(), new Suspension(visitor, countingErrorReporter));
              doReport = false;
            } else {
              doReport = true;
            }
          } else {
            doReport = true;
          }
        } else {
          Suspension suspension = suspensions.get(unit.getDefinition());
          if (suspension != null) {
            Definition def = state.getTypechecked(unit.getDefinition());
            DefinitionCheckType.typeCheckBody(def, suspension.visitor);
            if (def instanceof FunctionDefinition) {
              cycleDefs.add(def);
            }
            countingErrorReporter = suspension.countingErrorReporter;
            suspensions.remove(unit.getDefinition());
            doReport = true;
          } else {
            doReport = false;
          }
        }
      } else {
        doReport = true;
        if (state.getTypechecked(unit.getDefinition()) == null) {
          countingErrorReporter = new CountingErrorReporter();
          LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(errorReporter, countingErrorReporter));
          DefinitionCheckType.typeCheck(state, instancePool, staticNsProvider, dynamicNsProvider, unit, localErrorReporter);
        }
      }

      if (doReport) {
        if (countingErrorReporter == null || countingErrorReporter.getErrorsNumber() == 0) {
          typecheckedReporter.typecheckingSucceeded(unit.getDefinition());
        } else {
          typecheckedReporter.typecheckingFailed(unit.getDefinition());
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
        for (Definition d : callCategory.myErrorInfo.keySet())
           errorReporter.report(new TerminationCheckError(d, callCategory.myErrorInfo.get(d)));
      }
    }
  }

  public static void typecheckDefinitions(final TypecheckerState state, final StaticNamespaceProvider staticNsProvider, final DynamicNamespaceProvider dynamicNsProvider, ClassViewInstanceProvider instanceProvider, final Collection<? extends Abstract.Definition> definitions, final ErrorReporter errorReporter, final TypecheckedReporter typecheckedReporter, final DependencyListener dependencyListener) {
    final GlobalInstancePool instancePool = new GlobalInstancePool();
    final Map<Abstract.Definition, Suspension> suspensions = new HashMap<>();
    Ordering ordering = new Ordering(instanceProvider, new DependencyListener() {
      @Override
      public void sccFound(SCC scc) {
        typecheck(suspensions, scc, state, instancePool, staticNsProvider, dynamicNsProvider, errorReporter, typecheckedReporter);
        dependencyListener.sccFound(scc);
      }

      @Override
      public void dependsOn(Typecheckable unit, Abstract.Definition def) {
        dependencyListener.dependsOn(unit, def);
      }
    });

    try {
      for (Abstract.Definition definition : definitions) {
        ordering.doOrder(definition);
      }
    } catch (Ordering.SCCException ignored) { }
  }

  private static class OrderDefinitionVisitor implements AbstractDefinitionVisitor<Void, Void>, AbstractStatementVisitor<Void, Void> {
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
    public Void visitClassField(Abstract.ClassField def, Void params) {
      return null;
    }

    @Override
    public Void visitData(Abstract.DataDefinition def, Void params) {
      return null;
    }

    @Override
    public Void visitConstructor(Abstract.Constructor def, Void params) {
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
    public Void visitImplement(Abstract.Implementation def, Void params) {
      return null;
    }

    @Override
    public Void visitClassView(Abstract.ClassView def, Void params) {
      return null;
    }

    @Override
    public Void visitClassViewField(Abstract.ClassViewField def, Void params) {
      return null;
    }

    @Override
    public Void visitClassViewInstance(Abstract.ClassViewInstance def, Void params) {
      return null;
    }

    @Override
    public Void visitDefine(Abstract.DefineStatement stat, Void params) {
      orderDefinition(stat.getDefinition());
      return null;
    }

    @Override
    public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
      return null;
    }

    public void orderDefinition(Abstract.Definition definition) {
      ordering.doOrder(definition);
      definition.accept(this, null);
    }
  }

  public static void typecheckModules(final TypecheckerState state, final StaticNamespaceProvider staticNsProvider, final DynamicNamespaceProvider dynamicNsProvider, ClassViewInstanceProvider instanceProvider, final Collection<? extends Abstract.ClassDefinition> classDefs, final ErrorReporter errorReporter, final TypecheckedReporter typecheckedReporter, final DependencyListener dependencyListener) {
    final GlobalInstancePool instancePool = new GlobalInstancePool();
    final Map<Abstract.Definition, Suspension> suspensions = new HashMap<>();
    final Ordering ordering = new Ordering(instanceProvider, new DependencyListener() {
      @Override
      public void sccFound(SCC scc) {
        typecheck(suspensions, scc, state, instancePool, staticNsProvider, dynamicNsProvider, errorReporter, typecheckedReporter);
        dependencyListener.sccFound(scc);
      }

      @Override
      public void dependsOn(Typecheckable unit, Abstract.Definition def) {
        dependencyListener.dependsOn(unit, def);
      }
    });

    try {
      for (Abstract.ClassDefinition classDef : classDefs) {
        new OrderDefinitionVisitor(ordering).orderDefinition(classDef);
      }
    } catch (Ordering.SCCException e) {
      List<Abstract.Definition> cycle = new ArrayList<>(e.scc.getUnits().size());
      for (TypecheckingUnit unit : e.scc.getUnits()) {
        cycle.add(unit.getDefinition());
      }
      errorReporter.report(new TypeCheckingError(cycle.get(0), new CycleError(cycle)));
    }
  }
}
