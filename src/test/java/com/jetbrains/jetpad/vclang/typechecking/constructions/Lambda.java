package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.Collections;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Pi;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Universe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Lambda extends TypeCheckingTestCase {
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
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x => \\lam y => x", Pi(singleParam(true, vars("x", "y"), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void constantTyped() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x y : Nat) => x", null);
    assertNotNull(result);
  }

  @Test
  public void idImplicit() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam {x} => x", Pi(singleParam(false, Collections.singletonList(null), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void lambda1() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x y : Nat) => \\lam z => z", Pi(singleParam(true, vars("x", "y", "z"), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void lambda2() {
    SingleDependentLink param = singleParam(true, vars("x", "y"), Nat());
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x y z w : Nat) => path (\\lam _ => y)", Pi(Nat(), Pi(param, Pi(Nat(), FunCall(Prelude.PATH_INFIX, new Level(0), new Level(0), Nat(), Reference(param), Reference(param))))));
    assertNotNull(result);
  }

  @Test
  public void idImplicitError() {
    typeCheckExpr("\\lam {x} => x", Pi(Nat(), Nat()), 1);
  }

  @Test
  public void constantImplicitError() {
    typeCheckExpr("\\lam x {y} => x", Pi(Nat(), Pi(Nat(), Nat())), 1);
  }

  @Test
  public void constantImplicitTeleError() {
    typeCheckExpr("\\lam x {y} => x", Pi(singleParam(true, vars("x", "y"), Nat()), Nat()), 1);
  }

  @Test
  public void constantImplicitTypeError() {
    typeCheckExpr("\\lam x y => x", Pi(Nat(), Pi(singleParam(false, Collections.singletonList(null), Nat()), Nat())), 1);
  }

  @Test
  public void lambdaUniverse() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x : \\oo-Type1 -> \\oo-Type2) (y : \\oo-Type0) => x y", null);
    assertEquals(result.type, Pi(singleParam(null, Pi(Universe(1), Universe(2))), Pi(singleParam(null, Universe(0)), Universe(2))));
  }
}
