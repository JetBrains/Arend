package org.arend.prelude;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.TypedDependentLink;
import org.arend.core.context.param.UnusedIntervalDependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.error.DummyErrorReporter;
import org.arend.ext.ArendPrelude;
import org.arend.ext.concrete.expr.SigmaFieldKind;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.core.definition.CoreClassField;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.module.ModulePath;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.reference.Precedence;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.*;
import org.arend.naming.reference.converter.ReferableConverter;
import org.arend.naming.scope.Scope;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.arend.typechecking.order.PartialComparator;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.provider.ConcreteProvider;
import org.arend.util.SingletonList;
import org.arend.util.Version;

import java.util.*;
import java.util.function.Consumer;

import static org.arend.core.expr.ExpressionFactory.*;

public class Prelude implements ArendPrelude {
  public static final Version VERSION = GeneratedVersion.VERSION;

  public static final String LIBRARY_NAME = "prelude";
  public static final ModulePath MODULE_PATH = new ModulePath("Prelude");
  public static final ModuleLocation MODULE_LOCATION = new ModuleLocation(LIBRARY_NAME, true, ModuleLocation.LocationKind.GENERATED, MODULE_PATH);

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT;
  public static FunctionDefinition SQUEEZE, SQUEEZE_R;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;
  public static FunctionDefinition PLUS, MUL, MINUS;

  public static DataDefinition FIN;
  public static Constructor FIN_ZERO;
  public static Constructor FIN_SUC;
  public static FunctionDefinition FIN_FROM_NAT;

  public static DataDefinition INT;
  public static Constructor POS, NEG;

  public static FunctionDefinition COERCE, COERCE2;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition IN_PROP;

  public static DConstructor IDP;
  public static FunctionDefinition AT;
  public static FunctionDefinition ISO;

  public static FunctionDefinition DIV_MOD;
  public static FunctionDefinition DIV;
  public static FunctionDefinition MOD;
  public static FunctionDefinition DIV_MOD_PROPERTY;
  public static SigmaExpression DIV_MOD_TYPE;

  public static final String ARRAY_NAME = "Array";
  public static FunctionDefinition ARRAY;
  public static ClassDefinition DEP_ARRAY;
  public static ClassField ARRAY_ELEMENTS_TYPE;
  public static ClassField ARRAY_LENGTH;
  public static ClassField ARRAY_AT;
  public static DConstructor EMPTY_ARRAY;
  public static DConstructor ARRAY_CONS;
  public static FunctionDefinition ARRAY_INDEX;

  public static boolean isInitialized() {
    return INTERVAL != null;
  }

  public static void update(Definition definition) {
    switch (definition.getReferable().textRepresentation()) {
      case "Nat":
        NAT = (DataDefinition) definition;
        ZERO = NAT.getConstructor("zero");
        SUC = NAT.getConstructor("suc");
        DIV_MOD_TYPE = new SigmaExpression(Sort.SET0, sigmaParameter(SigmaFieldKind.ANY, Arrays.asList(null, null), Nat()));
        break;
      case "Fin":
        FIN = (DataDefinition) definition;
        FIN.setSort(Sort.SET0);
        if (FIN.getConstructors().isEmpty()) {
          FIN_ZERO = new Constructor(new LocatedReferableImpl(Precedence.DEFAULT, "zero", FIN.getRef(), GlobalReferable.Kind.CONSTRUCTOR), FIN);
          DependentLink binding = new TypedDependentLink(true, "n", ExpressionFactory.Nat(), EmptyDependentLink.getInstance());
          List<ExpressionPattern> patterns = Collections.singletonList(new ConstructorExpressionPattern(new ConCallExpression(SUC, Levels.EMPTY, Collections.emptyList(), Collections.emptyList()), Collections.singletonList(new BindingPattern(binding))));
          FIN_ZERO.setPatterns(patterns);
          FIN_ZERO.setParameters(EmptyDependentLink.getInstance());
          FIN_ZERO.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          FIN_ZERO.getReferable().setTypechecked(FIN_ZERO);
          FIN.addConstructor(FIN_ZERO);
          FIN_SUC = new Constructor(new LocatedReferableImpl(Precedence.DEFAULT, "suc", FIN.getRef(), GlobalReferable.Kind.CONSTRUCTOR), FIN);
          FIN_SUC.setPatterns(patterns);
          FIN_SUC.setParameters(new TypedDependentLink(true, null, new DataCallExpression(FIN, Levels.EMPTY, new SingletonList<>(new ReferenceExpression(binding))), EmptyDependentLink.getInstance()));
          FIN_SUC.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          FIN_SUC.getReferable().setTypechecked(FIN_SUC);
          FIN.addConstructor(FIN_SUC);
        } else {
          FIN_ZERO = FIN.getConstructor("zero");
          FIN_SUC = FIN.getConstructor("suc");
        }
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
        PATH.setSort(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, -1)));
        PATH.setCovariant(1, false);
        PATH.setCovariant(2, false);
        PATH_CON = PATH.getConstructor("path");
        break;
      case "=": {
        PATH_INFIX = (FunctionDefinition) definition;
        PATH_INFIX.setResultType(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, -1))));
        DataCallExpression dataCall = (DataCallExpression) PATH_INFIX.getBody();
        assert dataCall != null;
        PATH_INFIX.setBody(new DataCallExpression(dataCall.getDefinition(), dataCall.getLevels(), Arrays.asList(new LamExpression(new Sort(new Level(LevelVariable.PVAR, 1), Level.INFINITY), UnusedIntervalDependentLink.INSTANCE, ((LamExpression) dataCall.getDefCallArguments().get(0)).getBody()), dataCall.getDefCallArguments().get(1), dataCall.getDefCallArguments().get(2))));
        break;
      }
      case "idp": {
        IDP = (DConstructor) definition;
        List<Expression> args = new ArrayList<>(2);
        args.add(new ReferenceExpression(IDP.getParameters()));
        args.add(new ReferenceExpression(IDP.getParameters().getNext()));
        IDP.setPattern(new ConstructorExpressionPattern(FunCallExpression.makeFunCall(IDP, LevelPair.STD, args), Collections.emptyList()));
        IDP.setNumberOfParameters(2);
        IDP.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        PathExpression pathExpr = (PathExpression) IDP.getBody();
        assert pathExpr != null;
        Sort sort = new Sort(new Level(LevelVariable.PVAR), Level.INFINITY);
        IDP.setBody(new PathExpression(pathExpr.getLevels(), new LamExpression(sort, UnusedIntervalDependentLink.INSTANCE, args.get(0)), new LamExpression(sort, UnusedIntervalDependentLink.INSTANCE, ((LamExpression) pathExpr.getArgument()).getBody())));
        break;
      }
      case "@": {
        AT = (FunctionDefinition) definition;
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
        ISO.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      }
      case "fromNat":
        FIN_FROM_NAT = (FunctionDefinition) definition;
        FIN_FROM_NAT.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "inProp":
        IN_PROP = (FunctionDefinition) definition;
        IN_PROP.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
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
      case "DArray":
        DEP_ARRAY = (ClassDefinition) definition;
        ARRAY_ELEMENTS_TYPE = DEP_ARRAY.getPersonalFields().get(1);
        ARRAY_LENGTH = DEP_ARRAY.getPersonalFields().get(0);
        ARRAY_AT = DEP_ARRAY.getPersonalFields().get(2);
        break;
      case "Array":
        ARRAY = (FunctionDefinition) definition;
        if (ARRAY.getRef() instanceof TypedLocatedReferable) {
          ((TypedLocatedReferable) ARRAY.getRef()).setBodyReference(DEP_ARRAY.getRef());
        }
        break;
      case "nil":
        EMPTY_ARRAY = (DConstructor) definition;
        EMPTY_ARRAY.setPattern(new ConstructorExpressionPattern(FunCallExpression.makeFunCall(EMPTY_ARRAY, LevelPair.STD, Collections.emptyList()), Collections.singletonList(new BindingPattern(EMPTY_ARRAY.getParameters()))));
        EMPTY_ARRAY.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "::":
        ARRAY_CONS = (DConstructor) definition;
        ARRAY_CONS.setPattern(new ConstructorExpressionPattern(FunCallExpression.makeFunCall(ARRAY_CONS, LevelPair.STD, Collections.emptyList()), Arrays.asList(new BindingPattern(ARRAY_CONS.getParameters()), new BindingPattern(ARRAY_CONS.getParameters().getNext()), new BindingPattern(ARRAY_CONS.getParameters().getNext().getNext()), new BindingPattern(ARRAY_CONS.getParameters().getNext().getNext().getNext()))));
        ARRAY_CONS.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
        break;
      case "!!":
        ARRAY_INDEX = (FunctionDefinition) definition;
        ARRAY_INDEX.setBody(null);
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
    consumer.accept(FIN);
    consumer.accept(FIN_FROM_NAT);
    consumer.accept(INT);
    consumer.accept(POS);
    consumer.accept(NEG);
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
    consumer.accept(DIV_MOD);
    consumer.accept(DIV);
    consumer.accept(MOD);
    consumer.accept(DIV_MOD_PROPERTY);
    consumer.accept(DEP_ARRAY);
    consumer.accept(ARRAY);
    consumer.accept(ARRAY_ELEMENTS_TYPE);
    consumer.accept(ARRAY_LENGTH);
    consumer.accept(ARRAY_AT);
    consumer.accept(EMPTY_ARRAY);
    consumer.accept(ARRAY_CONS);
    consumer.accept(ARRAY_INDEX);
  }

  public static void initialize(Scope scope) {
    for (Referable ref : scope.getElements()) {
      if (ref instanceof TCDefReferable && ((TCDefReferable) ref).getKind().isTypecheckable()) {
        update(((TCDefReferable) ref).getTypechecked());
      }
    }

    for (String name : new String[] { "Nat", "Int", "Fin", "Path", "I", "DArray" }) {
      Scope childScope = scope.resolveNamespace(name, true);
      assert childScope != null;
      for (Referable ref : childScope.getElements()) {
        if (ref instanceof TCDefReferable && ((TCDefReferable) ref).getKind().isTypecheckable()) {
          update(((TCDefReferable) ref).getTypechecked());
        }
      }
    }
  }

  public static class PreludeTypechecking extends TypecheckingOrderingListener {
    public PreludeTypechecking(InstanceProviderSet instanceProviderSet, ConcreteProvider concreteProvider, ReferableConverter referableConverter, PartialComparator<TCDefReferable> comparator) {
      super(instanceProviderSet, concreteProvider, referableConverter, DummyErrorReporter.INSTANCE, comparator, ref -> null);
    }

    @Override
    public void typecheckingUnitFinished(TCDefReferable referable, Definition definition) {
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
  public DataDefinition getFin() {
    return FIN;
  }

  @Override
  public FunctionDefinition getFinFromNat() {
    return FIN_FROM_NAT;
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
  public ArendRef getPathConRef() {
    return PATH_CON.getRef();
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
  public TCDefReferable getAtRef() {
    return AT.getRef();
  }

  @Override
  public FunctionDefinition getIso() {
    return ISO;
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
  public CoreClassDefinition getDArray() {
    return DEP_ARRAY;
  }

  @Override
  public FunctionDefinition getArray() {
    return ARRAY;
  }

  @Override
  public CoreClassField getArrayElementsType() {
    return ARRAY_ELEMENTS_TYPE;
  }

  @Override
  public CoreClassField getArrayLength() {
    return ARRAY_LENGTH;
  }

  @Override
  public CoreClassField getArrayAt() {
    return ARRAY_AT;
  }

  @Override
  public CoreFunctionDefinition getEmptyArray() {
    return EMPTY_ARRAY;
  }

  @Override
  public CoreFunctionDefinition getArrayCons() {
    return ARRAY_CONS;
  }

  @Override
  public CoreFunctionDefinition getArrayIndex() {
    return ARRAY_INDEX;
  }
}
