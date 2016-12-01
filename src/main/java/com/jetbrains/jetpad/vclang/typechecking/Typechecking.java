package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.CycleError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.order.BaseOrdering;
import com.jetbrains.jetpad.vclang.typechecking.order.SCC;
import com.jetbrains.jetpad.vclang.typechecking.order.SCCListener;
import com.jetbrains.jetpad.vclang.typechecking.visitor.DefinitionCheckTypeVisitor;

import java.util.*;

public class Typechecking {
  private static void typecheck(Map<Abstract.Definition, CheckTypeVisitor> suspension, SCC scc, TypecheckerState state, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, ErrorReporter errorReporter, TypecheckedReporter typecheckedReporter) {
    if (scc.getUnits().size() > 1) {
      List<Abstract.Definition> cycle = new ArrayList<>(scc.getUnits().size());
      for (TypecheckingUnit unit : scc.getUnits()) {
        cycle.add(unit.getDefinition());
      }
      errorReporter.report(new TypeCheckingError(cycle.get(0), new CycleError(cycle)));
      throw new SCCException();
    } else {
      TypecheckingUnit unit = scc.getUnits().iterator().next();
      if (unit.getDefinition() instanceof Abstract.FunctionDefinition && unit.isHeader()) {
        return;
      }

      CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
      LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition(), new CompositeErrorReporter(errorReporter, countingErrorReporter));
      if (unit.getDefinition() instanceof Abstract.DataDefinition) {
        if (unit.isHeader()) {
          CheckTypeVisitor visitor = DefinitionCheckTypeVisitor.typeCheckHeader(state, staticNsProvider, dynamicNsProvider, unit.getDefinition(), unit.getEnclosingClass(), localErrorReporter);
          if (visitor != null) {
            suspension.put(unit.getDefinition(), visitor);
          }
        } else {
          CheckTypeVisitor visitor = suspension.get(unit.getDefinition());
          if (visitor != null) {
            DefinitionCheckTypeVisitor.typeCheckBody(state.getTypechecked(unit.getDefinition()), visitor);
            suspension.remove(unit.getDefinition());
          }
        }
      } else {
        DefinitionCheckTypeVisitor.typeCheck(state, staticNsProvider, dynamicNsProvider, unit, localErrorReporter);
      }

      if (!unit.isHeader()) {
        if (countingErrorReporter.getErrorsNumber() > 0) {
          typecheckedReporter.typecheckingFailed(unit.getDefinition());
        } else {
          typecheckedReporter.typecheckingSucceeded(unit.getDefinition());
        }
      }
    }
  }

  public static void typecheckDefinitions(final TypecheckerState state, final StaticNamespaceProvider staticNsProvider, final DynamicNamespaceProvider dynamicNsProvider, final Collection<? extends Abstract.Definition> definitions, final ErrorReporter errorReporter, final TypecheckedReporter typecheckedReporter) {
    final Map<Abstract.Definition, CheckTypeVisitor> suspension = new HashMap<>();
    BaseOrdering ordering = new BaseOrdering(new SCCListener() {
      @Override
      public void sccFound(SCC scc) {
        typecheck(suspension, scc, state, staticNsProvider, dynamicNsProvider, errorReporter, typecheckedReporter);
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

  public static void typecheckModules(final TypecheckerState state, final StaticNamespaceProvider staticNsProvider, final DynamicNamespaceProvider dynamicNsProvider, final Collection<? extends Abstract.ClassDefinition> classDefs, final ErrorReporter errorReporter, final TypecheckedReporter typecheckedReporter) {
    final Map<Abstract.Definition, CheckTypeVisitor> suspension = new HashMap<>();
    final BaseOrdering ordering = new BaseOrdering(new SCCListener() {
      @Override
      public void sccFound(SCC scc) {
        typecheck(suspension, scc, state, staticNsProvider, dynamicNsProvider, errorReporter, typecheckedReporter);
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
