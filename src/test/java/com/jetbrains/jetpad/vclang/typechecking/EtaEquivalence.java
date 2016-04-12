package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Level;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Reference;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EtaEquivalence {
  @Test
  public void classesEq() {
    List<Binding> context = new ArrayList<>(1);
    context.add(new TypedBinding("l", Level()));
    CheckTypeVisitor.Result result1 = typeCheckExpr(context, "\\new Level { PLevel => l.PLevel | HLevel => l.HLevel }", null);
    assertNotNull(result1);
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, result1.expression, Reference(context.get(0)), null));
  }

  @Test
  public void classesGe() {
    List<Binding> context = new ArrayList<>(1);
    context.add(new TypedBinding("l", Level()));
    CheckTypeVisitor.Result result1 = typeCheckExpr(context, "\\new Level { PLevel => sucLvl l.PLevel | HLevel => sucCNat l.HLevel }", null);
    assertNotNull(result1);
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.GE, result1.expression, Reference(context.get(0)), null));
  }

  @Test
  public void classesLe() {
    List<Binding> context = new ArrayList<>(1);
    context.add(new TypedBinding("l", Level()));
    CheckTypeVisitor.Result result1 = typeCheckExpr(context, "\\new Level { PLevel => sucLvl l.PLevel | HLevel => sucCNat l.HLevel }", null);
    assertNotNull(result1);
    assertTrue(CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.LE, Reference(context.get(0)), result1.expression, null));
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
}
