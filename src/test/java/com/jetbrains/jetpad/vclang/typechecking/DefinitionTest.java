package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static org.junit.Assert.*;

public class DefinitionTest {
  ListErrorReporter errorReporter;

  @Before
  public void initialize() {
    RootModule.initialize();
    errorReporter = new ListErrorReporter();
  }

  private Definition typeCheckDefinition(Definition definition) {
    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(definition.getParentNamespace(), errorReporter);
    // TODO: check for null?
    definition.getParentNamespace().addMember(definition.getName());
    visitor.setNamespaceMember(definition.getParentNamespace().getMember(definition.getName().name));
    return definition.accept(visitor, null);
  }

  @Test
  public void function() {
    // f : N => 0;
    FunctionDefinition def = new FunctionDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("f"), Abstract.Definition.DEFAULT_PRECEDENCE, new ArrayList<Argument>(), Nat(), Definition.Arrow.RIGHT, Zero());
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDefinition(def);
    assertNotNull(typedDef);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
  }

  @Test
  public void functionUntyped() {
    // f => 0;
    FunctionDefinition def = new FunctionDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("f"), Abstract.Definition.DEFAULT_PRECEDENCE, new ArrayList<Argument>(), null, Definition.Arrow.RIGHT, Zero());
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDefinition(def);
    assertNotNull(typedDef);
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

    FunctionDefinition def = new FunctionDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("f"), Abstract.Definition.DEFAULT_PRECEDENCE, arguments, null, Definition.Arrow.RIGHT, Index(0));
    FunctionDefinition typedDef = (FunctionDefinition) typeCheckDefinition(def);
    assertNotNull(typedDef);
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

    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("D"), Abstract.Definition.DEFAULT_PRECEDENCE, null, parameters);
    Namespace defNamespace = def.getParentNamespace().getChild(def.getName());

    List<TypeArgument> arguments1 = new ArrayList<>(6);
    arguments1.add(Tele(vars("x"), Index(4)));
    arguments1.add(TypeArg(Apps(Index(3), Index(0), Index(1))));
    def.addConstructor(new Constructor(defNamespace, new Name("con1"), Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments1, def));

    List<TypeArgument> arguments2 = new ArrayList<>(6);
    arguments2.add(Tele(false, vars("y"), Index(3)));
    arguments2.add(TypeArg(Apps(Index(3), Index(2), Index(0))));
    def.addConstructor(new Constructor(defNamespace, new Name("con2"), Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments2, def));

    DataDefinition typedDef = (DataDefinition) typeCheckDefinition(def);
    assertNotNull(typedDef);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(parameters, Universe(0)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(arguments1, Apps(Apps(Apps(DataCall(typedDef), Index(6), false, false), Index(5), false, false), Index(4), Index(3), Index(2))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(Apps(Apps(DataCall(typedDef), Index(6), false, false), Index(5), false, false), Index(4), Index(3), Index(2))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void dataType2() {
    // \data D (A : \7-Type2) = con1 (X : \1-Type5) X | con2 (Y : \2-Type3) A Y
    List<TypeArgument> parameters = new ArrayList<>(1);
    parameters.add(Tele(vars("A"), Universe(2, 7)));
    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("D"), Abstract.Definition.DEFAULT_PRECEDENCE, null, parameters);
    Namespace namespace = def.getParentNamespace().getChild(def.getName());

    List<TypeArgument> arguments1 = new ArrayList<>(3);
    arguments1.add(Tele(vars("X"), Universe(5, 1)));
    arguments1.add(TypeArg(Index(0)));
    def.addConstructor(new Constructor(namespace, new Name("con1"), Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments1, def));

    List<TypeArgument> arguments2 = new ArrayList<>(4);
    arguments2.add(Tele(vars("Y"), Universe(3, 2)));
    arguments2.add(TypeArg(Index(1)));
    arguments2.add(TypeArg(Index(1)));
    def.addConstructor(new Constructor(namespace, new Name("con2"), Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments2, def));

    DataDefinition typedDef = (DataDefinition) typeCheckDefinition(def);
    assertNotNull(typedDef);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(parameters, Universe(6, 7)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(arguments1, Apps(DataCall(typedDef), Index(2))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(DataCall(typedDef), Index(3))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void constructor() {
    // \data D (A : \Type0) = con (B : \Type1) A B |- con Nat zero zero : D Nat
    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("D"), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    RootModule.ROOT.getChild(new Name("test")).addDefinition(def);
    Constructor con = new Constructor(def.getParentNamespace().getChild(def.getName()), new Name("con"), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))), def);
    def.addConstructor(con);

    Expression expr = Apps(ConCall(con), Nat(), Zero(), Zero());
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), null, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Apps(DataCall(def), Nat()), result.type);
  }

  @Test
  public void constructorInfer() {
    // \data D (A : \Type0) = con (B : \Type1) A B, f : D (Nat -> Nat) -> Nat |- f (con Nat (\lam x => x) zero) : Nat
    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("D"), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    RootModule.ROOT.getChild(new Name("test")).addDefinition(def);
    Constructor con = new Constructor(def.getParentNamespace().getChild(def.getName()), new Name("con"), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))), def);
    def.addConstructor(con);

    Expression expr = Apps(Index(0), Apps(ConCall(con), Nat(), Lam("x", Index(0)), Zero()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Apps(DataCall(def), Pi(Nat(), Nat())), Nat())));

    CheckTypeVisitor.OKResult result = expr.checkType(localContext, null, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void constructorConst() {
    // \data D (A : \Type0) = con A, f : (Nat -> D Nat) -> Nat -> Nat |- f con : Nat -> Nat
    DataDefinition def = new DataDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("D"), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    RootModule.ROOT.getChild(new Name("test")).addDefinition(def);
    Constructor con = new Constructor(def.getParentNamespace().getChild(def.getName()), new Name("con"), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(TypeArg(Index(0))), def);
    def.addConstructor(con);

    Expression expr = Apps(Index(0), ConCall(con));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Pi(Nat(), Apps(DataCall(def), Nat())), Pi(Nat(), Nat()))));

    CheckTypeVisitor.OKResult result = expr.checkType(localContext, null, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertNotNull(result);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void patternVector() {
    typeCheckDef("\\data Vec \\Type0 Nat | Vec _ zero => Nil | Vec A (suc m) => Cons A (Vec A m)");
  }

  @Test
  public void patternDepParams() {
    typeCheckClass("\\data D (n : Nat) (n = n) | D zero _ => d \\data C {n : Nat} {p : n = n} (D n p) | C {zero} {p} d => c (p = p)");
  }

  @Test
  public void patternDepParamsError() {
    typeCheckClass("\\data D (n : Nat) (n = n) | D zero _ => d \\data C {n : Nat} {p : n = n} (D n p) | C {_} {p} d => c (p = p)", 1);
  }

  @Test
  public void patternNested() {
    typeCheckDef("\\data C (n : Nat) | C (suc (suc n)) => c2 (n = n)");
  }

  @Test
  public void patternDataLE() {
    typeCheckDef("\\data LE (n m : Nat) | LE zero m => LE-zero | LE (suc n) (suc m) => LE-suc (LE n m)");
  }

  @Test
  public void patternImplicitError() {
    typeCheckDef("\\data D (A : Nat) | D {A} => d", 1);
  }

  @Test
  public void patternConstructorCall() {
    typeCheckClass("\\data D {n : Nat} | D {zero} => d \\function test => d");
  }

  @Test
  public void patternAbstract() {
    typeCheckClass("\\data Wheel | wheel \\data VehicleType | bikeType | carType " +
        "\\data Vehicle (t : VehicleType) " +
        "| Vehicle (carType) => car Wheel Wheel Wheel Wheel" +
        "| Vehicle (bikeType) => bike Wheel Wheel");
  }

  @Test
  public void patternUnknownConstructorError() {
    typeCheckDef("\\data D (n : Nat) | D (suc (luc m)) => d", 1);
  }

  @Test
  public void patternLift() {
    typeCheckClass(
        "\\data D (n : Nat) | D (zero) => d\n" +
            "\\data C (m : Nat) (n : Nat) (D m) | C (zero) (zero) (d) => c");
  }

  @Test
  public void patternLiftError() {
    typeCheckClass(
        "\\data D (n : Nat) | D (zero) => d\n" +
            "\\data C (m : Nat) (n : Nat) (D m) | C _ (zero) (d) => c", 1);
  }

  @Test
  public void patternMultipleSubst() {
    typeCheckClass(
        "\\data D (n : Nat) (m : Nat) | d (n = n) (m = m)\n" +
            "\\data C | c (n m : Nat) (D n m)\n" +
            "\\data E C | E (c (zero) (suc (zero)) (d _ _)) => e\n" +
            "\\function test => (E (c 0 1 (d (path (\\lam _ => 0)) (path (\\lam _ => 1))))).e");
  }

  @Test
  public void patternConstructorDefCall() {
    typeCheckClass(
        "\\data D (n : Nat) (m : Nat) | D (suc n) (suc m) => d (n = n) (m = m)\n" +
            "\\function test => d (path (\\lam _ => 1)) (path (\\lam _ => 0))");
  }

  @Test
  public void patternConstructorDefCallError() {
    typeCheckClass("\\data D (n : Nat) | D (zero) => d \\function test (n : Nat) : D n => d", 1);
  }

  @Test
  public void patternSubstTest() {
    typeCheckClass(
        "\\data E (n : Nat) | E (zero) => e\n" +
            "\\data D (n : Nat) (E n) | D (zero) (e) => d\n" +
            "\\function test => d");
  }

  @Test
  public void patternExpandArgsTest() {
    typeCheckClass(
        "\\data D (n : Nat) | d (n = n)\n" +
            "\\data C (D 1) | C (d p) => c\n" +
            "\\function test : C (d (path (\\lam _ => 1))) => c");
  }

  @Test
  public void patternNormalizeTest() {
    typeCheckClass(
        "\\data E (x : 0 = 0) | e\n" +
            "\\data C (n : Nat) | C (suc n) => c (n = n)\n" +
            "\\data D ((\\lam (x : \\Type0) => x) (C 1)) | D (c p) => x (E p)\n" +
            "\\function test => x (E (path (\\lam _ => 0))).e");
  }

  @Test
  public void patternTypeCheck() {
    typeCheckClass(
        "\\static \\function f (x : Nat -> Nat) => x 0\n" +
            "\\static \\data Test (A : \\Set0)\n" +
            "| Test (suc n) => foo (f n)", 1);
  }
}
