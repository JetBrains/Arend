package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.LamExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.Ref;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EtaEquivalence extends TypeCheckingTestCase {
  @Test
  public void classesEq() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Foo { | foo : Nat | bar : Nat }\n" +
        "\\function f (l : Foo) => \\new Foo { foo => l.foo | bar => l.bar }");
    assertNotNull(result.typecheckerState.getTypechecked(result.classDefinition));
    assertTrue(result.getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, ((LeafElimTree) f.getBody()).getExpression(), Ref(f.getParameters()), null));
  }

  @Test
  public void classesGe() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Foo { | foo : Nat | bar : Nat }\n" +
        "\\function f (l : Foo) => \\new Foo { foo => l.foo | bar => l.bar }");
    assertNotNull(result.typecheckerState.getTypechecked(result.classDefinition));
    assertTrue(result.getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.GE, ((LeafElimTree) f.getBody()).getExpression(), Ref(f.getParameters()), null));
  }

  @Test
  public void classesLe() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class Foo { | foo : Nat | bar : Nat }\n" +
        "\\function f (l : Foo) => \\new Foo { foo => l.foo | bar => l.bar }");
    assertNotNull(result.typecheckerState.getTypechecked(result.classDefinition));
    assertTrue(result.getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) result.getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.LE, ((LeafElimTree) f.getBody()).getExpression(), Ref(f.getParameters()), null));
  }
  @Test
  public void pathEtaLeftTest() {
    typeCheckDef("\\function test (p : 0 = 0) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTest() {
    typeCheckDef("\\function test (p : 0 = 0) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void pathEtaLeftTestLevel() {
    typeCheckDef("\\function test (p : Nat = Nat) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTestLevel() {
    typeCheckDef("\\function test (p : Nat = Nat) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void onlyDefCallsExpanded() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\function f (x : Nat -> Nat) => x");
    assertTrue(!((LeafElimTree) fun.getBody()).getExpression().isInstance(LamExpression.class));
  }

  @Test
  public void emptyClass() {
    typeCheckClass(
      "\\class Unit\n" +
      "\\function f (x : Unit) : x = \\new Unit => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass2() {
    typeCheckClass(
      "\\class Unit\n" +
      "\\function f (x y : Unit) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass3() {
    typeCheckClass(
      "\\class C { | n : Nat }\n" +
      "\\function f (x y : C { n => 0 }) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass4a() {
    typeCheckClass(
      "\\class C { | n : Nat }\n" +
      "\\function f (x : C { n => 0 }) (y : C) : x = y => path (\\lam _ => x)", 1);
  }

  @Test
  public void emptyClass4b() {
    typeCheckClass(
      "\\class C { | n : Nat }\n" +
      "\\function f (x : C) (y : C { n => 0 }) : x = y => path (\\lam _ => x)", 1);
  }

  @Test
  public void unitClass() {
    typeCheckClass(
      "\\class C { | n : Nat }\n" +
      "\\function f (x : C) (y : C { n => x.n }) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void unitClass2() {
    typeCheckClass(
      "\\class C { | n : Nat | m : Nat }\n" +
      "\\function f (x : C { n => 3 }) (y : C { n => 3 | m => x.m }) : x = y => path (\\lam _ => y)");
  }

  @Test
  public void unitClass3() {
    typeCheckClass(
      "\\class C {| m : Nat | n : Nat  }\n" +
      "\\function f (x : C) (y : C { m => x.m }) : x = y => path (\\lam _ => y)", 1);
  }

  @Test
  public void unitClass4() {
    typeCheckClass(
      "\\class C { | n : Nat | m : Nat }\n" +
      "\\function f (x : C { n => 3 }) (y : C { n => 3 | m => x.n }) : x = y => path (\\lam _ => y)", 1);
  }
}
