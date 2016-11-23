package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.CycleError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.order.BaseOrdering;
import com.jetbrains.jetpad.vclang.typechecking.order.SCC;
import com.jetbrains.jetpad.vclang.typechecking.order.SCCListener;
import com.jetbrains.jetpad.vclang.typechecking.visitor.DefinitionCheckTypeVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Typechecking {
  private static void typecheck(SCC scc, TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter, boolean isPrelude) {
    if (scc.getUnits().size() > 1) {
      List<Abstract.Definition> cycle = new ArrayList<>(scc.getUnits().size());
      for (SCC.TypecheckingUnit unit : scc.getUnits()) {
        cycle.add(unit.definition);
      }
      errorReporter.report(new TypeCheckingError(cycle.get(0), new CycleError(cycle)));
      throw new SCCException();
    } else {
      SCC.TypecheckingUnit unit = scc.getUnits().iterator().next();
      CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
      DefinitionCheckTypeVisitor.typeCheck(state, staticNsProvider, dynamicNsProvider, unit.enclosingClass == null ? null : (ClassDefinition) state.getTypechecked(unit.enclosingClass), unit.definition, new ProxyErrorReporter(unit.definition, new CompositeErrorReporter(errorReporter, countingErrorReporter)), isPrelude);
      if (countingErrorReporter.getErrorsNumber() > 0) {
        typecheckedReporter.typecheckingFailed(unit.definition);
      } else {
        typecheckedReporter.typecheckingSucceeded(unit.definition);
      }
    }
  }

  public static void typecheckDefinitions(final TypecheckerState state, final StaticNamespaceProvider staticNsProvider, final DynamicNamespaceProvider dynamicNsProvider, final Collection<? extends Abstract.Definition> definitions, final ErrorReporter errorReporter, final TypecheckedReporter typecheckedReporter) {
    BaseOrdering ordering = new BaseOrdering(new SCCListener() {
      @Override
      public void sccFound(SCC scc) {
        typecheck(scc, state, staticNsProvider, dynamicNsProvider, errorReporter, typecheckedReporter, false);
      }
    });

    try {
      for (Abstract.Definition definition : definitions) {
        ordering.doOrder(definition);
      }
    } catch (SCCException ignored) { }
  }

  private static class OrderDefinitionVisitor implements AbstractDefinitionVisitor<Void, Void>, AbstractStatementVisitor<Void, Void> {
    public final BaseOrdering ordering;

    private OrderDefinitionVisitor(BaseOrdering ordering) {
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

  public static void typecheckModules(final TypecheckerState state, final StaticNamespaceProvider staticNsProvider, final DynamicNamespaceProvider dynamicNsProvider, final Collection<? extends Abstract.ClassDefinition> classDefs, final ErrorReporter errorReporter, final TypecheckedReporter typecheckedReporter, final boolean isPrelude) {
    final BaseOrdering ordering = new BaseOrdering(new SCCListener() {
      @Override
      public void sccFound(SCC scc) {
        typecheck(scc, state, staticNsProvider, dynamicNsProvider, errorReporter, typecheckedReporter, isPrelude);
      }
    });

    try {
      for (Abstract.ClassDefinition classDef : classDefs) {
        new OrderDefinitionVisitor(ordering).orderDefinition(classDef);
      }
    } catch (SCCException ignored) { }
  }

  private static class SCCException extends RuntimeException { }
}
