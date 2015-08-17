package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseExpr;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class DefinitionTest {
  private ModuleLoader dummyModuleLoader;

  @Before
  public void initialize() {
    dummyModuleLoader = new ModuleLoader();
    dummyModuleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
  }

  @Test
  public void function() {
    // f : N => 0;
    ModuleLoader moduleLoader = new ModuleLoader();
    FunctionDefinition def = new FunctionDefinition(new Utils.Name("f"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, new ArrayList<Argument>(), Nat(), Definition.Arrow.RIGHT, Zero());
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(moduleLoader, def.getParent(), def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(moduleLoader, def.getTerm(), typedDef, localContext, null, false);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertFalse(typedDef.hasErrors());
  }

  @Test
  public void functionUntyped() {
    // f => 0;
    ModuleLoader moduleLoader = new ModuleLoader();
    FunctionDefinition def = new FunctionDefinition(new Utils.Name("f"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, new ArrayList<Argument>(), null, Definition.Arrow.RIGHT, Zero());
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(moduleLoader, def.getParent(), def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(moduleLoader, def.getTerm(), typedDef, localContext, null, false);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertFalse(def.hasErrors());
    assertEquals(Nat(), typedDef.getType());
  }

  @Test
  public void functionWithArgs() {
    // f (x : N) (y : N -> N) => y;
    List<Argument> arguments = new ArrayList<>();
    arguments.add(Tele(vars("x"), Nat()));
    arguments.add(Tele(vars("y"), Pi(Nat(), Nat())));

    ModuleLoader moduleLoader = new ModuleLoader();
    FunctionDefinition def = new FunctionDefinition(new Utils.Name("f"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, arguments, null, Definition.Arrow.RIGHT, Index(0));
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedDef = TypeChecking.typeCheckFunctionBegin(moduleLoader, def.getParent(), def, localContext, null);
    assertNotNull(typedDef);
    TypeChecking.typeCheckFunctionEnd(moduleLoader, def.getTerm(), typedDef, localContext, null, false);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
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

    ModuleLoader moduleLoader = new ModuleLoader();
    DataDefinition def = new DataDefinition(new Utils.Name("D"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, null, parameters);

    List<TypeArgument> arguments1 = new ArrayList<>(6);
    arguments1.add(Tele(vars("x"), Index(4)));
    arguments1.add(TypeArg(Apps(Index(3), Index(0), Index(1))));
    def.addConstructor(new Constructor(0, new Utils.Name("con1"), def, Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments1));

    List<TypeArgument> arguments2 = new ArrayList<>(6);
    arguments2.add(Tele(false, vars("y"), Index(3)));
    arguments2.add(TypeArg(Apps(Index(3), Index(2), Index(0))));
    def.addConstructor(new Constructor(1, new Utils.Name("con2"), def, Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments2));

    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(moduleLoader, def.getParent(), def, localContext);
    assertNotNull(typedDef);
    for (int i = 0; i < def.getConstructors().size(); i++) {
      TypeChecking.typeCheckConstructor(moduleLoader, typedDef, def.getConstructors().get(i), localContext, i);
    }
    TypeChecking.typeCheckDataEnd(moduleLoader, def.getParent(), def, typedDef, localContext, false);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
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
    ModuleLoader moduleLoader = new ModuleLoader();
    DataDefinition def = new DataDefinition(new Utils.Name("D"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, null, parameters);

    List<TypeArgument> arguments1 = new ArrayList<>(3);
    arguments1.add(Tele(vars("X"), Universe(5, 1)));
    arguments1.add(TypeArg(Index(0)));
    def.addConstructor(new Constructor(0, new Utils.Name("con1"), def, Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments1));

    List<TypeArgument> arguments2 = new ArrayList<>(4);
    arguments2.add(Tele(vars("Y"), Universe(3, 2)));
    arguments2.add(TypeArg(Index(1)));
    arguments2.add(TypeArg(Index(1)));
    def.addConstructor(new Constructor(1, new Utils.Name("con2"), def, Abstract.Definition.DEFAULT_PRECEDENCE, null, arguments2));

    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(moduleLoader, def.getParent(), def, localContext);
    assertNotNull(typedDef);
    for (int i = 0; i < def.getConstructors().size(); i++) {
      TypeChecking.typeCheckConstructor(moduleLoader, typedDef, def.getConstructors().get(i), localContext, i);
    }
    TypeChecking.typeCheckDataEnd(moduleLoader, def.getParent(), def, typedDef, localContext, false);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertFalse(typedDef.hasErrors());
    assertEquals(Pi(parameters, Universe(6, 7)), typedDef.getType());
    assertEquals(2, typedDef.getConstructors().size());

    assertEquals(Pi(arguments1, Apps(DefCall(typedDef), Index(2))), typedDef.getConstructors().get(0).getType());
    assertEquals(Pi(arguments2, Apps(DefCall(typedDef), Index(3))), typedDef.getConstructors().get(1).getType());
  }

  @Test
  public void constructor() {
    // \data D (A : \Type0) = con (B : \Type1) A B |- con Nat zero zero : D Nat
    ModuleLoader moduleLoader = new ModuleLoader();
    DataDefinition def = new DataDefinition(new Utils.Name("D"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    Constructor con = new Constructor(0, new Utils.Name("con"), def, Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))));
    def.addConstructor(con);

    Expression expr = Apps(DefCall(con), Nat(), Zero(), Zero());
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), null, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertNotNull(result);
    assertEquals(Apps(DefCall(def), Nat()), result.type);
  }

  @Test
  public void constructorInfer() {
    // \data D (A : \Type0) = con (B : \Type1) A B, f : D (Nat -> Nat) -> Nat |- f (con Nat (\lam x => x) zero) : Nat
    ModuleLoader moduleLoader = new ModuleLoader();
    DataDefinition def = new DataDefinition(new Utils.Name("D"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    Constructor con = new Constructor(0, new Utils.Name("con"), def, Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("B"), Universe(1)), TypeArg(Index(1)), TypeArg(Index(1))));
    def.addConstructor(con);

    Expression expr = Apps(Index(0), Apps(DefCall(con), Nat(), Lam("x", Index(0)), Zero()));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Apps(DefCall(def), Pi(Nat(), Nat())), Nat())));

    CheckTypeVisitor.OKResult result = expr.checkType(localContext, null, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertNotNull(result);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void constructorConst() {
    // \data D (A : \Type0) = con A, f : (Nat -> D Nat) -> Nat -> Nat |- f con : Nat -> Nat
    ModuleLoader moduleLoader = new ModuleLoader();
    DataDefinition def = new DataDefinition(new Utils.Name("D"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, null, args(Tele(vars("A"), Universe(0))));
    Constructor con = new Constructor(0, new Utils.Name("con"), def, Abstract.Definition.DEFAULT_PRECEDENCE, null, args(TypeArg(Index(0))));
    def.addConstructor(con);

    Expression expr = Apps(Index(0), DefCall(con));
    List<Binding> localContext = new ArrayList<>(1);
    localContext.add(new TypedBinding("f", Pi(Pi(Nat(), Apps(DefCall(def), Nat())), Pi(Nat(), Nat()))));

    CheckTypeVisitor.OKResult result = expr.checkType(localContext, null, moduleLoader);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertNotNull(result);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void patternData() {
    parseDefs(dummyModuleLoader,
        "\\data C | _ => c1 | c2 \\data D (c : C) | D (c1) => d1  | D (c2) => d2");
  }

  @Test
  public void patternVector() {
    parseDefs(dummyModuleLoader,
        "\\data Vec (A : \\Type0) (n : Nat) | Vec _ (zero) => Nil | Vec _ (suc m) => Cons A (Vec A m)");
  }

  @Test
  public void patternDepParams() {
    parseDefs(dummyModuleLoader,
        "\\data D (n : Nat) (p : n = n) | D (zero) _ => d \\data C {n : Nat} {p : n = n} (D n p) | C {(zero)} (d) => c");
  }

  @Test(expected = AssertionError.class)
  public void patternDepParamsError() {
    parseDefs(dummyModuleLoader,
        "\\data D (n : Nat) (p : n = n) | D (zero) _ => d \\data C {n : Nat} {p : n = n} (D n p) | C (d) => c");
  }

  @Test
  public void patternConstructorDepArg() {
    ClassDefinition def = parseDefs(dummyModuleLoader,
        "\\data D (n : Nat) (p : n = n) | D (zero) _ => d \\data C {n : Nat} {p : n = n} (D n p) | C {(zero)} (d) => c (p = p)");
  }

  @Test
  public void patternNested() {
    parseDefs(dummyModuleLoader, "\\data C (n : Nat) | C (suc (suc n)) => c2 (n = n)");
  }

  @Test
  public void patternDataLE() {
    parseDefs(dummyModuleLoader, "\\data LE (n m : Nat) | LE (zero) m => LE-zero ; | LE (suc n) (suc m) => LE-suc (LE n m)");
  }

  @Test(expected = AssertionError.class)
  public void patternImplicit() {
    parseDefs(dummyModuleLoader, "\\data D (A : Nat) | D {A} => d");
  }

  @Test
  public void patternConstructorCall() {
    parseDefs(dummyModuleLoader, "\\data D {n : Nat} (n = n) | D {zero} p => d \\function test => d");
  }
}
