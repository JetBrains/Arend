package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
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
    DependentLink param = param("X", Universe(0));
    typeCheckExpr("\\lam X x => x", Pi(param, Pi(Reference(param), Reference(param))));
  }

  @Test
  public void typeCheckingIdSplit() {
    // \X x. x : (X : Type0) -> X -> X
    DependentLink param = param("X", Universe(0));
    typeCheckExpr("\\lam X => \\lam x => x", Pi(param, Pi(Reference(param), Reference(param))));
  }

  @Test
  public void typeCheckingIdError() {
    // \X x. X : (X : Type0) -> X -> X
    ListErrorReporter errorReporter = new ListErrorReporter();
    DependentLink param = param("X", Universe(0));
    typeCheckExpr("\\lam X x => X", Pi(param, Pi(Reference(param), Reference(param))), errorReporter);
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
    Concrete.Expression expr = cLam("x", cLam("y", cApps(cVar("y"), cApps(cVar("y"), cVar("x")))));
    ListErrorReporter errorReporter = new ListErrorReporter();
    new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build().checkType(expr, Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat())));
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void typeCheckingAppPiIndex() {
    // T : Nat -> Type, Q : (x : Nat) -> T x -> Type |- \f g. g zero (f zero) : (f : (x : Nat) -> T x) -> ((x : Nat) -> T x -> Q x (f x)) -> Q zero (f zero)
    Concrete.Expression expr = cLam("f", cLam("g", cApps(cVar("g"), cZero(), cApps(cVar("f"), cZero()))));
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("T", Pi(Nat(), Universe(0))));
    DependentLink x_ = param("x", Nat());
    context.add(new TypedBinding("Q", Pi(params(x_, param(Apps(Reference(context.get(0)), Reference(x_)))), Universe(0))));

    DependentLink x = param("x", Nat());
    DependentLink f = param("f", Pi(x, Apps(Reference(context.get(0)), Reference(x))));
    DependentLink x2 = param("x", Nat());
    Expression type = Pi(f, Pi(Pi(x2, Pi(Apps(Reference(context.get(0)), Reference(x2)), Apps(Reference(context.get(1)), Reference(x2), Apps(Reference(f), Reference(x2))))), Apps(Reference(context.get(1)), Zero(), Apps(Reference(f), Zero()))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    new CheckTypeVisitor.Builder(context, errorReporter).build().checkType(expr, type);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void typeCheckingAppLamPiIndex() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("X", Pi(Nat(), Universe(0))));
    DependentLink link = param("t", Nat());
    context.add(new TypedBinding("Y", Pi(params(link, param("x", Apps(Reference(context.get(0)), Reference(link)))), Universe(0))));
    CheckTypeVisitor.Result typeResult = typeCheckExpr(context,
          "\\Pi (f : \\Pi (g : Nat -> Nat) -> X (g zero)) " +
          "-> (\\Pi (z : (Nat -> Nat) -> Nat) -> Y (z (\\lam _ => 0)) (f (\\lam x => z (\\lam _ => x)))) " +
          "-> Y 0 (f (\\lam x => x))", null);
    assertNotNull(typeResult);
    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\lam f h => h (\\lam k => k 1)", typeResult.expression);
    assertNotNull(result);
  }

  @Test
  public void typeCheckingInferPiIndex() {
    // (X : Type1) -> X -> X : Type2
    Concrete.Expression expr = cPi("X", cUniverse(1), cPi(cVar("X"), cVar("X")));
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertEquals(Universe(2), expr.accept(new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build(), null).type);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void typeCheckingUniverse() {
    // (f : Type1 -> Type1) -> f Type1
    Concrete.Expression expr = cPi("f", cPi(cUniverse(1), cUniverse(1)), cApps(cVar("f"), cUniverse(1)));
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertEquals(null, expr.accept(new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build(), null));
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void typeCheckingTwoErrors() {
    // f : Nat -> Nat -> Nat |- f S (f 0 S) : Nat
    Concrete.Expression expr = cApps(cVar("f"), cSuc(), cApps(cVar("f"), cZero(), cSuc()));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(Nat(), Pi(Nat(), Nat()))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.accept(new CheckTypeVisitor.Builder(defs, errorReporter).build(), null));
    assertEquals(errorReporter.getErrorList().toString(), 2, errorReporter.getErrorList().size());
  }

  @Test
  public void typedLambda() {
    // \x:Nat. x : Nat -> Nat
    Concrete.Expression expr = cLam(cargs(cTele(true, cvars("x"), cNat())), cVar("x"));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build(), null);
    assertEquals(Pi(Nat(), Nat()), result.type);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void tooManyLambdasError() {
    // \x y. x : Nat -> Nat
    Concrete.Expression expr = cLam(cargs(cName("x"), cName("y")), cVar("x"));
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build().checkType(expr, Pi(Nat(), Nat())));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void typedLambdaExpectedType() {
    // \(X : Type0) x. x : (X : Type0) (X) -> X
    DependentLink link = param("X", Universe(0));
    typeCheckExpr("\\lam (X : \\Type0) x => x", Pi(params(link, param(Reference(link))), Reference(link)));
  }

  @Test
  public void lambdaExpectedError() {
    // \x. x : (Nat -> Nat) -> Nat
    Concrete.Expression expr = cLam("x", cVar("x"));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build().checkType(expr, Pi(Pi(Nat(), Nat()), Nat()));
    assertEquals(null, result);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void lambdaOmegaError() {
    // \x. x x : (Nat -> Nat) -> Nat
    Concrete.Expression expr = cLam("x", cApps(cVar("x"), cVar("x")));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build().checkType(expr, Pi(Pi(Nat(), Nat()), Nat()));
    assertEquals(null, result);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void lambdaExpectedError2() {
    // \x. x 0 : (Nat -> Nat) -> Nat -> Nat
    Concrete.Expression expr = cLam("x", cApps(cVar("x"), cZero()));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build().checkType(expr, Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat())));
    assertEquals(null, result);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof TypeMismatchError);
  }

  @Test
  public void letDependentType() {
    // \lam (F : \Pi N -> \Type0) (f : \Pi (x : N) -> F x) => \\let | x => 0 \\in f x");
    Concrete.Expression expr = cLam(cargs(cTele(cvars("F"), cPi(cNat(), cUniverse(0))), cTele(cvars("f"), cPi(ctypeArgs(cTele(cvars("x"), cNat())), cApps(cVar("F"), cVar("x"))))),
            cLet(clets(clet("x", cZero())), cApps(cVar("f"), cVar("x"))));
    ListErrorReporter errorReporter = new ListErrorReporter();
    new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void letTypeHasBoundVarError() {
    // \lam (F : \Pi {A : \Type0}  (a : A) -> \Type1) (f : \Pi {A : \Type0} (x : A) -> F x) =>
    //   \let | x (y : Nat) : Nat <= \elim y | zero => zero
    //                                       | suc x' => suc x' \in f x)
    Concrete.Expression elimTree = cElim(Collections.<Concrete.Expression>singletonList(cVar("y")),
        cClause(cPatterns(cConPattern(Preprelude.ZERO.getName())), Abstract.Definition.Arrow.RIGHT, cDefCall(null, Preprelude.ZERO)),
        cClause(cPatterns(cConPattern(Preprelude.SUC.getName(), cPatternArg(cNamePattern("x'"), true, false))), Abstract.Definition.Arrow.RIGHT, cSuc(cVar("x'"))));
    Concrete.Expression expr = cLam(cargs(
            cTele(cvars("F"), cPi(ctypeArgs(cTele(false, cvars("A"), cUniverse(0)), cTele(cvars("a"), cVar("A"))), cUniverse(1))),
            cTele(cvars("f"), cPi(ctypeArgs(cTele(false, cvars("A"), cUniverse(0)), cTele(cvars("x"), cVar("A"))), cApps(cVar("F"), cVar("x"))))),
        cLet(clets(clet("x", cargs(cTele(cvars("y"), cNat())), cNat(), Abstract.Definition.Arrow.LEFT, elimTree)), cApps(cVar("f"), cVar("x"))));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build(), null);
    Expression typeCodom = result.type.getPiParameters(new ArrayList<DependentLink>(), true, false);
    assertNotNull(typeCodom.toLet());
  }

  @Test
  public void letArrowType() {
    // \let | x (y : Nat) => Zero \in x : Nat -> Nat
    Concrete.Expression expr = cLet(clets(clet("x", cargs(cTele(cvars("y"), cNat())), cZero())), cVar("x"));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(new ArrayList<Binding>(), errorReporter).build(), null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Pi(Nat(), Nat()), result.type.normalize(NormalizeVisitor.Mode.WHNF));
  }

  @Test
  public void caseTranslation() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\function test (n : Nat) : Nat => \\case n, n | zero, _ => 0 | suc y, _ => y");
    FunctionDefinition def2 = (FunctionDefinition) typeCheckDef("\\function test (n : Nat) => \\let | caseF (caseA : Nat) (caseB : Nat) : Nat <= \\elim caseA, caseB | zero, _ => 0 | suc y, _ => y \\in caseF n n");
    assertTrue(CompareVisitor.compare(new HashMap<Binding, Binding>(Collections.singletonMap(def.getParameters(), def2.getParameters())),
        DummyEquations.getInstance(), Equations.CMP.EQ, def.getElimTree(), def2.getElimTree()));
  }

  @Test
  public void caseNoExpectedError() {
    typeCheckDef("\\function test => \\case 1 | zero => 0 | suc y => y", 1);
  }

  @Test
  public void coverageInLet() {
    typeCheckDef("\\function test => \\let x (n : Nat) : Nat <= \\elim n | zero => 0 \\in x 1", 1);
  }
}
