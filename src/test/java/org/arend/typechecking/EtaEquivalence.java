package org.arend.typechecking;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.ext.core.ops.CMP;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.junit.Test;

import static org.arend.ExpressionFactory.ClassCall;
import static org.arend.ExpressionFactory.Ref;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EtaEquivalence extends TypeCheckingTestCase {
  @Test
  public void classesCmp() {
    typeCheckModule(
        "\\record Foo { | foo : Nat | bar : Nat }\n" +
        "\\func f (l : Foo) => \\new Foo { | foo => l.foo | bar => l.bar }");
    assertTrue(getDefinition("f") instanceof FunctionDefinition);
    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    NewExpression newExpr = new NewExpression(null, (ClassCallExpression) f.getResultType());
    ClassCallExpression classCall = ClassCall(newExpr.getClassCall().getDefinition());
    ReferenceExpression refExpr = Ref(f.getParameters());

    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), CMP.EQ, newExpr, refExpr, classCall, null));
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), CMP.EQ, refExpr, newExpr, classCall, null));
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), CMP.GE, newExpr, refExpr, classCall, null));
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), CMP.GE, refExpr, newExpr, classCall, null));
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), CMP.LE, newExpr, refExpr, classCall, null));
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), CMP.LE, refExpr, newExpr, classCall, null));
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
    assertFalse(((Expression) fun.getBody()).isInstance(LamExpression.class));
  }

  @Test
  public void emptyClass() {
    typeCheckModule(
      "\\record Unit\n" +
      "\\func f (x : Unit) : x = \\new Unit => idp");
  }

  @Test
  public void emptyClass2() {
    typeCheckModule(
      "\\record Unit\n" +
      "\\func f (x y : Unit) : x = y => idp");
  }

  @Test
  public void emptyClass3() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x y : C { | n => 0 }) : x = y => idp");
  }

  @Test
  public void emptyClass4a() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C { | n => 0 }) (y : C) : x = y => idp", 1);
  }

  @Test
  public void emptyClass4b() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C) (y : C { | n => 0 }) : x = y => idp", 1);
  }

  @Test
  public void unitClass() {
    typeCheckModule(
      "\\record C { | n : Nat }\n" +
      "\\func f (x : C) (y : C { | n => x.n }) : x = y => idp");
  }

  @Test
  public void unitClass2() {
    typeCheckModule(
      "\\record C { | n : Nat | m : Nat }\n" +
      "\\func f (x : C { | n => 3 }) (y : C { | n => 3 | m => x.m }) : x = y => idp");
  }

  @Test
  public void unitClass3() {
    typeCheckModule(
      "\\record C {| m : Nat | n : Nat  }\n" +
      "\\func f (x : C) (y : C { | m => x.m }) : x = y => idp", 1);
  }

  @Test
  public void unitClass4() {
    typeCheckModule(
      "\\record C { | n : Nat | m : Nat }\n" +
      "\\func f (x : C { | n => 3 }) (y : C { | n => 3 | m => x.n }) : x = y => idp", 1);
  }

  @Test
  public void unitClass5() {
    typeCheckModule(
      "\\record C (n : Nat)\n" +
      "\\func f (x : C 0) (y : C 1) : x = y => idp", 1);
  }

  @Test
  public void unitClass6() {
    typeCheckModule(
      "\\record C (n : Nat)\n" +
      "\\func f (x : C 0) (y : C 1) : x = {C} y => idp", 1);
  }

  @Test
  public void unitClass7() {
    typeCheckModule(
      "\\record C (n : Nat)\n" +
      "\\func f (x : C 0) (y : C 0) : x = {C} y => idp");
  }

  @Test
  public void unitClass8() {
    typeCheckModule(
      "\\record C (n m : Nat)\n" +
      "\\func f (x : C 0) (y : C 0) : x = {C} y => idp", 1);
  }

  @Test
  public void typedComparison1() {
    typeCheckModule(
      "\\record C (n : Nat)\n" +
      "\\record D (m : Nat) \\extends C\n" +
      "\\func f (x : D 0 1) (y : D 0 2) : x = {C} y => idp");
  }

  @Test
  public void typedComparison2() {
    typeCheckModule(
      "\\record C (n : Nat)\n" +
      "\\record D (m : Nat) \\extends C\n" +
      "\\func f (x : C) : \\new D { | C => x | m => 0 } = {C} x => idp\n" +
      "\\func g (x : C) : x = {C} \\new D { | C => x | m => 0 } => idp");
  }

  @Test
  public void sigmaTest() {
    typeCheckModule("\\func f (x : \\Sigma Nat Nat) : x = (x.1,x.2) => idp");
  }

  @Test
  public void sigmaUnitTest() {
    typeCheckModule("\\func f (x y : \\Sigma) : x = y => idp");
  }
}
