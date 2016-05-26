package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.*;
import static org.junit.Assert.*;

public class ImplicitArgumentsTest {
  @Test
  public void inferId() {
    // f : {A : Type0} -> A -> A |- f 0 : N
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Reference(A), Reference(A)))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f 0", null);
    Expression expr = Reference(context.get(0))
      .addArgument(Nat(), EnumSet.of(AppExpression.Flag.VISIBLE))
      .addArgument(Zero(), AppExpression.DEFAULT);
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void unexpectedImplicit() {
    // f : N -> N |- f {0} 0 : N
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", Pi(Nat(), Nat())));

    assertNull(typeCheckExpr(context, "f {0} 0", null, 1));
  }

  @Test
  public void tooManyArguments() {
    // f : (x : N) {y : N} (z : N) -> N |- f 0 0 0 : N
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", Pi(param("x", Nat()), Pi(param(false, "y", Nat()), Pi(param("z", Nat()), Nat())))));

    assertNull(typeCheckExpr(context, "f 0 0 0", null, 1));
  }

  @Test
  public void cannotInfer() {
    // f : {A B : Type0} -> A -> A |- f 0 : N
    List<Binding> context = new ArrayList<>();
    DependentLink params = param(false, vars("A", "B"), Universe(0));
    context.add(new TypedBinding("f", Pi(params, Pi(Reference(params), Reference(params)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    typeCheckExpr(context, "f 0", null, errorReporter);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof ArgInferenceError);
  }

  @Test
  public void inferLam() {
    // f : {A : Type0} -> ((A -> Nat) -> Nat) -> A |- f (\g. g 0) : Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Pi(Reference(A), Nat()), Nat()), Reference(A)))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f (\\lam g => g 0)", null);
    DependentLink g = param("g", Pi(Nat(), Nat()));
    Expression expr = Reference(context.get(0))
      .addArgument(Nat(), EnumSet.noneOf(AppExpression.Flag.class))
      .addArgument(Lam(g, Apps(Reference(g), Zero())), AppExpression.DEFAULT);
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromFunction() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f suc : Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Reference(A)), Reference(A)))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f suc", null);
    Expression expr = Reference(context.get(0))
      .addArgument(Nat(), EnumSet.of(AppExpression.Flag.VISIBLE))
      .addArgument(Suc(), AppExpression.DEFAULT);
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromLam() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x. S) : Nat -> Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Reference(A)), Reference(A)))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f (\\lam x => suc)", null);
    Expression expr = Reference(context.get(0))
      .addArgument(Pi(Nat(), Nat()), EnumSet.of(AppExpression.Flag.VISIBLE))
      .addArgument(Lam(param("x", Nat()), Suc()), AppExpression.DEFAULT);
    assertEquals(expr, result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromLamType() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x (y : Nat -> Nat). y x) : (Nat -> Nat) -> Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Reference(A)), Reference(A)))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f (\\lam x (y : Nat -> Nat) => y x)", null);
    DependentLink params = params(param("x", Nat()), param("y", Pi(Nat(), Nat())));
    Expression arg = Lam(params, Apps(Reference(params.getNext()), Reference(params)));
    Expression expr = Reference(context.get(0))
      .addArgument(Pi(Pi(Nat(), Nat()), Nat()), EnumSet.of(AppExpression.Flag.VISIBLE))
      .addArgument(arg, AppExpression.DEFAULT);
    assertEquals(expr, result.expression);
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), result.type);
  }

  @Test
  public void inferFromSecondArg() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\x. x) : Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Reference(A), Reference(A)), Pi(Pi(Reference(A), Nat()), Nat())))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f (\\lam x => x) (\\lam x => x)", null);
    DependentLink x = param("x", Nat());
    Expression expr = Reference(context.get(0))
      .addArgument(Nat(), EnumSet.of(AppExpression.Flag.VISIBLE))
      .addArgument(Lam(x, Reference(x)), AppExpression.DEFAULT)
      .addArgument(Lam(x, Reference(x)), AppExpression.DEFAULT);
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromSecondArgLam() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\(x : Nat). x) : Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Reference(A), Reference(A)), Pi(Pi(Reference(A), Nat()), Nat())))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f (\\lam x => x) (\\lam (x : Nat) => x)", null);
    DependentLink x = param("x", Nat());
    Expression expr = Reference(context.get(0))
        .addArgument(Nat(), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Lam(x, Reference(x)), AppExpression.DEFAULT)
        .addArgument(Lam(x, Reference(x)), AppExpression.DEFAULT);
    assertEquals(expr, result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromTheGoal() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Nat(), Pi(Reference(A), Reference(A))))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f 0", Pi(Nat(), Nat()));
    Expression expr = Reference(context.get(0))
        .addArgument(Nat(), EnumSet.of(AppExpression.Flag.VISIBLE))
        .addArgument(Zero(), AppExpression.DEFAULT);
    assertEquals(expr, result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromTheGoalError() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat -> Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Nat(), Pi(Reference(A), Reference(A))))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    typeCheckExpr(context, "f 0", Pi(Nat(), Pi(Nat(), Nat())), errorReporter);
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof ArgInferenceError);
  }

  @Test
  public void inferCheckTypeError() {
    // I : Type1 -> Type1, i : I Type0, f : {A : Type0} -> I A -> Nat |- f i : Nat
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    context.add(new TypedBinding("i", Apps(Reference(context.get(0)), Universe(0))));
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Apps(Reference(context.get(0)), Reference(A)), Nat()))));

    typeCheckExpr(context, "f i", null, 1);
  }

  @Test
  public void inferTail() {
    // I : Nat -> Type0, i : {x : Nat} -> I (suc x) |- i : I (suc (suc 0))
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    DependentLink x = param(false, "x", Nat());
    context.add(new TypedBinding("i", Pi(x, Apps(Reference(context.get(0)), Apps(Suc(), Reference(x))))));
    Expression type = Apps(Reference(context.get(0)), Apps(Suc(), Apps(Suc(), Zero())));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "i", type);
    Expression expr = Reference(context.get(1))
        .addArgument(Suc(Zero()), EnumSet.of(AppExpression.Flag.VISIBLE));
    assertEquals(expr, result.expression);
    assertEquals(type, result.type);
  }

  @Test
  public void inferTail2() {
    // I : Nat -> Type0, i : {x : Nat} -> I x |- i : {x : Nat} -> I x
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    DependentLink x = param(false, "x", Nat());
    Expression type = Pi(x, Apps(Reference(context.get(0)), Reference(x)));
    context.add(new TypedBinding("i", type));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "i", type);
    assertEquals(Reference(context.get(1)), result.expression);
    assertEquals(type, result.type);
  }

  @Test
  public void inferTailError() {
    // I : Type1 -> Type1, i : {x : Type0} -> I x |- i : I Type0
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    DependentLink x = param(false, "x", Universe(0));
    context.add(new TypedBinding("i", Pi(x, Apps(Reference(context.get(0)), Reference(x)))));

    typeCheckExpr(context, "i", Apps(Reference(context.get(0)), Universe(0)), 1);
  }

  @Test
  public void inferUnderLet() {
    // f : {A : Type0} -> (A -> A) -> A -> A |- let | x {A : Type0} (y : A -> A) = f y | z (x : Nat) = x \in x z :
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, vars("A"), Universe(0));
    context.add(new TypedBinding("f", Pi(params(A, param(Pi(Reference(A), Reference(A))), param(Reference(A))), Reference(A))));

    String term =
        "\\let\n" +
        "  | x {A : \\Type0} (y : A -> A) => f y\n" +
        "  | z (x : Nat) => x\n" +
        "\\in x z";
    CheckTypeVisitor.Result result = typeCheckExpr(context, term, null);
    assertEquals(Pi(Nat(), Nat()), result.type.normalize(NormalizeVisitor.Mode.WHNF));
  }

  @Test
  public void untypedLambda1() {
    // f : (A : \Type0) (a : A) -> Nat |- \x1 x2. f x1 x2
    DependentLink A = param("A", Universe(0));
    Expression type = Pi(params(A, param("a", Reference(A))), Nat());
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));
    typeCheckExpr(context, "\\lam x1 x2 => f x1 x2", null);
  }

  @Test
  public void untypedLambda2() {
    // f : (A : Type) (B : A -> Type) (a : A) -> B a |- \x1 x2 x3. f x1 x2 x3
    DependentLink A = param("A", Universe(0));
    DependentLink params = params(A, param("B", Pi(Reference(A), Universe(0))), param("a", Reference(A)));
    Expression type = Pi(params, Apps(Reference(params.getNext()), Reference(params.getNext().getNext())));
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\lam x1 x2 x3 => f x1 x2 x3", null);
    assertEquals(type, result.type);
  }

  @Test
  public void untypedLambdaError1() {
    // f : (A : \Type0) (a : A) -> Nat |- \x1 x2. f x2 x1
    DependentLink A = param("A", Universe(0));
    Expression type = Pi(params(A, param("a", Reference(A))), Nat());
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));
    typeCheckExpr(context, "\\lam x1 x2 => f x2 x1", null, -1);
  }

  @Test
  public void untypedLambdaError2() {
    // f : (A : Type0) (B : A -> Type0) (a : A) -> B a |- \x1 x2 x3. f x2 x1 x3
    DependentLink A = param("A", Universe(0));
    DependentLink params = params(A, param("B", Pi(Reference(A), Universe(0))), param("a", Reference(A)));
    Expression type = Pi(params, Apps(Reference(params.getNext()), Reference(params.getNext().getNext())));
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));

    typeCheckExpr(context, "\\lam x1 x2 x3 => f x2 x1 x3", null, -1);
  }

  @Test
  public void inferLater() {
    // f : {A : \Type0} (B : \Type1) -> A -> B -> A |- f Nat (\lam x => x) 0 : Nat -> Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    DependentLink B = param(true, "B", Universe(1));
    A.setNext(B);
    B.setNext(params(param(Reference(A)), param(Reference(B))));
    context.add(new TypedBinding("f", Pi(A, Reference(A))));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "f Nat (\\lam x => x) 0", Pi(Nat(), Nat()));
    assertNotNull(result);
  }

  @Test
  public void inferUnderPi() {
    typeCheckClass(
        "\\static \\function ($) {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
        "\\static \\function foo (A : \\Type0) (B : A -> \\Type0) (f : \\Pi (a : A) -> B a) (a' : A) => f $ a'", -1);
  }

  @Test
  public void inferUnderPiExpected() {
    typeCheckClass(
        "\\static \\function ($) {X Y : \\Type0} (f : X -> Y) (x : X) => f x\n" +
        "\\static \\function foo (A : \\Type0) (B : A -> \\Type0) (f : \\Pi (a : A) -> B a) (a' : A) : B a' => f $ a'", -1);
  }

  @Test
  public void inferPathCon() {
    typeCheckDef("\\function f : 1 = 1 => path (\\lam _ => 0)", 1);
  }

  @Test
  public void inferPathCon0() {
    typeCheckDef("\\function f : 1 = 1 => path {\\lam _ => Nat} (\\lam _ => 0)", -1);
  }

  @Test
  public void inferPathCon1() {
    typeCheckDef("\\function f : 1 = 1 => path {\\lam _ => Nat} {1} (\\lam _ => 0)", 1);
  }

  @Test
  public void inferPathCon2() {
    typeCheckDef("\\function f : 1 = 1 => path {\\lam _ => Nat} {0} (\\lam _ => 0)", 1);
  }

  @Test
  public void inferPathCon3() {
    typeCheckDef("\\function f : 1 = 1 => path {\\lam _ => Nat} {1} {1} (\\lam _ => 0)", 1);
  }

  @Test
  public void pathWithoutArg() {
    typeCheckDef("\\function f => path", 1);
  }

  @Test
  public void pathWithoutArg1() {
    typeCheckDef("\\function f : \\Pi {A : I -> \\Type0} {a : A left} {a' : A right} (\\Pi (i : I) -> A i) -> Path A a a' => path", 1);
  }

  @Test
  public void pathWithoutArg2() {
    typeCheckDef("\\function f => path {\\lam _ => Nat}", 1);
  }

  @Test
  public void pathWithoutArg3() {
    typeCheckDef("\\function f => path {\\lam _ => Nat} {0}", 1);
  }

  @Test
  public void pathWithoutArg4() {
    typeCheckDef("\\function f => path {\\lam _ => Nat} {0} {0}", 1);
  }

  @Test
  public void orderTest1() {
    typeCheckClass(
        "\\function idpOver (A : I -> \\Type0) (a : A left) : Path A a (coe A a right) => path (coe A a)\n" +
        "\\function test {A : \\Type0} (P : A -> \\Type0) {a a' : A} (q : a = a') (pa : P a) (i : I)\n" +
        "  => idpOver (\\lam (j : I) => P (q @ j)) pa @ i\n");
  }

  @Test
  public void orderTest2() {
    typeCheckClass(
        "\\function idpOver (A : I -> \\Type0) (a : A left) : Path A a (coe A a right) => path (coe A a)\n" +
        "\\function test {A : \\Type0} (P : A -> \\Type0) {a : A} (pa : P a) (i : I)\n" +
        "  => \\lam (a' : A) (q : a = a') => idpOver (\\lam (j : I) => P (q @ j)) pa @ i");
  }

  @Test
  public void differentLevels() {
    typeCheckClass(
        "\\static \\function F {lp : Lvl} (X : \\Type (lp,inf)) (B : X -> \\Type (lp,inf)) => zero\n" +
        "\\static \\function g {lp : Lvl} (X : \\Type (lp,inf)) => F X (\\lam _ => (=) X X)");
  }
}
