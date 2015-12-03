package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.*;

public class ExpressionTest {
  @Test
  public void typeCheckingLam() {
    // \x. x : Nat -> Nat
    typeCheckExpr("\\lam x => x", Pi(Nat(), Nat()));
  }

  @Test
  public void typeCheckingLamError() {
    // \x. x : Nat -> Nat -> Nat
    typeCheckExpr("\\lam x => x", Pi(Nat(), Pi(Nat(), Nat())), 1);
  }

  @Test
  public void typeCheckingId() {
    // \X x. x : (X : Type0) -> X -> X
    typeCheckExpr("\\lam X x => x", Pi("X", Universe(0), Pi(Index(0), Index(0))));
  }

  @Test
  public void typeCheckingIdError() {
    // \X x. X : (X : Type0) -> X -> X
    ListErrorReporter errorReporter = new ListErrorReporter();
    typeCheckExpr("\\lam X x => X", Pi("X", Universe(0), Pi(Index(0), Index(0))), errorReporter);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void typeCheckingApp() {
    // \x y. y (y x) : Nat -> (Nat -> Nat) -> Nat
    typeCheckExpr("\\lam x y => y (y x)", Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat())));
  }

  @Test
  public void typeCheckingAppIndex() {
    // \x y. y (y x) : N -> (N -> N) -> N
    Expression expr = Lam("x", Lam("y", Apps(Index(0), Apps(Index(0), Index(1)))));
    ListErrorReporter errorReporter = new ListErrorReporter();
    expr.checkType(new ArrayList<Binding>(), Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat())), errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void typeCheckingAppPiIndex() {
    // \f g. g zero (f zero) : (f : (x : N) -> N x) -> ((x : N) -> N x -> N (f x)) -> N (f zero)
    Expression expr = Lam("f", Lam("g", Apps(Index(0), Zero(), Apps(Index(1), Zero()))));
    Expression type = Pi("f", Pi("x", Nat(), Apps(Nat(), Index(0))), Pi(Pi("x", Nat(), Pi(Apps(Nat(), Index(0)), Apps(Nat(), Apps(Index(1), Index(0))))), Apps(Nat(), Apps(Index(0), Zero()))));
    ListErrorReporter errorReporter = new ListErrorReporter();
    expr.checkType(new ArrayList<Binding>(), type, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void typeCheckingAppLamPiIndex() {
    // \f h. h (\k -> k (suc zero)) : (f : (g : N -> N) -> N (g zero)) -> ((z : (N -> N) -> N) -> N (f (\x. z (\_. x)))) -> N (f (\x. x))
    Expression expr = Lam("f", Lam("h", Apps(Index(0), Lam("k", Apps(Index(0), Apps(Suc(), Zero()))))));
    Expression type = Pi("f", Pi("g", Pi(Nat(), Nat()), Apps(Nat(), Apps(Index(0), Zero()))), Pi(Pi("z", Pi(Pi(Nat(), Nat()), Nat()), Apps(Nat(), Apps(Index(1), Lam("x", Apps(Index(1), Lam("_", Index(1))))))), Apps(Nat(), Apps(Index(0), Lam("x", Index(0))))));
    ListErrorReporter errorReporter = new ListErrorReporter();
    expr.checkType(new ArrayList<Binding>(), type, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void typeCheckingInferPiIndex() {
    // (X : Type1) -> X -> X : Type2
    Expression expr = Pi("X", Universe(1), Pi(Index(0), Index(0)));
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertEquals(Universe(2), expr.checkType(new ArrayList<Binding>(), null, errorReporter).type);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void typeCheckingUniverse() {
    // (f : Type1 -> Type1) -> f Type1
    Expression expr = Pi("f", Pi(Universe(1), Universe(1)), Apps(Index(0), Universe(1)));
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertEquals(null, expr.checkType(new ArrayList<Binding>(), null, errorReporter));
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void typeCheckingTwoErrors() {
    // f : Nat -> Nat -> Nat |- f S (f 0 S) : Nat
    Expression expr = Apps(Index(0), Suc(), Apps(Index(0), Zero(), Suc()));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(Nat(), Pi(Nat(), Nat()))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.checkType(defs, null, errorReporter));
    assertEquals(2, errorReporter.getErrorList().size());
  }

  @Test
  public void typedLambda() {
    // \x:Nat. x : Nat -> Nat
    Expression expr = Lam(lamArgs(Tele(true, vars("x"), Nat())), Index(0));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), null, errorReporter);
    assertEquals(Pi(Nat(), Nat()), result.type);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void tooManyLambdasError() {
    // \x y. x : Nat -> Nat
    Expression expr = Lam(lamArgs(Name("x"), Name("y")), Index(1));
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.checkType(new ArrayList<Binding>(), Pi(Nat(), Nat()), errorReporter));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void typedLambdaExpectedType() {
    // \(X : Type1) x. x : (X : Type0) (X) -> X
    Expression expr = Lam(lamArgs(Tele(vars("X"), Universe(1)), Name("x")), Index(0));
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertEquals(expr, expr.checkType(new ArrayList<Binding>(), Pi(args(Tele(vars("X"), Universe(0)), TypeArg(Index(0))), Index(1)), errorReporter).expression);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void lambdaExpectedError() {
    // \x. x : (Nat -> Nat) -> Nat
    Expression expr = Lam("x", Index(0));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), Pi(Pi(Nat(), Nat()), Nat()), errorReporter);
    assertEquals(null, result);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void lambdaOmegaError() {
    // \x. x x : (Nat -> Nat) -> Nat
    Expression expr = Lam("x", Apps(Index(0), Index(0)));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), Pi(Pi(Nat(), Nat()), Nat()), errorReporter);
    assertEquals(null, result);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void lambdaExpectedError2() {
    // \x. x 0 : (Nat -> Nat) -> Nat -> Nat
    Expression expr = Lam("x", Apps(Index(0), Zero()));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = expr.checkType(new ArrayList<Binding>(), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat())), errorReporter);
    assertEquals(null, result);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void letDependentType() {
    // \lam (F : \Pi N -> \Type0) (f : \Pi (x : N) -> F x) => \\let | x => 0 \\in f x");
    Expression expr = Lam(lamArgs(Tele(vars("F"), Pi(Nat(), Universe(0))), Tele(vars("f"), Pi(args(Tele(vars("x"), Nat())), Apps(Index(1), Index(0))))),
            Let(lets(let("x", Zero())), Apps(Index(1), Index(0))));
    ListErrorReporter errorReporter = new ListErrorReporter();
    expr.checkType(new ArrayList<Binding>(), null, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void letTypeHasBoundVarError() {
    // \lam (F : \Pi {A : \Type0}  (a : A) -> \Type1) (f : \Pi {A : \Type0} (x : A) -> F x) =>
    //   \let | x (y : Nat) : Nat <= \elim y | zero => zero
    //                                       | suc x' => suc x' \in f x)
    List<Clause> clauses = new ArrayList<>();
    ElimExpression elim = Elim(Index(0), clauses);
    clauses.add(new Clause(match(Prelude.ZERO), Abstract.Definition.Arrow.RIGHT, Zero(), elim));
    clauses.add(new Clause(match(Prelude.SUC, match("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Suc(), Index(0)), elim));
    Expression expr = Lam(lamArgs(
                    Tele(vars("F"), Pi(args(Tele(false, vars("A"), Universe(0)), Tele(vars("a"), Index(0))), Universe(1))),
                    Tele(vars("f"), Pi(args(Tele(false, vars("A"), Universe(0)), Tele(vars("x"), Index(0))), Apps(Index(2), Index(0))))),
            Let(lets(let("x", args(Tele(vars("y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, elim)), Apps(Index(1), Index(0))));
    ListErrorReporter errorReporter = new ListErrorReporter();
    expr.checkType(new ArrayList<Binding>(), null, errorReporter);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void letArrowType() {
    // \let | x (y : Nat) => Zero \in x : Nat -> Nat
    Expression expr = Let(lets(let("x", args(Tele(vars("y"), Nat())), Zero())), Index(0));
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertEquals(Pi(Nat(), Nat()), expr.checkType(new ArrayList<Binding>(), Pi(Nat(), Nat()), errorReporter).type);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void caseTranslation() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\function test (n : Nat) : Nat => \\case n, n | zero, _ => 0 | suc y, _ => y");
    FunctionDefinition def2 = (FunctionDefinition) typeCheckDef("\\function test (n : Nat) => \\let | caseF (caseA : Nat) (caseB : Nat) : Nat <= \\elim caseA, caseB | zero, _ => 0 | suc y, _ => y \\in caseF n n");
    assertEquals(def.getTerm(), def2.getTerm());
  }

  @Test
  public void caseNoExpectedError() {
    typeCheckDef("\\function test => \\case 1 | zero => 0 | suc y => y", 1);
  }
}
