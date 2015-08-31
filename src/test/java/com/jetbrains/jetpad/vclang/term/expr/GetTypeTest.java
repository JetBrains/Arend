package com.jetbrains.jetpad.vclang.term.expr;

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
    ClassDefinition def = parseDefs("\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => cons 0 nil");
    assertEquals(Apps(DefCall(def.getNamespace().getDefinition("List")), Nat()), def.getNamespace().getDefinition("test").getType());
    assertEquals(Apps(DefCall(def.getNamespace().getDefinition("List")), Nat()), ((FunctionDefinition) def.getNamespace().getDefinition("test")).getTerm().getType(new ArrayList<Binding>(0)));
  }

  @Test
  public void nilConstructorTest() {
    ClassDefinition def = parseDefs("\\static \\data List (A : \\Type0) | nil | cons A (List A) \\static \\function test => (List Nat).nil");
    assertEquals(Apps(DefCall(def.getNamespace().getDefinition("List")), Nat()), def.getNamespace().getDefinition("test").getType());
    assertEquals(Apps(DefCall(def.getNamespace().getDefinition("List")), Nat()), ((FunctionDefinition) def.getNamespace().getDefinition("test")).getTerm().getType(new ArrayList<Binding>(0)));
  }

  @Test
  public void classExtTest() {
    ClassDefinition def = parseDefs("\\static \\class Test { \\function A : \\Type0 \\function a : A } \\static \\function test => Test { \\override A => Nat }");
    assertEquals(Universe(1), def.getNamespace().getDefinition("Test").getType());
    assertEquals(Universe(0, Universe.Type.SET), def.getNamespace().getDefinition("test").getType());
    assertEquals(Universe(0, Universe.Type.SET), ((FunctionDefinition) def.getNamespace().getDefinition("test")).getTerm().getType(new ArrayList<Binding>(0)));
  }

  @Test
  public void lambdaTest() {
    Definition def = parseDef("\\static \\function test => \\lam (f : Nat -> Nat) => f 0");
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), def.getType());
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), ((FunctionDefinition) def).getTerm().getType(new ArrayList<Binding>(1)));
  }

  @Test
  public void lambdaTest2() {
    Definition def = parseDef("\\static \\function test => \\lam (A : \\Type0) (x : A) => x");
    assertEquals(Pi(args(Tele(vars("A"), Universe(0)), Tele(vars("x"), Index(0))), Index(1)), def.getType());
    assertEquals(Pi(args(Tele(vars("A"), Universe(0)), Tele(vars("x"), Index(0))), Index(1)), ((FunctionDefinition) def).getTerm().getType(new ArrayList<Binding>(1)));
  }

  @Test
  public void fieldAccTest() {
    ClassDefinition def = parseDefs("\\static \\class C { \\function x : Nat \\function f (p : 0 = x) => p } \\static \\function test (p : Nat -> C) => (p 0).f");
    Expression type = Apps(Apps(DefCall(Prelude.PATH_INFIX), new ArgumentExpression(Nat(), false, true)), Zero(), DefCall(Apps(Index(0), Zero()), ((ClassDefinition) def.getNamespace().getDefinition("C")).getLocalNamespace().getDefinition("x")));
    List<Binding> context = new ArrayList<>(1);
    context.add(new TypedBinding("p", Pi(Nat(), DefCall(def.getNamespace().getDefinition("C")))));
    assertEquals(Pi(args(Tele(vars("p"), context.get(0).getType())), Pi(type, type)), def.getNamespace().getDefinition("test").getType());
    assertEquals(Pi(type, type), ((FunctionDefinition) def.getNamespace().getDefinition("test")).getTerm().getType(context));
  }

  @Test
  public void tupleTest() {
    Definition def = parseDef("\\static \\function test : \\Sigma (x y : Nat) (x = y) => (0, 0, path (\\lam _ => 0))");
    assertEquals(Sigma(args(Tele(vars("x", "y"), Nat()), TypeArg(Apps(DefCall(Prelude.PATH_INFIX), Nat(), Index(1), Index(0))))), ((FunctionDefinition) def).getTerm().getType(new ArrayList<Binding>(0)));
  }

  @Test
  public void letTest() {
    Definition def = parseDef("\\static \\function test => \\lam (F : Nat -> \\Type0) (f : \\Pi (x : Nat) -> F x) => \\let | x => 0 \\in f x");
    assertEquals(Pi(args(Tele(vars("F"), Pi(Nat(), Universe())), Tele(vars("f"), Pi(args(Tele(vars("x"), Nat())), Apps(Index(1), Index(0))))), Apps(Index(1), Zero())),
            ((FunctionDefinition) def).getTerm().getType(new ArrayList<Binding>()));
  }

  @Test
  public void patternConstructor1() {
    ClassDefinition def = parseDefs(
        "\\data C (n : Nat) | C (zero) => c1 | C (suc n) => c2 Nat");
    assertEquals(Apps(DefCall(def.getField("C")), Zero()), ((DataDefinition) def.getField("C")).getConstructor("c1").getType());
    assertEquals(Pi("n", Nat(), Apps(DefCall(def.getField("C")), Suc(Index(1)))), ((DataDefinition) def.getField("C")).getConstructor("c2").getType());
  }

  @Test
  public void patternConstructor2() {
    ClassDefinition def = parseDefs(
        "\\data Vec (A : \\Type0) (n : Nat) | Vec _ (zero) => Nil | Vec _ (suc n) => Cons A (Vec A n)" +
        "\\data D (n : Nat) (Vec Nat n) | D (zero) _ => dzero | D (suc n) _ => done");
    DataDefinition vec = (DataDefinition) def.getField("Vec");
    DataDefinition d = (DataDefinition) def.getField("D");
    assertEquals(Apps(DefCall(d), Zero(), Index(0)), d.getConstructor("dzero").getType());
    assertEquals(Apps(DefCall(d), Suc(Index(1)), Index(0)), d.getConstructor("done").getType());
    assertEquals(Pi("x", Index(1), Pi("xs", Apps(DefCall(vec), Index(2), Index(1)), Apps(DefCall(vec), Index(3), Suc(Index(2))))), vec.getConstructor("Cons").getType());
  }

  @Test
  public void patternConstructor3() {
    ClassDefinition def = parseDefs(
        "\\data D | d \\Type0" +
            "\\data C D | C (d A) => c A");
    DataDefinition d = (DataDefinition) def.getField("D");
    DataDefinition c = (DataDefinition) def.getField("C");
    assertEquals(Pi("x", Index(0), Apps(DefCall(c), Apps(DefCall(d.getConstructor("d")), Index(1)))), c.getConstructor("c").getType());
  }

  @Test
  public void patternConstructorDep() {
    ClassDefinition def = parseDefs(
        "\\data Box (n : Nat) | box" +
            "\\data D (n : Nat) (Box n) | D (zero) _ => d");
    DataDefinition d = (DataDefinition) def.getField("D");
    assertEquals(Apps(DefCall(d), Zero(), Index(0)), d.getConstructor("d").getType());
  }
}
