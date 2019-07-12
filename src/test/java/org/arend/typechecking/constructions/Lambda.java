package org.arend.typechecking.constructions;

import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.sort.Sort;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.arend.ExpressionFactory.*;
import static org.arend.ExpressionFactory.Pi;
import static org.arend.ExpressionFactory.Universe;
import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.arend.core.expr.ExpressionFactory.singleParams;
import static org.arend.typechecking.Matchers.typeMismatchError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Lambda extends TypeCheckingTestCase {
  @Test
  public void id() {
    TypecheckingResult result = typeCheckExpr("\\lam x => x", Pi(Nat(), Nat()));
    assertNotNull(result);
  }

  @Test
  public void idTyped() {
    TypecheckingResult result = typeCheckExpr("\\lam (x : Nat) => x", null);
    assertNotNull(result);
  }

  @Test
  public void constant() {
    TypecheckingResult result = typeCheckExpr("\\lam x y => x", Pi(Nat(), Pi(Nat(), Nat())));
    assertNotNull(result);
  }

  @Test
  public void constantSep() {
    TypecheckingResult result = typeCheckExpr("\\lam x => \\lam y => x", Pi(singleParams(true, vars("x", "y"), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void constantTyped() {
    TypecheckingResult result = typeCheckExpr("\\lam (x y : Nat) => x", null);
    assertNotNull(result);
  }

  @Test
  public void idImplicit() {
    TypecheckingResult result = typeCheckExpr("\\lam {x} => x", Pi(singleParams(false, Collections.singletonList(null), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void skipImplicit() {
    TypecheckingResult result = typeCheckExpr("\\lam x => x", Pi(singleParams(false, Collections.singletonList(null), Nat()), Pi(singleParams(true, Collections.singletonList(null), Nat()), Nat())));
    assertNotNull(result);
  }

  @Test
  public void skipImplicitTyped() {
    TypecheckingResult result = typeCheckExpr("\\lam (x : Nat) => x", Pi(singleParams(false, Collections.singletonList(null), Nat()), Pi(singleParams(true, Collections.singletonList(null), Nat()), Nat())));
    assertNotNull(result);
  }

  @Test
  public void implicitTyped() {
    TypecheckingResult result = typeCheckExpr("\\lam {y : Nat} (x : Nat) => y", Pi(singleParams(false, Collections.singletonList(null), Nat()), Pi(singleParams(true, Collections.singletonList(null), Nat()), Nat())));
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
    TypecheckingResult result = typeCheckExpr("\\lam (x y : Nat) => \\lam z => z", Pi(singleParams(true, vars("x", "y", "z"), Nat()), Nat()));
    assertNotNull(result);
  }

  @Test
  public void lambda2() {
    SingleDependentLink param = singleParams(true, vars("x", "y"), Nat());
    TypecheckingResult result = typeCheckExpr("\\lam (x y z w : Nat) => path (\\lam _ => y)", Pi(Nat(), Pi(param, Pi(Nat(), FunCall(Prelude.PATH_INFIX, Sort.PROP, Nat(), Ref(param), Ref(param))))));
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
    TypecheckingResult result = typeCheckExpr("\\lam (x : \\oo-Type1 -> \\oo-Type2) (y : \\oo-Type0) => x y", null);
    assertEquals(result.type, Pi(singleParam(null, Pi(Universe(1), Universe(2))), Pi(singleParam(null, Universe(0)), Universe(2))));
  }

  @Test
  public void typedLambda() {
    typeCheckExpr("\\lam (X : \\Type1) => X", Pi(singleParams(true, vars("x"), Universe(0)), Universe(1)));
  }

  @Test
  public void typedLambdaError() {
    typeCheckExpr("\\lam (X : \\Type0) => X", Pi(singleParams(true, vars("x"), Universe(1)), Universe(1)), 1);
    assertThatErrorsAre(typeMismatchError());
  }
}
