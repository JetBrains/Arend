package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.LamExpression;
import com.jetbrains.jetpad.vclang.core.expr.NewExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.Ref;
import static org.junit.Assert.assertTrue;

public class EtaEquivalence extends TypeCheckingTestCase {
  @Test
  public void classesEq() {
    typeCheckModule(
        "\\record Foo { | foo : Nat | bar : Nat }\n" +
        "\\func f (l : Foo) => \\new Foo { | foo => l.foo | bar => l.bar }");
    assertTrue(getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, new NewExpression((ClassCallExpression) f.getResultType()), Ref(f.getParameters()), null));
  }

  @Test
  public void classesGe() {
    typeCheckModule(
        "\\record Foo { | foo : Nat | bar : Nat }\n" +
        "\\func f (l : Foo) => \\new Foo { | foo => l.foo | bar => l.bar }");
    assertTrue(getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.GE, new NewExpression((ClassCallExpression) f.getResultType()), Ref(f.getParameters()), null));
  }

  @Test
  public void classesLe() {
    typeCheckModule(
        "\\record Foo { | foo : Nat | bar : Nat }\n" +
        "\\func f (l : Foo) => \\new Foo { | foo => l.foo | bar => l.bar }");
    assertTrue(getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.LE, new NewExpression((ClassCallExpression) f.getResultType()), Ref(f.getParameters()), null));
  }

  @Test
  public void pathEtaLeftTest() {
    typeCheckDef("\\func test (p : 0 = 0) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTest() {
    typeCheckDef("\\func test (p : 0 = 0) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void pathEtaLeftTestLevel() {
    typeCheckDef("\\func test (p : Nat = Nat) => (\\lam (x : path (\\lam i => p @ i) = p) => x) (path (\\lam _ => p))");
  }

  @Test
  public void pathEtaRightTestLevel() {
    typeCheckDef("\\func test (p : Nat = Nat) => (\\lam (x : p = p) => x) (path (\\lam _ => path (\\lam i => p @ i)))");
  }

  @Test
  public void onlyDefCallsExpanded() {
    FunctionDefinition fun = (FunctionDefinition) typeCheckDef("\\func f (x : Nat -> Nat) => x");
    assertTrue(!((LeafElimTree) fun.getBody()).getExpression().isInstance(LamExpression.class));
  }

  @Test
  public void emptyClass() {
    typeCheckModule(
      "\\record Unit\n" +
      "\\func f (x : Unit) : x = \\new Unit => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass2() {
    typeCheckModule(
      "\\record Unit\n" +
      "\\func f (x y : Unit) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass3() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x y : C { | n => 0 }) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void emptyClass4a() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C { | n => 0 }) (y : C) : x = y => path (\\lam _ => x)", 1);
  }

  @Test
  public void emptyClass4b() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C) (y : C { | n => 0 }) : x = y => path (\\lam _ => x)", 1);
  }

  @Test
  public void unitClass() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C) (y : C { | n => x.n }) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void unitClass2() {
    typeCheckModule(
      "\\record C { | n : Nat | m : Nat }\n" +
      "\\func f (x : C { | n => 3 }) (y : C { | n => 3 | m => x.m }) : x = y => path (\\lam _ => y)");
  }

  @Test
  public void unitClass3() {
    typeCheckModule(
      "\\record C {| m : Nat | n : Nat  }\n" +
      "\\func f (x : C) (y : C { | m => x.m }) : x = y => path (\\lam _ => y)", 1);
  }

  @Test
  public void unitClass4() {
    typeCheckModule(
      "\\record C { | n : Nat | m : Nat }\n" +
      "\\func f (x : C { | n => 3 }) (y : C { | n => 3 | m => x.n }) : x = y => path (\\lam _ => y)", 1);
  }
}
