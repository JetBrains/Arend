package org.arend.typechecking.implicitargs;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.PiExpression;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Level;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.ArgInferenceError;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class ImplicitArgumentsTest extends TypeCheckingTestCase {
  @Test
  public void inferId() {
    // f : {A : Type0} -> A -> A |- f 0 : N
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Ref(A), Ref(A)))));

    TypecheckingResult result = typeCheckExpr(context, "f 0", null);
    Expression expr = Apps(Ref(context.get(0)), Nat(), Zero());
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void unexpectedImplicit() {
    // f : N -> N |- f {0} 0 : N
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", Pi(Nat(), Nat())));

    assertThat(typeCheckExpr(context, "f {0} 0", null, 1), is(nullValue()));
  }

  @Test
  public void tooManyArguments() {
    // f : (x : N) {y : N} (z : N) -> N |- f 0 0 0 : N
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", Pi(singleParam("x", Nat()), Pi(singleParams(false, vars("y"), Nat()), Pi(singleParam("z", Nat()), Nat())))));

    assertThat(typeCheckExpr(context, "f 0 0 0", null, 1), is(nullValue()));
  }

  @Test
  public void cannotInfer() {
    // f : {A B : Type0} -> A -> A |- f 0 : N
    List<Binding> context = new ArrayList<>();
    SingleDependentLink params = singleParams(false, vars("A", "B"), Universe(0));
    context.add(new TypedBinding("f", Pi(params, Pi(Ref(params), Ref(params)))));

    typeCheckExpr(context, "f 0", null, 1);
    assertTrue(errorList.get(0).getLocalError() instanceof ArgInferenceError);
  }

  @Test
  public void inferLam() {
    // f : {A : Type0} -> ((A -> Nat) -> Nat) -> A |- f (\g. g 0) : Nat
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Pi(Ref(A), Nat()), Nat()), Ref(A)))));

    TypecheckingResult result = typeCheckExpr(context, "f (\\lam g => g 0)", null);
    SingleDependentLink g = singleParam("g", Pi(Nat(), Nat()));
    Expression expr = Apps(Ref(context.get(0)), Nat(), Lam(g, Apps(Ref(g), Zero())));
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromFunction() {
    // s : Nat -> Nat, f : {A : Type0} -> (Nat -> A) -> A |- f s : Nat
    List<Binding> context = new ArrayList<>(2);
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("s", Pi(Nat(), Nat())));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Ref(A)), Ref(A)))));

    TypecheckingResult result = typeCheckExpr(context, "f s", null);
    Expression expr = Apps(Ref(context.get(1)), Nat(), Ref(context.get(0)));
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromLam() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x y. suc y) : Nat -> Nat
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Ref(A)), Ref(A)))));

    TypecheckingResult result = typeCheckExpr(context, "f (\\lam x y => suc y)", null);
    SingleDependentLink xy = singleParams(true, vars("x", "y"), Nat());
    Expression expr = Apps(Ref(context.get(0)), Pi(Nat(), Nat()), Lam(xy, Suc(Ref(xy.getNext()))));
    assertEquals(expr, result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromLamType() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x (y : Nat -> Nat). y x) : (Nat -> Nat) -> Nat
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Ref(A)), Ref(A)))));

    TypecheckingResult result = typeCheckExpr(context, "f (\\lam x (y : Nat -> Nat) => y x)", null);
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Pi(Nat(), Nat()));
    Expression arg = Lam(x, Lam(y, Apps(Ref(y), Ref(x))));
    Expression expr = Apps(Ref(context.get(0)), Pi(Pi(Nat(), Nat()), Nat()), arg);
    assertEquals(expr, result.expression);
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), result.type);
  }

  @Test
  public void inferFromSecondArg() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\x. x) : Nat
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Ref(A), Ref(A)), Pi(Pi(Ref(A), Nat()), Nat())))));

    TypecheckingResult result = typeCheckExpr(context, "f (\\lam x => x) (\\lam x => x)", null);
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = Apps(Ref(context.get(0)), Nat(), Lam(x, Ref(x)), Lam(x, Ref(x)));
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromSecondArgLam() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\(x : Nat). x) : Nat
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Ref(A), Ref(A)), Pi(Pi(Ref(A), Nat()), Nat())))));

    TypecheckingResult result = typeCheckExpr(context, "f (\\lam x => x) (\\lam (x : Nat) => x)", null);
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = Apps(Ref(context.get(0)), Nat(), Lam(x, Ref(x)), Lam(x, Ref(x)));
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromTheGoal() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Nat(), Pi(Ref(A), Ref(A))))));

    TypecheckingResult result = typeCheckExpr(context, "f 0", Pi(Nat(), Nat()));
    Expression expr = Apps(Ref(context.get(0)), Nat(), Zero());
    assertEquals(expr, result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromTheGoalError() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat -> Nat
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Nat(), Pi(Ref(A), Ref(A))))));

    typeCheckExpr(context, "f 0", Pi(Nat(), Pi(Nat(), Nat())), 1);
  }

  @Test
  public void inferCheckTypeError() {
    // I : Type1 -> Type1, i : I Type0, f : {A : Type0} -> I A -> Nat |- f i : Nat
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    context.add(new TypedBinding("i", Apps(Ref(context.get(0)), Universe(0))));
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Apps(Ref(context.get(0)), Ref(A)), Nat()))));

    typeCheckExpr(context, "f i", null, 1);
  }

  @Test
  public void inferTail() {
    // I : Nat -> Type0, i : {x : Nat} -> I (suc x) |- i : I (suc (suc 0))
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    SingleDependentLink x = singleParams(false, vars("x"), Nat());
    context.add(new TypedBinding("i", Pi(x, Apps(Ref(context.get(0)), Suc(Ref(x))))));
    Expression type = Apps(Ref(context.get(0)), Suc(Suc(Zero())));

    TypecheckingResult result = typeCheckExpr(context, "i", type);
    Expression expr = Apps(Ref(context.get(1)), Suc(Zero()));
    assertEquals(expr, result.expression);
    assertEquals(type, result.type);
  }

  @Test
  public void inferTail2() {
    // I : Nat -> Type0, i : {x : Nat} -> I x |- i : {x : Nat} -> I x
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    SingleDependentLink x = singleParams(false, vars("x"), Nat());
    PiExpression type = Pi(x, Apps(Ref(context.get(0)), Ref(x)));
    context.add(new TypedBinding("i", type));

    TypecheckingResult result = typeCheckExpr(context, "i", type);
    assertEquals(Ref(context.get(1)), result.expression);
    assertEquals(type, result.type);
  }

  @Test
  public void inferTailError() {
    // I : Type1 -> Type1, i : {x : Type0} -> I x |- i : I Type0
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    SingleDependentLink x = singleParams(false, vars("x"), Universe(0));
    context.add(new TypedBinding("i", Pi(x, Apps(Ref(context.get(0)), Ref(x)))));

    typeCheckExpr(context, "i", Apps(Ref(context.get(0)), Universe(0)), 1);
  }

  @Test
  public void inferUnderLet() {
    // f : {A : Type0} -> (A -> A) -> A -> A |- let | x {A : Type0} (y : A -> A) = f y | z (x : Nat) = x \in x z :
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(singleParam(null, Pi(Ref(A), Ref(A))), Pi(singleParam(null, Ref(A)), Ref(A))))));

    String term =
        "\\let\n" +
        "  | x {A : \\oo-Type0} (y : A -> A) => f y\n" +
        "  | z (x : Nat) => x\n" +
        "\\in x z";
    TypecheckingResult result = typeCheckExpr(context, term, null);
    assertEquals(Pi(Nat(), Nat()), result.type.normalize(NormalizeVisitor.Mode.WHNF));
  }

  @Test
  public void untypedLambda1() {
    // f : (A : \Type0) (a : A) -> Nat |- \x1 x2. f x1 x2
    SingleDependentLink A = singleParam("A", Universe(0));
    PiExpression type = Pi(A, Pi(singleParam("a", Ref(A)), Nat()));
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));
    typeCheckExpr(context, "\\lam x1 x2 => f x1 x2", null);
  }

  @Test
  public void untypedLambda2() {
    // f : (A : Type) (B : A -> Type) (a : A) -> B a |- \x1 x2 x3. f x1 x2 x3
    SingleDependentLink A = singleParam("A", Universe(0));
    SingleDependentLink B = singleParam("B", Pi(Ref(A), Universe(0)));
    SingleDependentLink a = singleParam("a", Ref(A));
    PiExpression type = Pi(A, Pi(B, Pi(a, Apps(Ref(B), Ref(a)))));
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));

    TypecheckingResult result = typeCheckExpr(context, "\\lam x1 x2 x3 => f x1 x2 x3", null);
    A.setType(Universe(new Level(0), new Level(LevelVariable.HVAR)));
    B.setType(Pi(Ref(A), Universe(new Level(0), new Level(LevelVariable.HVAR))));
    assertEquals(type, result.type);
  }

  @Test
  public void untypedLambdaError1() {
    // f : (A : \Type0) (a : A) -> Nat |- \x1 x2. f x2 x1
    SingleDependentLink A = singleParam("A", Universe(0));
    PiExpression type = Pi(A, Pi(singleParam("a", Ref(A)), Nat()));
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));
    typeCheckExpr(context, "\\lam x1 x2 => f x2 x1", null, 1);
  }

  @Test
  public void untypedLambdaError2() {
    // f : (A : Type0) (B : A -> Type0) (a : A) -> B a |- \x1 x2 x3. f x2 x1 x3
    SingleDependentLink A = singleParam("A", Universe(0));
    SingleDependentLink B = singleParam("B", Pi(Ref(A), Universe(0)));
    SingleDependentLink a = singleParam("a", Ref(A));
    PiExpression type = Pi(A, Pi(B, Pi(a, Apps(Ref(B), Ref(a)))));
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));

    typeCheckExpr(context, "\\lam x1 x2 x3 => f x2 x1 x3", null, 1);
  }

  @Test
  public void inferLater() {
    // f : {A : \Type0} (B : \Type1) -> A -> B -> A |- f Nat (\lam x => x) 0 : Nat -> Nat
    List<Binding> context = new ArrayList<>();
    SingleDependentLink A = singleParams(false, vars("A"), Universe(0));
    SingleDependentLink B = singleParams(true, vars("B"), Universe(1));
    context.add(new TypedBinding("f", Pi(A, Pi(B, Pi(Ref(A), Pi(Ref(B), Ref(A)))))));
    typeCheckExpr(context, "f Nat (\\lam x => x) 0", Pi(Nat(), Nat()));
  }

  @Test
  public void inferUnderPi() {
    typeCheckModule(
        "\\func $ {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
        "\\func foo (A : \\Type0) (B : A -> \\Type0) (f : \\Pi (a : A) -> B a) (a' : A) => f $ a'", 1);
  }

  @Test
  public void inferUnderPiExpected() {
    typeCheckModule(
        "\\func $ {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
        "\\func foo (A : \\Type0) (B : A -> \\Type0) (f : \\Pi (a : A) -> B a) (a' : A) : B a' => f $ a'", 1);
  }

  @Test
  public void inferPathCon() {
    typeCheckDef("\\func f : 1 = 1 => path (\\lam _ => 0)", 1);
  }

  @Test
  public void inferPathCon0() {
    typeCheckDef("\\func f : 1 = 1 => path {\\lam _ => Nat} (\\lam _ => 0)", 1);
  }

  @Test
  public void inferPathCon1() {
    typeCheckDef("\\func f : 1 = 1 => path {\\lam _ => Nat} {1} (\\lam _ => 0)", 1);
  }

  @Test
  public void inferPathCon2() {
    typeCheckDef("\\func f : 1 = 1 => path {\\lam _ => Nat} {0} (\\lam _ => 0)", 1);
  }

  @Test
  public void inferPathCon3() {
    typeCheckDef("\\func f : 1 = 1 => path {\\lam _ => Nat} {1} {1} (\\lam _ => 0)", 1);
  }

  @Test
  public void pathWithoutArg() {
    typeCheckDef("\\func f => path", 1);
  }

  @Test
  public void pathWithoutArg1() {
    typeCheckDef("\\func f : \\Pi {A : I -> \\Type0} {a : A left} {a' : A right} (\\Pi (i : I) -> A i) -> Path A a a' => path", 1);
  }

  @Test
  public void pathWithoutArg2() {
    typeCheckDef("\\func f => path {\\lam _ => Nat}", 1);
  }

  @Test
  public void pathWithoutArg3() {
    typeCheckDef("\\func f => path {\\lam _ => Nat} {0}", 1);
  }

  @Test
  public void pathWithoutArg4() {
    typeCheckDef("\\func f => path {\\lam _ => Nat} {0} {0}", 1);
  }

  @Test
  public void orderTest1() {
    typeCheckModule(
        "\\func idpOver (A : I -> \\Type0) (a : A left) : Path A a (coe A a right) => path (coe A a)\n" +
        "\\func test {A : \\Type0} (P : A -> \\Type0) {a a' : A} (q : a = a') (pa : P a) (i : I)\n" +
        "  => idpOver (\\lam (j : I) => P (q @ j)) pa @ i\n");
  }

  @Test
  public void orderTest2() {
    typeCheckModule(
        "\\func idpOver (A : I -> \\Type0) (a : A left) : Path A a (coe A a right) => path (coe A a)\n" +
        "\\func test {A : \\Type0} (P : A -> \\Type0) {a : A} (pa : P a) (i : I)\n" +
        "  => \\lam (a' : A) (q : a = a') => idpOver (\\lam (j : I) => P (q @ j)) pa @ i");
  }

  @Test
  public void differentLevels() {
    typeCheckModule(
        "\\func F (X : \\Type \\lp) (B : X -> \\Type \\lp) => zero\n" +
        "\\func g (X : \\Type \\lp) => F X (\\lam _ => X = X)");
  }

  @Test
  public void piTest() {
    typeCheckDef("\\func f (A : \\Type \\lp) (B : A -> \\Type \\lp) (f g : \\Pi (x : A) -> B x) => f = g");
  }

  @Test
  public void etaExpansionTest() {
    typeCheckModule(
        "\\func \\infixr 9 $ {A B : \\Set0} (f : A -> B) (a : A) => f a\n" +
        "\\data Fin Nat \\with | n => fzero | suc n => fsuc (Fin n)\n" +
        "\\func unsuc {n : Nat} (x : Fin (suc n)) : Fin n \\elim n, x\n" +
        "  | _, fzero => fzero\n" +
        "  | zero, fsuc x => fzero\n" +
        "  | suc n, fsuc x => fsuc (unsuc x)\n" +
        "\\func foo {n : Nat} (x : Fin n) : Nat \\elim n\n" +
        "  | zero => zero\n" +
        "  | suc n' => foo $ unsuc x");
  }

  @Test
  public void freeVars() {
    typeCheckModule(
      "\\func f {n : Nat} {g : Nat -> Nat} (p : g = (\\lam x => n)) => 0\n" +
      "\\func h => f (path (\\lam _ x => x))", 1);
  }
}
