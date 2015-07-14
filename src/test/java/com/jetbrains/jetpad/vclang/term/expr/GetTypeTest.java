package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
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
    ClassDefinition def = parseDefs(moduleLoader, "\\data List (A : \\Type0) | nil | cons A (List A) \\function test => cons 0 nil");
    assertEquals(Apps(DefCall(def.getPublicField("List")), Nat()), def.getPublicField("test").getType());
    assertEquals(Apps(DefCall(def.getPublicField("List")), Nat()), ((FunctionDefinition) def.getPublicField("test")).getTerm().getType(new ArrayList<Expression>(0)));
  }

  @Test
  public void nilConstructorTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = parseDefs(moduleLoader, "\\data List (A : \\Type0) | nil | cons A (List A) \\function test => (List Nat).nil");
    assertEquals(Apps(DefCall(def.getPublicField("List")), Nat()), def.getPublicField("test").getType());
    assertEquals(Apps(DefCall(def.getPublicField("List")), Nat()), ((FunctionDefinition) def.getPublicField("test")).getTerm().getType(new ArrayList<Expression>(0)));
  }

  @Test
  public void classExtTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = parseDefs(moduleLoader, "\\class Test { \\function A : \\Type0 \\function a : A } \\function test => Test { \\override A => Nat }");
    assertEquals(Universe(1), def.getPublicField("Test").getType());
    assertEquals(Universe(0, Universe.Type.SET), def.getPublicField("test").getType());
    assertEquals(Universe(0, Universe.Type.SET), ((FunctionDefinition) def.getPublicField("test")).getTerm().getType(new ArrayList<Expression>(0)));
  }

  @Test
  public void lambdaTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition def = parseDef(moduleLoader, "\\function test => \\lam (f : Nat -> Nat) => f 0");
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), def.getType());
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), ((FunctionDefinition) def).getTerm().getType(new ArrayList<Expression>(1)));
  }

  @Test
  public void fieldAccTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    ClassDefinition def = parseDefs(moduleLoader, "\\class C { \\function x : Nat \\function f (p : 0 = x) => p } \\function test (p : Nat -> C) => (p 0).f");
    Expression type = Apps(Apps(DefCall(Prelude.PATH_INFIX), new ArgumentExpression(Nat(), false, true)), Zero(), FieldAcc(Apps(Index(0), Zero()), ((ClassDefinition) def.getPublicField("C")).getPublicField("x")));
    List<Expression> context = new ArrayList<>(1);
    context.add(Pi(Nat(), DefCall(def.getPublicField("C"))));
    assertEquals(Pi(args(Tele(vars("p"), context.get(0))), Pi(type, type)), def.getPublicField("test").getType());
    assertEquals(Pi(type, type), ((FunctionDefinition) def.getPublicField("test")).getTerm().getType(context));
  }

  @Test
  public void tupleTest() {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    Definition def = parseDef(moduleLoader, "\\function test : \\Sigma (x y : Nat) (x = y) => (0, 0, path (\\lam _ => 0))");
    assertEquals(Sigma(args(Tele(vars("x", "y"), Nat()), TypeArg(Apps(DefCall(Prelude.PATH_INFIX), Nat(), Index(1), Index(0))))), ((FunctionDefinition) def).getTerm().getType(new ArrayList<Expression>(0)));
  }
}
