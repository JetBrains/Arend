package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.*;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.Matchers.typeMismatchError;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class ExpressionTest extends TypeCheckingTestCase {
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
    SingleDependentLink param = singleParam("X", Universe(0));
    typeCheckExpr("\\lam X x => x", Pi(param, Pi(Ref(param), Ref(param))));
  }

  @Test
  public void typeCheckingIdSplit() {
    // \X x. x : (X : Type0) -> X -> X
    SingleDependentLink param = singleParam("X", Universe(0));
    typeCheckExpr("\\lam X => \\lam x => x", Pi(param, Pi(Ref(param), Ref(param))));
  }

  @Test
  public void typeCheckingIdError() {
    // \X x. X : (X : Type0) -> X -> X
    SingleDependentLink param = singleParam("X", Universe(0));
    typeCheckExpr("\\lam X x => X", Pi(param, Pi(Ref(param), Ref(param))), 1);
    assertTrue(errorList.get(0) instanceof TypeCheckingError && ((TypeCheckingError) errorList.get(0)).localError instanceof TypeMismatchError);
  }

  @Test
  public void typeCheckingApp() {
    // \x y. y (y x) : Nat -> (Nat -> Nat) -> Nat
    typeCheckExpr("\\lam x y => y (y x)", Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat())));
  }

  @Test
  public void typeCheckingAppIndex() {
    // \x y. y (y x) : N -> (N -> N) -> N
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.Expression expr = cLam(x, cLam(y, cApps(cVar(y), cApps(cVar(y), cVar(x)))));
    typeCheckExpr(expr, Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat())));
  }

  @Test
  public void typeCheckingAppPiIndex() {
    // T : Nat -> Type, Q : (x : Nat) -> T x -> Type |- \f g. g zero (f zero) : (f : (x : Nat) -> T x) -> ((x : Nat) -> T x -> Q x (f x)) -> Q zero (f zero)
    Concrete.ReferableSourceNode cf = ref("f");
    Concrete.ReferableSourceNode cg = ref("g");
    Concrete.Expression expr = cLam(cf, cLam(cg, cApps(cVar(cg), cZero(), cApps(cVar(cf), cZero()))));
    Map<Abstract.ReferableSourceNode, Binding> context = new HashMap<>();
    Binding T = new TypedBinding("T", Pi(Nat(), Universe(0)));
    context.put(ref("T"), T);
    SingleDependentLink x_ = singleParam("x", Nat());
    Binding Q = new TypedBinding("Q", Pi(x_, Pi(singleParam(null, Apps(Ref(T), Ref(x_))), Universe(0))));
    context.put(ref("Q"), Q);

    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink f = singleParam("f", Pi(x, Apps(Ref(T), Ref(x))));
    SingleDependentLink x2 = singleParam("x", Nat());
    Expression type = Pi(f, Pi(Pi(x2, Pi(Apps(Ref(T), Ref(x2)), Apps(Ref(Q), Ref(x2), Apps(Ref(f), Ref(x2))))), Apps(Ref(Q), Zero(), Apps(Ref(f), Zero()))));

    typeCheckExpr(context, expr, type);
  }

  @Test
  public void typeCheckingAppLamPiIndex() {
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("X", Pi(Nat(), Universe(0))));
    SingleDependentLink link = singleParam("t", Nat());
    context.add(new TypedBinding("Y", Pi(link, Pi(singleParam("x", Apps(Ref(context.get(0)), Ref(link))), Universe(0)))));
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
    Concrete.ReferableSourceNode X = ref("X");
    Concrete.Expression expr = cPi(X, cUniverseInf(1), cPi(cVar(X), cVar(X)));
    assertThat(typeCheckExpr(expr, null).type, is(Universe(2)));
  }

  @Test
  public void typeCheckingUniverse() {
    // (f : Type1 -> Type1) -> f Type1
    Concrete.ReferableSourceNode f = ref("f");
    Concrete.Expression expr = cPi(f, cPi(cUniverseStd(1), cUniverseStd(1)), cApps(cVar(f), cUniverseStd(1)));
    typeCheckExpr(expr, null, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void typeCheckingTwoErrors() {
    // f : Nat -> Nat -> Nat |- f S (f 0 S) : Nat
    Concrete.ReferableSourceNode f = ref("f");
    Concrete.Expression expr = cApps(cVar(f), cSuc(), cApps(cVar(f), cZero(), cSuc()));
    Map<Abstract.ReferableSourceNode, Binding> defs = new HashMap<>();
    defs.put(f, new TypedBinding(f.getName(), Pi(Nat(), Pi(Nat(), Nat()))));
    assertThat(typeCheckExpr(defs, expr, null, 2), is(nullValue()));
  }

  @Test
  public void typedLambda() {
    // \x:Nat. x : Nat -> Nat
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.Expression expr = cLam(cargs(cTele(true, cvars(x), cNat())), cVar(x));
    assertEquals(typeCheckExpr(expr, null).type, Pi(Nat(), Nat()));
  }

  @Test
  public void tooManyLambdasError() {
    // \x y. x : Nat -> Nat
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.Expression expr = cLam(cargs(cName(x), cName(y)), cVar(x));
    assertThat(typeCheckExpr(expr, Pi(Nat(), Nat()), 1), is(nullValue()));
  }

  @Test
  public void typedLambdaExpectedType() {
    // \(X : Type0) x. x : (X : Type0) (X) -> X
    SingleDependentLink link = singleParam("X", Universe(0));
    typeCheckExpr("\\lam (X : \\oo-Type0) x => x", Pi(link, Pi(singleParam(null, Ref(link)), Ref(link))));
  }

  @Test
  public void lambdaExpectedError() {
    // \x. x : (Nat -> Nat) -> Nat
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.Expression expr = cLam(x, cVar(x));
    typeCheckExpr(expr, Pi(Pi(Nat(), Nat()), Nat()), 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void lambdaOmegaError() {
    // \x. x x : (Nat -> Nat) -> Nat
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.Expression expr = cLam(x, cApps(cVar(x), cVar(x)));
    typeCheckExpr(expr, Pi(Pi(Nat(), Nat()), Nat()), 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void lambdaExpectedError2() {
    // \x. x 0 : (Nat -> Nat) -> Nat -> Nat
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.Expression expr = cLam(x, cApps(cVar(x), cZero()));
    typeCheckExpr(expr, Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat())), 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void letDependentType() {
    // \lam (F : \Pi N -> \Type0) (f : \Pi (x : N) -> F x) => \\let | x => 0 \\in f x");
    Concrete.ReferableSourceNode F = ref("F");
    Concrete.ReferableSourceNode f = ref("f");
    Concrete.LetClause x = clet("x", cZero());
    Concrete.Expression expr = cLam(cargs(cTele(cvars(F), cPi(cNat(), cUniverseStd(0))), cTele(cvars(f), cPi(ctypeArgs(cTele(cvars(x), cNat())), cApps(cVar(F), cVar(x))))),
            cLet(clets(x), cApps(cVar(f), cVar(x))));
    typeCheckExpr(expr, null);
  }

  @Test
  public void letArrowType() {
    // \let | x (y : Nat) => Zero \in x : Nat -> Nat
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.LetClause x = clet("x", cargs(cTele(cvars(y), cNat())), cZero());
    Concrete.Expression expr = cLet(clets(x), cVar(x));
    CheckTypeVisitor.Result result = typeCheckExpr(expr, null);
    assertEquals(result.type.normalize(NormalizeVisitor.Mode.WHNF), Pi(Nat(), Nat()));
  }

  @Test
  public void caseTranslation() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\function test (n : Nat) : Nat => \\case n, n | zero, _ => 0 | suc y, _ => y");
    FunctionDefinition def2 = (FunctionDefinition) typeCheckDef("\\function test (n : Nat) => \\let | caseF (caseA : Nat) (caseB : Nat) : Nat => \\elim caseA, caseB | zero, _ => 0 | suc y, _ => y \\in caseF n n");
    assertTrue(CompareVisitor.compare(new HashMap<>(Collections.singletonMap(def.getParameters(), def2.getParameters())),
        DummyEquations.getInstance(), (LeafElimTree) def.getBody(), (LeafElimTree) def2.getBody()));
  }

  @Test
  public void caseNoExpectedError() {
    typeCheckDef("\\function test => \\case 1 | zero => 0 | suc y => y", 1);
  }
}
