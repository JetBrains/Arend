package org.arend.prelude;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.*;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.IntervalElim;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.Patterns;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.error.DummyErrorReporter;
import org.arend.ext.ArendPrelude;
import org.arend.ext.core.elimtree.CoreBranchKey;
import org.arend.ext.module.ModulePath;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.scope.Scope;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.PartialComparator;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.provider.ConcreteProvider;

import java.util.*;
import java.util.function.Consumer;

import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.arend.core.expr.ExpressionFactory.parameter;

public class Prelude implements ArendPrelude {
  public static final String VERSION = "1.1";

  public static final ModulePath MODULE_PATH = new ModulePath("Prelude");
  public static final String LIBRARY_NAME = "prelude";

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT;
  public static FunctionDefinition SQUEEZE, SQUEEZE_R;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;
  public static FunctionDefinition PLUS, MUL, MINUS;

  public static DataDefinition INT;
  public static Constructor POS, NEG;
  private static FunctionDefinition FROM_NAT;

  public static FunctionDefinition COERCE, COERCE2;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition IN_PROP;

  public static DConstructor IDP;
  public static FunctionDefinition AT;
  public static FunctionDefinition ISO;

  private static DataDefinition LESS_OR_EQ;
  private static Constructor ZERO_LESS_OR_EQ;
  private static Constructor SUC_LESS_OR_EQ;

  public static FunctionDefinition DIV_MOD;
  public static FunctionDefinition DIV;
  public static FunctionDefinition MOD;

  public static FunctionDefinition DIV_MOD_PROPERTY;
  public static FunctionDefinition MOD_PROPERTY;

  public static SigmaExpression DIV_MOD_TYPE;

  public static boolean isInitialized() {
    return INTERVAL != null;
  }

  public static void update(Definition definition) {
    switch (definition.getReferable().textRepresentation()) {
      case "Nat":
        NAT = (DataDefinition) definition;
        ZERO = NAT.getConstructor("zero");
        SUC = NAT.getConstructor("suc");
        DIV_MOD_TYPE = new SigmaExpression(Sort.SET0, parameter(true, Arrays.asList(null, null), Nat()));
        break;
      case "+":
        PLUS = (FunctionDefinition) definition;
        break;
      case "-":
        MINUS = (FunctionDefinition) definition;
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
        INTERVAL.setSort(new Sort(new Level(0), Level.INFINITY));
        INTERVAL.setMatchesOnInterval();
        LEFT = INTERVAL.getConstructor("left");
        RIGHT = INTERVAL.getConstructor("right");
        break;
      case "squeeze":
        SQUEEZE = (FunctionDefinition) definition;
        SQUEEZE.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "squeezeR":
        SQUEEZE_R = (FunctionDefinition) definition;
        SQUEEZE_R.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "Path":
        PATH = (DataDefinition) definition;
        PATH.getParameters().setType(new PiExpression(new Sort(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR, 2)), new TypedSingleDependentLink(true, null, new DataCallExpression(INTERVAL, new Sort(new Level(0), new Level(-1)), Collections.emptyList())), new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, 1)))));
        PATH_CON = PATH.getConstructor("path");
        break;
      case "=":
        PATH_INFIX = (FunctionDefinition) definition;
        PATH_INFIX.getParameters().setType(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, 1))));
        break;
      case "idp": {
        IDP = (DConstructor) definition;
        List<Expression> args = new ArrayList<>(2);
        args.add(new ReferenceExpression(IDP.getParameters()));
        args.add(new ReferenceExpression(IDP.getParameters().getNext()));
        IDP.setPattern(new ConstructorPattern(new FunCallExpression(IDP, Sort.STD, args), new Patterns(Collections.emptyList())));
        IDP.setNumberOfParameters(2);
        IDP.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      }
      case "@": {
        AT = (FunctionDefinition) definition;
        DependentLink atParams = DependentLink.Helper.take(AT.getParameters(), 3);
        SingleDependentLink intervalParam = new TypedSingleDependentLink(true, "i", ExpressionFactory.Interval());
        DependentLink pathParam = parameter("f", new PiExpression(Sort.STD, intervalParam, AppExpression.make(new ReferenceExpression(atParams), new ReferenceExpression(intervalParam))));
        pathParam.setNext(parameter("i", ExpressionFactory.Interval()));
        ElimTree otherwise = new BranchElimTree(atParams, Collections.singletonMap(PATH_CON, new LeafElimTree(pathParam, AppExpression.make(new ReferenceExpression(pathParam), new ReferenceExpression(pathParam.getNext())))));
        AT.setBody(new IntervalElim(5, Collections.singletonList(new IntervalElim.CasePair(new ReferenceExpression(AT.getParameters().getNext()), new ReferenceExpression(AT.getParameters().getNext().getNext()))), otherwise));
        AT.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      }
      case "coe":
        COERCE = (FunctionDefinition) definition;
        COERCE.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "coe2":
        COERCE2 = (FunctionDefinition) definition;
        COERCE2.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "iso": {
        ISO = (FunctionDefinition) definition;
        DependentLink isoParams = DependentLink.Helper.take(ISO.getParameters(), 6);
        Map<CoreBranchKey, ElimTree> children = new HashMap<>();
        children.put(LEFT, new LeafElimTree(EmptyDependentLink.getInstance(), new ReferenceExpression(isoParams)));
        children.put(RIGHT, new LeafElimTree(EmptyDependentLink.getInstance(), new ReferenceExpression(isoParams.getNext())));
        ISO.setBody(new BranchElimTree(isoParams, children));
        ISO.setResultType(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))));
        ISO.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      }
      case "fromNat":
        FROM_NAT = (FunctionDefinition) definition;
        FROM_NAT.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "inProp":
        IN_PROP = (FunctionDefinition) definition;
        IN_PROP.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "<=":
        LESS_OR_EQ = (DataDefinition) definition;
        ZERO_LESS_OR_EQ = LESS_OR_EQ.getConstructor("zero<=_");
        SUC_LESS_OR_EQ = LESS_OR_EQ.getConstructor("suc<=suc");
        break;
      case "divMod":
        DIV_MOD = (FunctionDefinition) definition;
        DIV_MOD.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "div":
        DIV = (FunctionDefinition) definition;
        DIV.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "mod":
        MOD = (FunctionDefinition) definition;
        MOD.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "divModProp":
        DIV_MOD_PROPERTY = (FunctionDefinition) definition;
        DIV_MOD_PROPERTY.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "modProp":
        MOD_PROPERTY = (FunctionDefinition) definition;
        MOD_PROPERTY.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      default:
        throw new IllegalStateException();
    }
  }

  public static void forEach(Consumer<Definition> consumer) {
    consumer.accept(NAT);
    consumer.accept(PLUS);
    consumer.accept(MINUS);
    consumer.accept(MUL);
    consumer.accept(ZERO);
    consumer.accept(SUC);
    consumer.accept(INT);
    consumer.accept(POS);
    consumer.accept(NEG);
    consumer.accept(FROM_NAT);
    consumer.accept(INTERVAL);
    consumer.accept(LEFT);
    consumer.accept(RIGHT);
    consumer.accept(SQUEEZE);
    consumer.accept(SQUEEZE_R);
    consumer.accept(PATH);
    consumer.accept(PATH_CON);
    consumer.accept(PATH_INFIX);
    consumer.accept(IN_PROP);
    consumer.accept(IDP);
    consumer.accept(AT);
    consumer.accept(COERCE);
    consumer.accept(COERCE2);
    consumer.accept(ISO);
    consumer.accept(LESS_OR_EQ);
    consumer.accept(ZERO_LESS_OR_EQ);
    consumer.accept(SUC_LESS_OR_EQ);
    consumer.accept(DIV_MOD);
    consumer.accept(DIV);
    consumer.accept(MOD);
    consumer.accept(DIV_MOD_PROPERTY);
    consumer.accept(MOD_PROPERTY);
  }

  public static void fillInTypecheckerState(TypecheckerState state) {
    forEach(def -> state.record(def.getReferable(), def));
  }

  public static void initialize(Scope scope, TypecheckerState state) {
    for (Referable ref : scope.getElements()) {
      if (ref instanceof TCReferable && ((TCReferable) ref).getKind().isTypecheckable()) {
        update(state.getTypechecked((TCReferable) ref));
      }
    }

    for (String name : new String[] {"Nat", "Int", "Path", "I"}) {
      Scope childScope = scope.resolveNamespace(name, true);
      assert childScope != null;
      for (Referable ref : childScope.getElements()) {
        if (ref instanceof TCReferable && ((TCReferable) ref).getKind().isTypecheckable()) {
          update(state.getTypechecked((TCReferable) ref));
        }
      }
    }
  }

  public static class PreludeTypechecking extends TypecheckingOrderingListener {
    public PreludeTypechecking(InstanceProviderSet instanceProviderSet, TypecheckerState state, ConcreteProvider concreteProvider, PartialComparator<TCReferable> comparator) {
      super(instanceProviderSet, state, concreteProvider, IdReferableConverter.INSTANCE, DummyErrorReporter.INSTANCE, comparator);
    }

    @Override
    public void typecheckingUnitFinished(TCReferable referable, Definition definition) {
      update(definition);
    }
  }

  @Override
  public DataDefinition getInterval() {
    return INTERVAL;
  }

  @Override
  public Constructor getLeft() {
    return LEFT;
  }

  @Override
  public Constructor getRight() {
    return RIGHT;
  }

  @Override
  public FunctionDefinition getSqueeze() {
    return SQUEEZE;
  }

  @Override
  public FunctionDefinition getSqueezeR() {
    return SQUEEZE_R;
  }

  @Override
  public DataDefinition getNat() {
    return NAT;
  }

  @Override
  public Constructor getZero() {
    return ZERO;
  }

  @Override
  public Constructor getSuc() {
    return SUC;
  }

  @Override
  public FunctionDefinition getPlus() {
    return PLUS;
  }

  @Override
  public FunctionDefinition getMul() {
    return MUL;
  }

  @Override
  public FunctionDefinition getMinus() {
    return MINUS;
  }

  @Override
  public DataDefinition getInt() {
    return INT;
  }

  @Override
  public Constructor getPos() {
    return POS;
  }

  @Override
  public Constructor getNeg() {
    return NEG;
  }

  @Override
  public FunctionDefinition getFromNat() {
    return FROM_NAT;
  }

  @Override
  public FunctionDefinition getCoerce() {
    return COERCE;
  }

  @Override
  public FunctionDefinition getCoerce2() {
    return COERCE2;
  }

  @Override
  public DataDefinition getPath() {
    return PATH;
  }

  @Override
  public FunctionDefinition getEquality() {
    return PATH_INFIX;
  }

  @Override
  public Constructor getPathCon() {
    return PATH_CON;
  }

  @Override
  public FunctionDefinition getInProp() {
    return IN_PROP;
  }

  @Override
  public DConstructor getIdp() {
    return IDP;
  }

  @Override
  public FunctionDefinition getAt() {
    return AT;
  }

  @Override
  public FunctionDefinition getIso() {
    return ISO;
  }

  @Override
  public DataDefinition getLessOrEq() {
    return LESS_OR_EQ;
  }

  @Override
  public Constructor getZeroLessOrEq() {
    return ZERO_LESS_OR_EQ;
  }

  @Override
  public Constructor getSucLessOrEq() {
    return SUC_LESS_OR_EQ;
  }

  @Override
  public FunctionDefinition getDivMod() {
    return DIV_MOD;
  }

  @Override
  public FunctionDefinition getDiv() {
    return DIV;
  }

  @Override
  public FunctionDefinition getMod() {
    return MOD;
  }

  @Override
  public FunctionDefinition getDivModProp() {
    return DIV_MOD_PROPERTY;
  }

  @Override
  public FunctionDefinition getModProp() {
    return MOD_PROPERTY;
  }
}
