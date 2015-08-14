package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class GetTypeTest {
  @Test
  public void constructorTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = parseDefs(moduleLoader, "\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => cons 0 nil");
    assertEquals(Apps(DefCall(def.getNamespace().getMember("List")), Nat()), def.getNamespace().getMember("test").getType());
    assertEquals(Apps(DefCall(def.getNamespace().getMember("List")), Nat()), ((FunctionDefinition) def.getNamespace().getMember("test")).getTerm().getType(new ArrayList<Binding>(0)));
  }

  @Test
  public void nilConstructorTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = parseDefs(moduleLoader, "\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => (List Nat).nil");
    assertEquals(Apps(DefCall(def.getNamespace().getMember("List")), Nat()), def.getNamespace().getMember("test").getType());
    assertEquals(Apps(DefCall(def.getNamespace().getMember("List")), Nat()), ((FunctionDefinition) def.getNamespace().getMember("test")).getTerm().getType(new ArrayList<Binding>(0)));
  }

  @Test
  public void classExtTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = parseDefs(moduleLoader, "\\static \\class Test { \\function A : \\Type0 \\function a : A } \\static \\function test => Test { \\override A => Nat }");
    assertEquals(Universe(1), def.getNamespace().getMember("Test").getType());
    assertEquals(Universe(0, Universe.Type.SET), def.getNamespace().getMember("test").getType());
    assertEquals(Universe(0, Universe.Type.SET), ((FunctionDefinition) def.getNamespace().getMember("test")).getTerm().getType(new ArrayList<Binding>(0)));
  }

  @Test
  public void lambdaTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition def = parseDef(moduleLoader, "\\static \\function test => \\lam (f : Nat -> Nat) => f 0");
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), def.getType());
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), ((FunctionDefinition) def).getTerm().getType(new ArrayList<Binding>(1)));
  }

  @Test
  public void lambdaTest2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition def = parseDef(moduleLoader, "\\static \\function test => \\lam (A : \\Type0) (x : A) => x");
    assertEquals(Pi(args(Tele(vars("A"), Universe(0)), Tele(vars("x"), Index(0))), Index(1)), def.getType());
    assertEquals(Pi(args(Tele(vars("A"), Universe(0)), Tele(vars("x"), Index(0))), Index(1)), ((FunctionDefinition) def).getTerm().getType(new ArrayList<Binding>(1)));
  }

  @Test
  public void fieldAccTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = parseDefs(moduleLoader, "\\static \\class C { \\function x : Nat \\function f (p : 0 = x) => p } \\static \\function test (p : Nat -> C) => (p 0).f");
    Expression type = Apps(Apps(DefCall(Prelude.PATH_INFIX), new ArgumentExpression(Nat(), false, true)), Zero(), DefCall(Apps(Index(0), Zero()), ((ClassDefinition) def.getNamespace().getMember("C")).getLocalNamespace().getMember("x")));
    List<Binding> context = new ArrayList<>(1);
    context.add(new TypedBinding("p", Pi(Nat(), DefCall(def.getNamespace().getMember("C")))));
    assertEquals(Pi(args(Tele(vars("p"), context.get(0).getType())), Pi(type, type)), def.getNamespace().getMember("test").getType());
    assertEquals(Pi(type, type), ((FunctionDefinition) def.getNamespace().getMember("test")).getTerm().getType(context));
  }

  @Test
  public void tupleTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition def = parseDef(moduleLoader, "\\static \\function test : \\Sigma (x y : Nat) (x = y) => (0, 0, path (\\lam _ => 0))");
    assertEquals(Sigma(args(Tele(vars("x", "y"), Nat()), TypeArg(Apps(DefCall(Prelude.PATH_INFIX), Nat(), Index(1), Index(0))))), ((FunctionDefinition) def).getTerm().getType(new ArrayList<Binding>(0)));
  }

  @Test
  public void letTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition def = parseDef(moduleLoader, "\\static \\function test => \\lam (F : Nat -> \\Type0) (f : \\Pi (x : Nat) -> F x) => \\let | x => 0 \\in f x");
    assertEquals(Pi(args(Tele(vars("F"), Pi(Nat(), Universe())), Tele(vars("f"), Pi(args(Tele(vars("x"), Nat())), Apps(Index(1), Index(0))))), Apps(Index(1), Zero())),
            ((FunctionDefinition) def).getTerm().getType(new ArrayList<Binding>()));
  }
}
