package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.ExpressionFactory.Pi;
import static com.jetbrains.jetpad.vclang.ExpressionFactory.Universe;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Nat;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.singleParams;
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
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x => \\lam y => x", Pi(singleParams(true, vars("x", "y"), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void constantTyped() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x y : Nat) => x", null);
    assertNotNull(result);
  }

  @Test
  public void idImplicit() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam {x} => x", Pi(singleParams(false, Collections.singletonList(null), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void skipImplicit() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam x => x", Pi(singleParams(false, Collections.singletonList(null), Nat()), Pi(singleParams(true, Collections.singletonList(null), Nat()), Nat())));
    assertNotNull(result);
  }

  @Test
  public void skipImplicitTyped() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x : Nat) => x", Pi(singleParams(false, Collections.singletonList(null), Nat()), Pi(singleParams(true, Collections.singletonList(null), Nat()), Nat())));
    assertNotNull(result);
  }

  @Test
  public void implicitTyped() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam {y : Nat} (x : Nat) => y", Pi(singleParams(false, Collections.singletonList(null), Nat()), Pi(singleParams(true, Collections.singletonList(null), Nat()), Nat())));
    assertNotNull(result);
  }

  @Test
  public void implicitTypedError() {
    typeCheckExpr("\\lam {y : Nat} (x : Nat) => y", Pi(singleParams(true, Arrays.asList(null, null), Nat()), Nat()), 1);
  }

  @Test
  public void implicitTypedError2() {
    typeCheckExpr("\\lam {y : Nat} (x : Nat) => y", Pi(singleParams(true, Collections.singletonList(null), Nat()), Pi(singleParams(true, Collections.singletonList(null), Nat()), Nat())), 1);
  }

  @Test
  public void explicitTyped() {
    typeCheckExpr("\\lam (y : Nat) (x : Nat) => y", Pi(singleParams(false, Collections.singletonList(null), Nat()), Pi(singleParams(true, Collections.singletonList(null), Nat()), Nat())), 1);
  }

  @Test
  public void lambda1() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x y : Nat) => \\lam z => z", Pi(singleParams(true, vars("x", "y", "z"), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void lambda2() {
    SingleDependentLink param = singleParams(true, vars("x", "y"), Nat());
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x y z w : Nat) => path (\\lam _ => y)", Pi(Nat(), Pi(param, Pi(Nat(), FunCall(Prelude.PATH_INFIX, Sort.PROP, Nat(), Ref(param), Ref(param))))));
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
    typeCheckExpr("\\lam x {y} => x", Pi(singleParams(true, vars("x", "y"), Nat()), Nat()), 1);
  }

  @Test
  public void constantImplicitTypeError() {
    typeCheckExpr("\\lam x y => x", Pi(Nat(), Pi(singleParams(false, Collections.singletonList(null), Nat()), Nat())), 1);
  }

  @Test
  public void lambdaUniverse() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (x : \\oo-Type1 -> \\oo-Type2) (y : \\oo-Type0) => x y", null);
    assertEquals(result.type, Pi(singleParam(null, Pi(Universe(1), Universe(2))), Pi(singleParam(null, Universe(0)), Universe(2))));
  }
}
