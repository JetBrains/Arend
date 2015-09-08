package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ListErrorReporter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class DefinitionTest {
  ListErrorReporter errorReporter;

  @Before
  public void initialize() {
    RootModule.initialize();
    errorReporter = new ListErrorReporter();
  }

  @Test
  public void function() {
    // f : N => 0;
    FunctionDefinition def = new FunctionDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("f")), Abstract.Definition.DEFAULT_PRECEDENCE, new ArrayList<Argument>(), Nat(), Definition.Arrow.RIGHT, Zero());
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(errorReporter, def.getNamespace().getParent(), null, def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(errorReporter, def.getNamespace().getParent(), def.getTerm(), typedDef, localContext, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
  }

  @Test
  public void functionUntyped() {
    // f => 0;
    FunctionDefinition def = new FunctionDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("f")), Abstract.Definition.DEFAULT_PRECEDENCE, new ArrayList<Argument>(), null, Definition.Arrow.RIGHT, Zero());
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(errorReporter, def.getNamespace().getParent(), null, def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(errorReporter, def.getNamespace().getParent(), def.getTerm(), typedDef, localContext, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(def.hasErrors());
    assertEquals(Nat(), typedDef.getType());
  }

  @Test
  public void functionWithArgs() {
    // f (x : N) (y : N -> N) => y;
    List<Argument> arguments = new ArrayList<>();
    arguments.add(Tele(vars("x"), Nat()));
    arguments.add(Tele(vars("y"), Pi(Nat(), Nat())));

    FunctionDefinition def = new FunctionDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("f")), Abstract.Definition.DEFAULT_PRECEDENCE, arguments, null, Definition.Arrow.RIGHT, Index(0));
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(errorReporter, def.getNamespace().getParent(), null, def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(errorReporter, def.getNamespace().getParent(), def.getTerm(), typedDef, localContext, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(Nat(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat()))), typedDef.getType());
  }

  @Test
  public void dataType() {
    // \data D {A B : \Type0} (I : A -> B -> Type0) (a : A) (b : B) | con1 (x : A) (I x b) | con2 {y : B} (I a y)
    List<TypeArgument> parameters = new ArrayList<>(4);
    parameters.add(Tele(false, vars("A", "B"), Universe(0)));
    parameters.add(Tele(vars("I"), Pi(Index(1), Pi(Index(0), Universe(0)))));
    parameters.add(Tele(vars("a"), Index(2)));
    parameters.add(Tele(vars("b"), Index(2)));

    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("D")), Abstract.Definition.DEFAULT_PRECEDENCE, null, parameters);

    List<TypeArgument> arguments1 = new ArrayList<>(6);
    arguments1.add(Tele(vars("x"), Index(4)));
    arguments1.add(TypeArg(Apps(Index(3), Index(0), Index(1))));
    def.addConstructor(new Constructor(def.getNamespace().getChild(new Utils.Name("con1")), Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments1, def));

    List<TypeArgument> arguments2 = new ArrayList<>(6);
    arguments2.add(Tele(false, vars("y"), Index(3)));
    arguments2.add(TypeArg(Apps(Index(3), Index(2), Index(0))));
    def.addConstructor(new Constructor(def.getNamespace().getChild(new Utils.Name("con2")), Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments2, def));

    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(errorReporter, def.getNamespace().getParent(), null, def, localContext);
    assertNotNull(typedDef);
    for (int i = 0; i < def.getConstructors().size(); i++) {
      TypeChecking.typeCheckConstructor(errorReporter, def.getNamespace().getParent(), typedDef, def.getConstructors().get(i), localContext);
    }
    TypeChecking.typeCheckDataEnd(errorReporter, def.getNamespace().getParent(), def, typedDef, localContext);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(parameters, Universe(0)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(arguments1, Apps(Apps(Apps(DefCall(typedDef), Index(6), false, false), Index(5), false, false), Index(4), Index(3), Index(2))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(Apps(Apps(DefCall(typedDef), Index(6), false, false), Index(5), false, false), Index(4), Index(3), Index(2))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void dataType2() {
    // \data D (A : \7-Type2) = con1 (X : \1-Type5) X | con2 (Y : \2-Type3) A Y
    List<TypeArgument> parameters = new ArrayList<>(1);
    parameters.add(Tele(vars("A"), Universe(2, 7)));
    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("D")), Abstract.Definition.DEFAULT_PRECEDENCE, null, parameters);

    List<TypeArgument> arguments1 = new ArrayList<>(3);
    arguments1.add(Tele(vars("X"), Universe(5, 1)));
    arguments1.add(TypeArg(Index(0)));
    def.addConstructor(new Constructor(def.getNamespace().getChild(new Utils.Name("con1")), Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments1, def));

    List<TypeArgument> arguments2 = new ArrayList<>(4);
    arguments2.add(Tele(vars("Y"), Universe(3, 2)));
    arguments2.add(TypeArg(Index(1)));
    arguments2.add(TypeArg(Index(1)));
    def.addConstructor(new Constructor(def.getNamespace().getChild(new Utils.Name("con2")), Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments2, def));

    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(errorReporter, def.getNamespace().getParent(), null, def, localContext);
    assertNotNull(typedDef);
    for (int i = 0; i < def.getConstructors().size(); i++) {
      TypeChecking.typeCheckConstructor(errorReporter, def.getNamespace().getParent(), typedDef, def.getConstructors().get(i), localContext);
    }
    TypeChecking.typeCheckDataEnd(errorReporter, def.getNamespace().getParent(), def, typedDef, localContext);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(parameters, Universe(6, 7)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(arguments1, Apps(DefCall(typedDef), Index(2))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(DefCall(typedDef), Index(3))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void constructor() {
    // \data D (A : \Type0) = con (B : \Type1) A B |- con Nat zero zero : D Nat
    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("D")), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    Constructor con = new Constructor(def.getNamespace().getChild(new Utils.Name("con")), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))), def);
    def.addConstructor(con);

    Expression expr = Apps(DefCall(con), Nat(), Zero(), Zero());
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), null, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Apps(DefCall(def), Nat()), result.type);
  }

  @Test
  public void constructorInfer() {
    // \data D (A : \Type0) = con (B : \Type1) A B, f : D (Nat -> Nat) -> Nat |- f (con Nat (\lam x => x) zero) : Nat
    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("D")), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    Constructor con = new Constructor(def.getNamespace().getChild(new Utils.Name("con")), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))), def);
    def.addConstructor(con);

    Expression expr = Apps(Index(0), Apps(DefCall(con), Nat(), Lam("x", Index(0)), Zero()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Apps(DefCall(def), Pi(Nat(), Nat())), Nat())));

    CheckTypeVisitor.OKResult result = expr.checkType(localContext, null, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void constructorConst() {
    // \data D (A : \Type0) = con A, f : (Nat -> D Nat) -> Nat -> Nat |- f con : Nat -> Nat
    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("D")), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    Constructor con = new Constructor(def.getNamespace().getChild(new Utils.Name("con")), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(TypeArg(Index(0))), def);
    def.addConstructor(con);

    Expression expr = Apps(Index(0), DefCall(con));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Pi(Nat(), Apps(DefCall(def), Nat())), Pi(Nat(), Nat()))));

    CheckTypeVisitor.OKResult result = expr.checkType(localContext, null, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void patternVector() {
    parseDefs(
        "\\data Vec (A : \\Type0) (n : Nat) | Vec _ (zero) => Nil | Vec _ (suc m) => Cons A (Vec A m)");
  }

  @Test
  public void patternDepParams() {
    parseDefs(
        "\\data D (n : Nat) (p : n = n) | D (zero) _ => d \\data C {n : Nat} {p : n = n} (D n p) | C {zero} (d) => c (p = p)");
  }

  @Test
  public void patternDepParamsError() {
    parseDefs(
        "\\data D (n : Nat) (p : n = n) | D (zero) _ => d \\data C {n : Nat} {p : n = n} (D n p) | C (d) => c (p = p)", 1);
  }

  @Test
  public void patternNested() {
    parseDefs("\\data C (n : Nat) | C (suc (suc n)) => c2 (n = n)");
  }

  @Test
  public void patternDataLE() {
    parseDefs("\\data LE (n m : Nat) | LE (zero) m => LE-zero | LE (suc n) (suc m) => LE-suc (LE n m)");
  }

  @Test
  public void patternImplicitError() {
    parseDefs("\\data D (A : Nat) | D {A} => d", 1);
  }

  @Test
  public void patternConstructorCall() {
    parseDefs("\\data D {n : Nat} | D {zero} => d \\function test => d");
  }

  @Test
  public void patternAbstract() {
    parseDefs("\\data Wheel | wheel \\data VehicleType | bikeType | carType " +
        "\\data Vehicle (t : VehicleType) " +
        "| Vehicle (carType) => car Wheel Wheel Wheel Wheel" +
        "| Vehicle (bikeType) => bike Wheel Wheel");
  }

  @Test
  public void patternUnkownConstructorError() {
    parseDefs("\\data D (n : Nat) | D (suc (luc m)) => d", 1);
  }

  @Test
  public void patternLift() {
    parseDefs("\\data D (n : Nat) | D (zero) => d " +
        "\\data C (m : Nat) (n : Nat) (D m) | C (zero) (zero) (d) => c");
  }

  @Test
  public void patternLiftError() {
    parseDefs("\\data D (n : Nat) | D (zero) => d " +
        "\\data C (m : Nat) (n : Nat) (D m) | C _ (zero) (d) => c", 1);
  }

  @Test
  public void patternMultipleSubst() {
    parseDefs("\\data D (n : Nat) (m : Nat) | d (n = n) (m = m)" +
        "\\data C | c (n m : Nat) (D n m) " +
        "\\data E C | E (c (zero) (suc (zero)) (d _ _)) => e" +
        "\\function test => (E (c 0 1 (d (path (\\lam _ => 0)) (path (\\lam _ => 1))))).e");
  }

  @Test
  public void patternConstructorDefCall() {
    parseDefs("\\data D (n : Nat) (m : Nat) | D (suc n) (suc m) => d (n = n) (m = m)" +
            "\\function test => d (path (\\lam _ => 1)) (path (\\lam _ => 0))");
  }

  @Test
  public void patternConstructorDefCallError() {
    parseDefs("\\data D (n : Nat) | D (zero) => d \\function test (n : Nat) : D n => d", 1);
  }

  @Test
  public void patternSubstTest() {
    parseDefs("\\data E (n : Nat) | E (zero) => e" +
        "\\data D (n : Nat) (E n) | D (zero) (e) => d" +
        "\\function test => d");
  }

  @Test
  public void patternExpandArgsTest() {
    parseDefs(
        "\\data D (n : Nat) | d (n = n) " +
        "\\data C (D 1) | C (d p) => c" +
        "\\function test : C (d (path (\\lam _ => 1))) => c");
  }

  @Test
  public void patternNormalizeTest() {
    parseDefs(
        "\\data E (x : 0 = 0) | e" +
        "\\data C (n : Nat) | C (suc n) => c (n = n)" +
        "\\data D ((\\lam (x : \\Type0) => x) (C 1)) | D (c p) => x (E p)" +
        "\\function test => x (E (path (\\lam _ => 0))).e");
  }
}
