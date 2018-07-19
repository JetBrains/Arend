package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedSingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.IntervalElim;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.instance.provider.InstanceProviderSet;
import com.jetbrains.jetpad.vclang.typechecking.order.listener.TypecheckingOrderingListener;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.ConcreteProvider;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.parameter;

public class Prelude {
  public static final ModulePath MODULE_PATH = new ModulePath("Prelude");

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;
  public static FunctionDefinition PLUS, MUL;

  public static DataDefinition INT;
  public static Constructor POS, NEG;

  public static FunctionDefinition COERCE;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition AT;
  public static FunctionDefinition ISO;

  public static DataDefinition PROP_TRUNC;
  public static DataDefinition SET_TRUNC;

  public static Constructor PROP_TRUNC_PATH_CON;
  public static Constructor SET_TRUNC_PATH_CON;

  private Prelude() {
  }

  public static void update(Definition definition) {
    switch (definition.getReferable().textRepresentation()) {
      case "Nat":
        NAT = (DataDefinition) definition;
        ZERO = NAT.getConstructor("zero");
        SUC = NAT.getConstructor("suc");
        break;
      case "+":
        PLUS = (FunctionDefinition) definition;
        break;
      case "*":
        MUL = (FunctionDefinition) definition;
        break;
      case "Int":
        INT = (DataDefinition) definition;
        POS = INT.getConstructor("pos");
        NEG = INT.getConstructor("neg");
        break;
      case "I":
        INTERVAL = (DataDefinition) definition;
        INTERVAL.setSort(Sort.PROP);
        INTERVAL.setMatchesOnInterval();
        LEFT = INTERVAL.getConstructor("left");
        RIGHT = INTERVAL.getConstructor("right");
        break;
      case "Path":
        PATH = (DataDefinition) definition;
        PATH.setSort(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, -1)));
        PATH_CON = PATH.getConstructor("path");
        break;
      case "=":
        PATH_INFIX = (FunctionDefinition) definition;
        break;
      case "@": {
        AT = (FunctionDefinition) definition;
        DependentLink atParams = AT.getParameters().subst(new ExprSubstitution(), LevelSubstitution.EMPTY, 3);
        SingleDependentLink intervalParam = new TypedSingleDependentLink(true, "i", ExpressionFactory.Interval());
        DependentLink pathParam = parameter("f", new PiExpression(Sort.STD, intervalParam, new AppExpression(new ReferenceExpression(atParams), new ReferenceExpression(intervalParam))));
        pathParam.setNext(parameter("i", ExpressionFactory.Interval()));
        Map<Constructor, ElimTree> children = Collections.singletonMap(PATH_CON, new LeafElimTree(pathParam, new AppExpression(new ReferenceExpression(pathParam), new ReferenceExpression(pathParam.getNext()))));
        ElimTree otherwise = new BranchElimTree(atParams, children);
        AT.setBody(new IntervalElim(AT.getParameters(), Collections.singletonList(new Pair<>(new ReferenceExpression(AT.getParameters().getNext()), new ReferenceExpression(AT.getParameters().getNext().getNext()))), otherwise));
        AT.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      }
      case "coe":
        COERCE = (FunctionDefinition) definition;
        DependentLink coeParams = COERCE.getParameters().subst(new ExprSubstitution(), LevelSubstitution.EMPTY, 2);
        COERCE.setBody(new BranchElimTree(coeParams, Collections.singletonMap(LEFT, new LeafElimTree(EmptyDependentLink.getInstance(), new ReferenceExpression(coeParams.getNext())))));
        COERCE.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "iso": {
        ISO = (FunctionDefinition) definition;
        DependentLink isoParams = ISO.getParameters().subst(new ExprSubstitution(), LevelSubstitution.EMPTY, 6);
        Map<Constructor, ElimTree> children = new HashMap<>();
        children.put(LEFT, new LeafElimTree(EmptyDependentLink.getInstance(), new ReferenceExpression(isoParams)));
        children.put(RIGHT, new LeafElimTree(EmptyDependentLink.getInstance(), new ReferenceExpression(isoParams.getNext())));
        ISO.setBody(new BranchElimTree(isoParams, children));
        ISO.setResultType(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))));
        ISO.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      }
      case "TrP":
        PROP_TRUNC = (DataDefinition) definition;
        PROP_TRUNC.setSort(Sort.PROP);
        PROP_TRUNC_PATH_CON = PROP_TRUNC.getConstructor("truncP");
        PROP_TRUNC.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "TrS":
        SET_TRUNC = (DataDefinition) definition;
        SET_TRUNC.setSort(Sort.SetOfLevel(new Level(LevelVariable.PVAR)));
        SET_TRUNC_PATH_CON = SET_TRUNC.getConstructor("truncS");
        SET_TRUNC.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      default:
        throw new IllegalStateException();
    }
  }

  public static void fillInTypecheckerState(TypecheckerState state) {
    state.record(NAT.getReferable(), NAT);
    state.record(PLUS.getReferable(), PLUS);
    state.record(MUL.getReferable(), MUL);
    state.record(ZERO.getReferable(), ZERO);
    state.record(SUC.getReferable(), SUC);
    state.record(INT.getReferable(), INT);
    state.record(POS.getReferable(), POS);
    state.record(NEG.getReferable(), NEG);
    state.record(INTERVAL.getReferable(), INTERVAL);
    state.record(LEFT.getReferable(), LEFT);
    state.record(RIGHT.getReferable(), RIGHT);
    state.record(PATH.getReferable(), PATH);
    state.record(PATH_CON.getReferable(), PATH_CON);
    state.record(PATH_INFIX.getReferable(), PATH_INFIX);
    state.record(AT.getReferable(), AT);
    state.record(COERCE.getReferable(), COERCE);
    state.record(ISO.getReferable(), ISO);

    state.record(PROP_TRUNC.getReferable(), PROP_TRUNC);
    for (Constructor constructor : PROP_TRUNC.getConstructors()) {
      state.record(constructor.getReferable(), constructor);
    }

    state.record(SET_TRUNC.getReferable(), SET_TRUNC);
    for (Constructor constructor : SET_TRUNC.getConstructors()) {
      state.record(constructor.getReferable(), constructor);
    }
  }

  public static void initialize(Scope scope, TypecheckerState state) {
    for (String name : new String[]{"Nat", "Int", "I", "Path", "=", "@", "coe", "iso", "TrP", "TrS"}) {
      update(state.getTypechecked((TCReferable) scope.resolveName(name)));
    }
    Scope natScope = scope.resolveNamespace("Nat");
    assert natScope != null;
    update(state.getTypechecked((TCReferable) natScope.resolveName("+")));
    update(state.getTypechecked((TCReferable) natScope.resolveName("*")));
  }

  public static class PreludeTypechecking extends TypecheckingOrderingListener {
    public PreludeTypechecking(InstanceProviderSet instanceProviderSet, TypecheckerState state, ConcreteProvider concreteProvider) {
      super(instanceProviderSet, state, concreteProvider, DummyErrorReporter.INSTANCE);
    }

    @Override
    public void typecheckingUnitFinished(TCReferable referable, Definition definition) {
      update(definition);
    }
  }
}
