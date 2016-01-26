package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class Lambda {
  @Test
  public void id() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x => x", Pi(Nat(), Nat()));
    assertNotNull(result);
  }

  @Test
  public void idTyped() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x : Nat) => x", null);
    assertNotNull(result);
  }

  @Test
  public void constant() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x y => x", Pi(Nat(), Pi(Nat(), Nat())));
    assertNotNull(result);
  }

  @Test
  public void constantSep() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x => \\lam y => x", Pi(param(true, vars("x", "y"), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void constantTyped() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x y : Nat) => x", null);
    assertNotNull(result);
  }

  @Test
  public void idImplicit() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam {x} => x", Pi(param(false, (String) null, Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void idImplicitError() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam {x} => x", Pi(Nat(), Nat()), 1);
    assertNull(result);
  }

  @Test
  public void constantImplicitError() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x {y} => x", Pi(Nat(), Pi(Nat(), Nat())), 1);
    assertNull(result);
  }

  @Test
  public void constantImplicitTeleError() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x {y} => x", Pi(param(true, vars("x", "y"), Nat()), Nat()), 1);
    assertNull(result);
  }

  @Test
  public void constantImplicitTypeError() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x y => x", Pi(Nat(), Pi(param(false, (String) null, Nat()), Nat())), 1);
    assertNull(result);
  }
}
