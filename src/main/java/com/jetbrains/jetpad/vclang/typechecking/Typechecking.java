package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.Clause;
import com.jetbrains.jetpad.vclang.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.reference.converter.IdReferableConverter;
import com.jetbrains.jetpad.vclang.naming.reference.converter.ReferableConverter;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.error.CycleError;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TerminationCheckError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.DummyDependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.order.Ordering;
import com.jetbrains.jetpad.vclang.typechecking.order.SCC;
import com.jetbrains.jetpad.vclang.typechecking.termination.DefinitionCallGraph;
import com.jetbrains.jetpad.vclang.typechecking.termination.RecursiveBehavior;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.TypecheckingUnit;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.pool.GlobalInstancePool;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProviderSet;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.visitor.DesugarVisitor;
import com.jetbrains.jetpad.vclang.util.ComputationInterruptedException;

import java.util.*;

public class Typechecking implements DependencyListener {
  private final TypecheckerState myState;
  private final DependencyListener myDependencyListener;
  private final Map<GlobalReferable, CheckTypeVisitor> mySuspensions = new HashMap<>();
  private final ErrorReporter myErrorReporter;
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private final ReferableConverter myReferableConverter;
  private boolean myTypecheckingHeaders = false;

  public static CancellationIndicator CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;

  public static void setDefaultCancellationIndicator() {
    CANCELLATION_INDICATOR = ThreadCancellationIndicator.INSTANCE;
  }

  public Typechecking(TypecheckerState state, ConcreteProvider concreteProvider, ErrorReporter errorReporter, DependencyListener dependencyListener, ReferableConverter referableConverter) {
    myState = state;
    myErrorReporter = errorReporter;
    myDependencyListener = dependencyListener;
    myInstanceProviderSet = new InstanceProviderSet();
    myConcreteProvider = concreteProvider;
    myReferableConverter = referableConverter;
  }

  public Typechecking(TypecheckerState state, ConcreteProvider concreteProvider, ErrorReporter errorReporter) {
    this(state, concreteProvider, errorReporter, DummyDependencyListener.INSTANCE, IdReferableConverter.INSTANCE);
  }

  public boolean typecheckDefinitions(final Collection<? extends Concrete.Definition> definitions) {
    Ordering ordering = new Ordering(myInstanceProviderSet, myConcreteProvider, this, false);

    try {
      for (Concrete.Definition definition : definitions) {
        ordering.doOrder(definition);
      }
    } catch (ComputationInterruptedException ignored) {
      return false;
    }

    return true;
  }

  public boolean typecheckModules(final Collection<? extends Group> modules) {
    /* TODO[classes]
    InstanceNamespaceProvider instanceNamespaceProvider = new InstanceNamespaceProvider(myErrorReporter);
    NameResolver nameResolver = new NameResolver(new NamespaceProviders(null, myStaticNsProvider, myDynamicNsProvider));
    GroupResolver resolver = new GroupInstanceResolver(nameResolver, myErrorReporter, myInstanceProviderSet);
    Scope emptyScope = EmptyScope.INSTANCE;
    for (Group group : modules) {
      resolver.resolveGroup(group, emptyScope);
    }
    */
    Ordering ordering = new Ordering(myInstanceProviderSet, myConcreteProvider, this, false);

    try {
      for (Group group : modules) {
        orderGroup(group, ordering);
      }
    } catch (ComputationInterruptedException ignored) {
      return false;
    }

    return true;
  }

  public enum Recursion { NO, IN_HEADER, IN_BODY }

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

  private void orderGroup(Group group, Ordering ordering) {
    LocatedReferable referable = group.getReferable();
    TCReferable tcReferable = myReferableConverter.toDataLocatedReferable(referable);
    Definition typechecked = tcReferable == null ? null : getTypechecked(tcReferable);
    if (typechecked == null) {
      Concrete.ReferableDefinition def = myConcreteProvider.getConcrete(referable);
      if (def instanceof Concrete.Definition) {
        ordering.doOrder((Concrete.Definition) def);
      }
    } else {
      typecheckingUnitStarted(tcReferable);
      typecheckingUnitFinished(tcReferable, typechecked);
    }

    for (Group subgroup : group.getSubgroups()) {
      orderGroup(subgroup, ordering);
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      orderGroup(subgroup, ordering);
    }
  }

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

  public final Definition getTypechecked(TCReferable definition) {
    Definition typechecked = myState.getTypechecked(definition);
    if (typechecked == null || typechecked.status().needsTypeChecking()) {
      return null;
    } else {
      return typechecked;
    }
  }

  @Override
  public void dependsOn(TCReferable def1, boolean header, TCReferable def2) {
    myDependencyListener.dependsOn(def1, header, def2);
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
      LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition().getData(), new CompositeErrorReporter(myErrorReporter, countingErrorReporter));
      CheckTypeVisitor visitor = new CheckTypeVisitor(myState, new LinkedHashMap<>(), localErrorReporter, null);
      if (unit.getDefinition().hasErrors()) {
        visitor.setHasErrors();
      }
      unit.getDefinition().accept(new DesugarVisitor(myConcreteProvider, myErrorReporter), null);
      Definition typechecked = DefinitionTypechecking.typecheckHeader(visitor, new GlobalInstancePool(myState, myInstanceProviderSet.getInstanceProvider(unit.getDefinition().getData())), unit.getDefinition());
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
    Ordering ordering = new Ordering(myInstanceProviderSet, myConcreteProvider, this, true);
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

    for (Concrete.Definition definition : definitions) {
      typecheckingBodyStarted(definition.getData());

      Definition def = myState.getTypechecked(definition.getData());
      CheckTypeVisitor visitor = mySuspensions.remove(definition.getData());
      if (headersAreOK && visitor != null) {
        List<Clause> clauses = DefinitionTypechecking.typecheckBody(def, definition, visitor, dataDefinitions);
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

    LocalErrorReporter localErrorReporter = new ProxyErrorReporter(unit.getDefinition().getData(), myErrorReporter);
    unit.getDefinition().accept(new DesugarVisitor(myConcreteProvider, myErrorReporter), null);
    List<Clause> clauses = DefinitionTypechecking.typecheck(myState, new GlobalInstancePool(myState, myInstanceProviderSet.getInstanceProvider(unit.getDefinition().getData())), unit, recursive, localErrorReporter);
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
