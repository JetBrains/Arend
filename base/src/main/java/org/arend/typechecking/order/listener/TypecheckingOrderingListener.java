package org.arend.typechecking.order.listener;

import org.arend.core.context.binding.PersistentEvaluatingBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.ElimClause;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Sort;
import org.arend.error.CountingErrorReporter;
import org.arend.ext.ArendExtension;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.library.Library;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.ext.concrete.definition.FunctionKind;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.DefinableMetaDefinition;
import org.arend.term.concrete.ReplaceDataVisitor;
import org.arend.term.group.Group;
import org.arend.typechecking.*;
import org.arend.typechecking.computation.BooleanComputationRunner;
import org.arend.typechecking.computation.CancellationIndicator;
import org.arend.typechecking.error.CycleError;
import org.arend.typechecking.error.TerminationCheckError;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.Ordering;
import org.arend.typechecking.order.PartialComparator;
import org.arend.typechecking.order.dependency.DependencyListener;
import org.arend.typechecking.order.dependency.DummyDependencyListener;
import org.arend.typechecking.patternmatching.ExtElimClause;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.typechecking.termination.DefinitionCallGraph;
import org.arend.typechecking.termination.RecursiveBehavior;
import org.arend.typechecking.visitor.*;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TypecheckingOrderingListener extends BooleanComputationRunner implements OrderingListener {
  private final DependencyListener myDependencyListener;
  private final Map<TCDefReferable, Suspension> mySuspensions = new HashMap<>();
  private final ErrorReporter myErrorReporter;
  private final InstanceProviderSet myInstanceProviderSet;
  private final ConcreteProvider myConcreteProvider;
  private final ReferableConverter myReferableConverter;
  private final PartialComparator<TCDefReferable> myComparator;
  private final ArendExtensionProvider myExtensionProvider;
  private final Map<TCDefReferable, Concrete.ResolvableDefinition> myDesugaredDefinitions = new HashMap<>();
  private List<TCDefReferable> myCurrentDefinitions = Collections.emptyList();
  private boolean myHeadersAreOK = true;

  private record Suspension(CheckTypeVisitor typechecker, boolean isNew, UniverseKind universeKind) {}

  public TypecheckingOrderingListener(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, ReferableConverter referableConverter, ErrorReporter errorReporter, DependencyListener dependencyListener, PartialComparator<TCDefReferable> comparator, ArendExtensionProvider extensionProvider) {
    myErrorReporter = errorReporter;
    myDependencyListener = dependencyListener;
    myInstanceProviderSet = instanceProviderSet;
    myConcreteProvider = concreteProvider;
    myReferableConverter = referableConverter;
    myComparator = comparator;
    myExtensionProvider = extensionProvider;
  }

  public TypecheckingOrderingListener(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, ReferableConverter referableConverter, ErrorReporter errorReporter, PartialComparator<TCDefReferable> comparator, ArendExtensionProvider extensionProvider) {
    this(instanceProviderSet, concreteProvider, referableConverter, errorReporter, DummyDependencyListener.INSTANCE, comparator, extensionProvider);
  }

  public ConcreteProvider getConcreteProvider() {
    return myConcreteProvider;
  }

  public InstanceProviderSet getInstanceProviderSet() {
    return myInstanceProviderSet;
  }

  public ReferableConverter getReferableConverter() {
    return myReferableConverter;
  }

  @Override
  protected Boolean computationInterrupted() {
    for (TCDefReferable currentDefinition : myCurrentDefinitions) {
      Definition typechecked = currentDefinition.getTypechecked();
      currentDefinition.setTypechecked(null);
      typecheckingInterrupted(currentDefinition, typechecked);
    }
    myCurrentDefinitions = Collections.emptyList();
    return false;
  }

  public boolean typecheckDefinitions(final Collection<? extends Concrete.ResolvableDefinition> definitions, CancellationIndicator cancellationIndicator, boolean withInstances) {
    return run(cancellationIndicator, () -> {
      Ordering ordering = new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myComparator, withInstances);
      for (Concrete.ResolvableDefinition definition : definitions) {
        ordering.order(definition);
      }
      return true;
    });
  }

  public boolean typecheckDefinitions(final Collection<? extends Concrete.ResolvableDefinition> definitions, CancellationIndicator cancellationIndicator) {
    return typecheckDefinitions(definitions, cancellationIndicator, true);
  }

  public boolean typecheckModules(final Collection<? extends Group> modules, CancellationIndicator cancellationIndicator) {
    return run(cancellationIndicator, () -> {
      new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myComparator).orderModules(modules);
      return true;
    });
  }

  public boolean typecheckLibrary(Library library, CancellationIndicator cancellationIndicator) {
    return run(cancellationIndicator, () -> library.orderModules(new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myComparator)));
  }

  public boolean typecheckLibrary(Library library) {
    return typecheckLibrary(library, null);
  }

  public boolean typecheckTests(Library library, CancellationIndicator cancellationIndicator) {
    return run(cancellationIndicator, () -> library.orderTestModules(new Ordering(myInstanceProviderSet, myConcreteProvider, this, myDependencyListener, myReferableConverter, myComparator)));
  }

  public boolean typecheckCollected(CollectingOrderingListener collector, CancellationIndicator cancellationIndicator) {
    if (collector.isEmpty()) return true;
    return run(cancellationIndicator, () -> {
      collector.feed(this);
      return true;
    });
  }

  public void typecheckingHeaderStarted(TCDefReferable definition) {

  }

  public void typecheckingBodyStarted(TCDefReferable definition) {

  }

  public void typecheckingUnitStarted(TCDefReferable definition) {

  }

  public void typecheckingHeaderFinished(TCDefReferable referable, Definition definition) {

  }

  public void typecheckingBodyFinished(TCDefReferable referable, Definition definition) {

  }

  public void typecheckingUnitFinished(TCDefReferable referable, Definition definition) {

  }

  public void typecheckingInterrupted(TCDefReferable definition, @Nullable Definition typechecked) {

  }

  private Definition newDefinition(Concrete.ResolvableDefinition definition) {
    Definition typechecked;
    if (definition instanceof Concrete.DataDefinition def) {
      typechecked = new DataDefinition(def.getData());
      for (Concrete.ConstructorClause constructorClause : ((Concrete.DataDefinition) definition).getConstructorClauses()) {
        for (Concrete.Constructor constructor : constructorClause.getConstructors()) {
          Constructor tcConstructor = new Constructor(constructor.getData(), (DataDefinition) typechecked);
          tcConstructor.setParameters(EmptyDependentLink.getInstance());
          tcConstructor.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          ((DataDefinition) typechecked).addConstructor(tcConstructor);
          constructor.getData().setTypecheckedIfAbsent(tcConstructor);
        }
      }
    } else if (definition instanceof Concrete.BaseFunctionDefinition def) {
      typechecked = def.getKind() == FunctionKind.CONS ? new DConstructor(def.getData()) : new FunctionDefinition(def.getData());
      ((FunctionDefinition) typechecked).setResultType(new ErrorExpression());
    } else if (definition instanceof Concrete.ClassDefinition def) {
      typechecked = new ClassDefinition(def.getData());
      for (Concrete.ClassElement element : ((Concrete.ClassDefinition) definition).getElements()) {
        if (element instanceof Concrete.ClassField) {
          ClassField classField = new ClassField(((Concrete.ClassField) element).getData(), (ClassDefinition) typechecked, new PiExpression(Sort.PROP, new TypedSingleDependentLink(false, "this", new ClassCallExpression((ClassDefinition) typechecked, typechecked.makeIdLevels()), true), new ErrorExpression()), null);
          classField.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          ((ClassDefinition) typechecked).addPersonalField(classField);
          ((ClassDefinition) typechecked).addField(classField);
          classField.getReferable().setTypecheckedIfAbsent(classField);
        }
      }
    } else if (definition instanceof DefinableMetaDefinition) {
      typechecked = new MetaTopDefinition(((DefinableMetaDefinition) definition).getData());
    } else {
      throw new IllegalStateException();
    }
    typechecked.setStatus(Definition.TypeCheckingStatus.NEEDS_TYPE_CHECKING);
    definition.getData().setTypecheckedIfAbsent(typechecked);
    return typechecked;
  }

  @Override
  public void unitFound(Concrete.ResolvableDefinition definition, boolean recursive) {
    myHeadersAreOK = true;

    if (recursive) {
      Set<TCReferable> dependencies = new HashSet<>();
      definition.accept(new CollectDefCallsVisitor(dependencies, false), null);
      if (dependencies.contains(definition.getData())) {
        typecheckingUnitStarted(definition.getData());
        myErrorReporter.report(new CycleError(Collections.singletonList(definition.getData())));
        typecheckingUnitFinished(definition.getData(), newDefinition(definition));
        return;
      }
      if (definition instanceof Concrete.Definition def) {
        def.setRecursiveDefinitions(Collections.singleton(def.getData()));
      }
    }

    List<ExtElimClause> clauses;
    ArendExtension extension = myExtensionProvider.getArendExtension(definition.getData());
    CheckTypeVisitor checkTypeVisitor = new CheckTypeVisitor(new LocalErrorReporter(definition.getData(), myErrorReporter), null, extension);
    checkTypeVisitor.setInstancePool(new GlobalInstancePool(myInstanceProviderSet.get(definition.getData()), checkTypeVisitor));
    definition = definition.accept(new ReplaceDataVisitor(), null);
    if (definition instanceof Concrete.FunctionDefinition funDef && funDef.getKind().isUse()) {
      myDesugaredDefinitions.put(funDef.getData(), funDef);
    }
    if (definition instanceof Concrete.Definition) {
      WhereVarsFixVisitor.fixDefinition(Collections.singletonList((Concrete.Definition) definition), myErrorReporter);
    }
    DesugarVisitor.desugar(definition, checkTypeVisitor.getErrorReporter());
    myCurrentDefinitions = Collections.singletonList(definition.getData());
    typecheckingUnitStarted(definition.getData());
    DefinitionTypechecker typechecker = new DefinitionTypechecker(checkTypeVisitor, recursive ? Collections.singleton(definition.getData()) : Collections.emptySet());
    clauses = definition.accept(typechecker, null);
    Definition typechecked = definition.getData().getTypechecked();
    if (typechecked == null) {
      typechecked = newDefinition(definition);
    }
    if (!(typechecked instanceof TopLevelDefinition || typechecked instanceof MetaTopDefinition)) {
      throw new IllegalStateException();
    }

    setParametersOriginalDefinitionsDependency(typechecked);
    if (typechecker.isNew()) {
      if (!(definition instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) definition).getKind().isCoclause()) && typechecked instanceof TopLevelDefinition) {
        FixLevelParameters.fix(Collections.singleton((TopLevelDefinition) typechecked), Collections.singleton(typechecked));
      }
      if (recursive && typechecked instanceof FunctionDefinition) {
        ((FunctionDefinition) typechecked).setRecursiveDefinitions(Collections.singleton((FunctionDefinition) typechecked));
      }
      if (recursive && typechecked instanceof DataDefinition) {
        ((DataDefinition) typechecked).setRecursiveDefinitions(Collections.singleton((DataDefinition) typechecked));
      }
      findAxiomsAndGoals(Collections.singletonList(definition), Collections.singleton(typechecked));
    }
    if (definition instanceof Concrete.Definition def && def.isRecursive() && typechecked instanceof FunctionDefinition) {
      checkRecursiveFunctions(Collections.singletonMap((FunctionDefinition) typechecked, def), clauses == null ? Collections.emptyMap() : Collections.singletonMap((FunctionDefinition) typechecked, clauses));
    }

    typecheckingUnitFinished(definition.getData(), typechecked);

    if (extension != null) {
      DefinitionListener listener = extension.getDefinitionListener();
      if (listener != null) {
        listener.typechecked(typechecked);
      }
    }

    myCurrentDefinitions = Collections.emptyList();
  }

  private void setParametersOriginalDefinitionsDependency(Definition definition) {
    for (Pair<TCDefReferable, Integer> pair : definition.getParametersOriginalDefinitions()) {
      myDependencyListener.dependsOn(definition.getRef(), pair.proj1);
    }
  }

  @Override
  public void cycleFound(List<Concrete.ResolvableDefinition> definitions) {
    List<TCReferable> cycle = new ArrayList<>();
    for (Concrete.ResolvableDefinition definition : definitions) {
      if (cycle.isEmpty() || cycle.get(cycle.size() - 1) != definition.getData()) {
        cycle.add(definition.getData());
      }

      if (definition instanceof Concrete.Definition def) {
        Definition typechecked = def.getData().getTypechecked();
        if (typechecked == null) {
          typechecked = newDefinition(def);
        }
        typechecked.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        typecheckingUnitStarted(def.getData());
        mySuspensions.remove(def.getData());
        typecheckingUnitFinished(def.getData(), typechecked);
      }
    }
    myErrorReporter.report(new CycleError(cycle));
  }

  @Override
  public void preBodiesFound(List<Concrete.ResolvableDefinition> definitions) {
    List<Concrete.Definition> newDefs = new ArrayList<>(definitions.size());
    for (Concrete.ResolvableDefinition definition : definitions) {
      Concrete.ResolvableDefinition def = definition.accept(new ReplaceDataVisitor(), null);
      myDesugaredDefinitions.put(definition.getData(), def);
      if (def instanceof Concrete.Definition) {
        newDefs.add((Concrete.Definition) def);
      }
    }
    WhereVarsFixVisitor.fixDefinition(newDefs, myErrorReporter);
  }

  @Override
  public void headerFound(Concrete.ResolvableDefinition definition) {
    Concrete.ResolvableDefinition newDef = myDesugaredDefinitions.get(definition.getData());
    if (newDef != null) definition = newDef;
    myCurrentDefinitions = Collections.singletonList(definition.getData());
    typecheckingHeaderStarted(definition.getData());

    CountingErrorReporter countingErrorReporter = new CountingErrorReporter(myErrorReporter);
    CheckTypeVisitor visitor = new CheckTypeVisitor(new LocalErrorReporter(definition.getData(), countingErrorReporter), null, myExtensionProvider.getArendExtension(definition.getData()));
    visitor.setStatus(definition.getStatus().getTypecheckingStatus());
    DesugarVisitor.desugar(definition, visitor.getErrorReporter());
    Definition oldTypechecked = definition.getData().getTypechecked();
    DefinitionTypechecker typechecker = new DefinitionTypechecker(visitor, definition instanceof Concrete.Definition ? ((Concrete.Definition) definition).getRecursiveDefinitions() : Collections.emptySet());
    Definition typechecked = typechecker.typecheckHeader(oldTypechecked, new GlobalInstancePool(myInstanceProviderSet.get(definition.getData()), visitor), definition);
    UniverseKind universeKind = typechecked.getUniverseKind();
    if (typechecked instanceof TopLevelDefinition) {
      ((TopLevelDefinition) typechecked).setUniverseKind(UniverseKind.WITH_UNIVERSES);
    }
    if (typechecked.status() == Definition.TypeCheckingStatus.TYPE_CHECKING) {
      mySuspensions.put(definition.getData(), new Suspension(visitor, typechecker.isNew(), universeKind));
    }

    typecheckingHeaderFinished(definition.getData(), typechecked);
    myCurrentDefinitions = Collections.emptyList();
    if (!typechecked.status().headerIsOK()) {
      myHeadersAreOK = false;
    }
  }

  @Override
  public void bodiesFound(List<Concrete.ResolvableDefinition> definitions) {
    Map<FunctionDefinition,Concrete.Definition> functionDefinitions = new HashMap<>();
    Map<FunctionDefinition, List<? extends ElimClause<ExpressionPattern>>> clausesMap = new HashMap<>();
    Set<DataDefinition> dataDefinitions = new HashSet<>();
    List<Concrete.ResolvableDefinition> orderedDefinitions = new ArrayList<>(definitions.size());
    List<Concrete.ResolvableDefinition> otherDefs = new ArrayList<>();
    Set<TCDefReferable> refs = new HashSet<>();
    for (Concrete.ResolvableDefinition definition : definitions) {
      Concrete.ResolvableDefinition newDef = myDesugaredDefinitions.get(definition.getData());
      if (newDef == null) newDef = definition;
      Definition typechecked = newDef.getData().getTypechecked();
      if (typechecked instanceof DataDefinition) {
        dataDefinitions.add((DataDefinition) typechecked);
        orderedDefinitions.add(newDef);
      } else {
        otherDefs.add(newDef);
      }
      refs.add(definition.getData());
    }
    orderedDefinitions.addAll(otherDefs);

    DefinitionTypechecker typechecking = new DefinitionTypechecker(null, refs);
    myCurrentDefinitions = new ArrayList<>();
    for (Concrete.ResolvableDefinition definition : orderedDefinitions) {
      myCurrentDefinitions.add(definition.getData());
    }

    Set<Definition> newDefs = new HashSet<>();
    List<Pair<Definition, DefinitionListener>> listeners = new ArrayList<>();
    for (Concrete.ResolvableDefinition definition : orderedDefinitions) {
      Definition def = definition.getData().getTypechecked();
      if (def instanceof TopLevelDefinition) {
        Suspension suspension = mySuspensions.get(definition.getData());
        if (suspension != null) {
          ((TopLevelDefinition) def).setUniverseKind(suspension.universeKind);
        }
      }
    }

    for (Concrete.ResolvableDefinition definition : orderedDefinitions) {
      typecheckingBodyStarted(definition.getData());

      Definition def = definition.getData().getTypechecked();
      Suspension suspension = mySuspensions.remove(definition.getData());
      if (suspension != null && suspension.isNew) {
        newDefs.add(def);
      }
      if (myHeadersAreOK && suspension != null) {
        typechecking.setTypechecker(suspension.typechecker);
        typechecking.updateState(suspension.isNew);
        List<? extends ElimClause<ExpressionPattern>> clauses = typechecking.typecheckBody(def, definition, dataDefinitions);
        if (def instanceof FunctionDefinition && definition instanceof Concrete.Definition) {
          functionDefinitions.put((FunctionDefinition) def, (Concrete.Definition) definition);
          if (clauses != null) {
            clausesMap.put((FunctionDefinition) def, clauses);
          }
        }

        ArendExtension extension = suspension.typechecker.getExtension();
        if (extension != null) {
          DefinitionListener listener = extension.getDefinitionListener();
          if (listener != null) {
            listeners.add(new Pair<>(def, listener));
          }
        }
      }
    }
    myCurrentDefinitions = Collections.emptyList();

    myHeadersAreOK = true;

    boolean fixLevels = true;
    Set<TopLevelDefinition> allDefinitions = new LinkedHashSet<>();
    for (Concrete.ResolvableDefinition definition : orderedDefinitions) {
      Definition typechecked = definition.getData().getTypechecked();
      if (!newDefs.contains(typechecked)) continue;
      if (typechecked instanceof FunctionDefinition) {
        ((FunctionDefinition) typechecked).setRecursiveDefinitions(allDefinitions);
        allDefinitions.add((FunctionDefinition) typechecked);
      } else if (typechecked instanceof DataDefinition) {
        ((DataDefinition) typechecked).setRecursiveDefinitions(allDefinitions);
        allDefinitions.add((DataDefinition) typechecked);
      }
      if (definition instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) definition).getKind().isCoclause()) {
        fixLevels = false;
      }
    }

    if (fixLevels) {
      FixLevelParameters.fix(allDefinitions, newDefs);
    }

    if (!functionDefinitions.isEmpty()) {
      FindDefCallVisitor<DataDefinition> visitor = new FindDefCallVisitor<>(dataDefinitions, false);
      Iterator<Map.Entry<FunctionDefinition, Concrete.Definition>> it = functionDefinitions.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<FunctionDefinition, Concrete.Definition> entry = it.next();
        visitor.visitBody(entry.getKey().getBody(), null);
        Definition found = visitor.getFoundDefinition();
        if (found != null) {
          entry.getKey().setBody(null);
          entry.getKey().addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          myErrorReporter.report(new TypecheckingError("Mutually recursive function refers to data type '" + found.getName() + "'", entry.getValue()).withDefinition(entry.getKey().getReferable()));
          it.remove();
          visitor.clear();
        }
      }

      if (!functionDefinitions.isEmpty()) {
        checkRecursiveFunctions(functionDefinitions, clausesMap);
      }
    }

    for (Concrete.ResolvableDefinition definition : orderedDefinitions) {
      if (definition.getData().getTypechecked().accept(new SearchVisitor<Void>() {
        @Override
        protected CoreExpression.FindAction processDefCall(DefCallExpression expr, Void param) {
          return expr instanceof LeveledDefCallExpression && expr.getDefinition() instanceof TopLevelDefinition && allDefinitions.contains((TopLevelDefinition) expr.getDefinition()) && !((LeveledDefCallExpression) expr).getLevels().compare(expr.getDefinition().makeIdLevels(), CMP.EQ, DummyEquations.getInstance(), null) ? CoreExpression.FindAction.STOP : CoreExpression.FindAction.CONTINUE;
        }
      }, null)) {
        myErrorReporter.report(new TypecheckingError("Recursive call must have the same levels as the definition", definition));
      }
    }

    for (TopLevelDefinition definition : allDefinitions) {
      setParametersOriginalDefinitionsDependency(definition);
    }

    findAxiomsAndGoals(orderedDefinitions, newDefs);

    for (Definition definition : allDefinitions) {
      typecheckingBodyFinished(definition.getReferable(), definition);
    }

    for (Pair<Definition, DefinitionListener> pair : listeners) {
      pair.proj2.typechecked(pair.proj1);
    }
  }

  @Override
  public void useFound(List<Concrete.UseDefinition> definitions) {
    myCurrentDefinitions = new ArrayList<>();
    List<Concrete.UseDefinition> newDefs = new ArrayList<>(definitions.size());
    for (Concrete.UseDefinition definition : definitions) {
      myCurrentDefinitions.add(definition.getData());
      myCurrentDefinitions.add(definition.getUseParent());
      Concrete.ResolvableDefinition newDef = myDesugaredDefinitions.get(definition.getData());
      newDefs.add(newDef instanceof Concrete.UseDefinition ? (Concrete.UseDefinition) newDef : definition);
    }
    UseTypechecking.typecheck(newDefs, myErrorReporter);
    myCurrentDefinitions = Collections.emptyList();
  }

  @Override
  public void classFinished(Concrete.ClassDefinition definition) {
    DefinitionTypechecker.setDefaultDependencies(definition);
  }

  private void findAxiomsAndGoals(List<? extends Concrete.ResolvableDefinition> definitions, Set<Definition> newDefs) {
    Set<FunctionDefinition> axioms = new HashSet<>();
    Set<Definition> goals = new HashSet<>();
    VoidExpressionVisitor<Void> visitor = new VoidExpressionVisitor<>() {
      @Override
      public Void visitReference(ReferenceExpression expr, Void params) {
        if (expr.getBinding() instanceof PersistentEvaluatingBinding) {
          ((PersistentEvaluatingBinding) expr.getBinding()).getExpression().accept(this, params);
        }
        return null;
      }

      @Override
      public Void visitDefCall(DefCallExpression expr, Void params) {
        axioms.addAll(expr.getDefinition().getAxioms());
        goals.addAll(expr.getDefinition().getGoals());
        return super.visitDefCall(expr, params);
      }

      @Override
      public Void visitError(ErrorExpression expr, Void params) {
        if (expr.isGoal()) {
          goals.addAll(newDefs);
        }
        return null;
      }
    };

    for (Concrete.ResolvableDefinition definition : definitions) {
      Definition def = definition.getData().getTypechecked();
      def.accept(visitor, null);
      if (def instanceof TopLevelDefinition topDef && newDefs.contains(def)) {
        if (definition instanceof Concrete.BaseFunctionDefinition && ((Concrete.BaseFunctionDefinition) definition).getKind() == FunctionKind.AXIOM) {
          axioms.add((FunctionDefinition) def);
        }
        topDef.setAxioms(axioms);
        goals.addAll(topDef.getGoals());
        topDef.setGoals(goals);
      }
    }
  }

  private void checkRecursiveFunctions(Map<FunctionDefinition,Concrete.Definition> definitions, Map<FunctionDefinition, ? extends List<? extends ElimClause<ExpressionPattern>>> clauses) {
    boolean ok = true;
    DefinitionCallGraph definitionCallGraph = new DefinitionCallGraph();
    for (Map.Entry<FunctionDefinition, Concrete.Definition> entry : definitions.entrySet()) {
      List<? extends ElimClause<ExpressionPattern>> functionClauses = clauses.get(entry.getKey());
      definitionCallGraph.add(entry.getKey(), functionClauses == null ? Collections.emptyList() : functionClauses, definitions.keySet());
      for (DependentLink link = entry.getKey().getParameters(); link.hasNext(); link = link.getNext()) {
        link = link.getNextTyped(null);
        if (FindDefCallVisitor.findDefinition(link.getTypeExpr(), definitions.keySet()) != null) {
          myErrorReporter.report(new TypecheckingError("Mutually recursive functions are not allowed in parameters", entry.getValue()).withDefinition(entry.getKey().getReferable()));
          ok = false;
        }
      }
      if (entry.getValue() instanceof Concrete.FunctionDefinition && ((Concrete.FunctionDefinition) entry.getValue()).getBody() instanceof Concrete.CoelimFunctionBody) {
        myErrorReporter.report(new TypecheckingError("Recursive functions cannot be defined by copattern matching", entry.getValue()).withDefinition(entry.getKey().getReferable()));
        ok = false;
      }
    }

    if (!definitionCallGraph.checkTermination()) {
      for (Map.Entry<Definition, Set<RecursiveBehavior<Definition>>> entry : definitionCallGraph.myErrorInfo.entrySet()) {
        myErrorReporter.report(new TerminationCheckError(entry.getKey(), entry.getValue()));
      }
      ok = false;
    }

    if (!ok) {
      for (FunctionDefinition definition : definitions.keySet()) {
        definition.addStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
        definition.setBody(null);
      }
    }
  }
}
