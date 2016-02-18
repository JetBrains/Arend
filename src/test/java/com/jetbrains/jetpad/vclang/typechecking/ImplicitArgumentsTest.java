package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.*;

public class ImplicitArgumentsTest {
  @Test
  public void inferId() {
    // f : {A : Type0} -> A -> A |- f 0 : N
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Reference(A), Reference(A)))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f 0", null);
    assertEquals(Apps(Apps(Reference(context.get(0)), Nat(), false, false), Zero()), result.expression);
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
    DependentLink g = param("g", Nat());
    assertEquals(Apps(Reference(context.get(0)), new ArgumentExpression(Nat(), false, true), new ArgumentExpression(Lam(g, Apps(Reference(g), Zero())), true, false)), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromFunction() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f suc : Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Reference(A)), Reference(A)))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f suc", null);
    assertEquals(Apps(Apps(Reference(context.get(0)), Nat(), false, false), Suc()), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromLam() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x. S) : Nat -> Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Reference(A)), Reference(A)))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f (\\lam x => suc)", null);
    assertEquals(Apps(Apps(Reference(context.get(0)), Pi(Nat(), Nat()), false, false), Lam(param("x", Nat()), Suc())), result.expression);
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
    assertEquals(Apps(Apps(Reference(context.get(0)), Pi(Pi(Nat(), Nat()), Nat()), false, false), arg), result.expression);
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
    assertEquals(Apps(Apps(Reference(context.get(0)), Nat(), false, false), Lam(x, Reference(x)), Lam(x, Reference(x))), result.expression);
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
    assertEquals(Apps(Apps(Reference(context.get(0)), Nat(), false, false), Lam(x, Reference(x)), Lam(x, Reference(x))), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromTheGoal() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat
    List<Binding> context = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    context.add(new TypedBinding("f", Pi(A, Pi(Nat(), Pi(Reference(A), Reference(A))))));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "f 0", Pi(Nat(), Nat()));
    assertEquals(Apps(Apps(Reference(context.get(0)), Nat(), false, false), Zero()), result.expression);
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

    assertNull(typeCheckExpr(context, "f i", null, 1));
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
    assertEquals(Apps(Reference(context.get(1)), Apps(Suc(), Zero()), false, false), result.expression);
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

    assertNull(typeCheckExpr(context, "i", Apps(Reference(context.get(0)), Universe(0)), 1));
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
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void untypedLambda() {
    // f : (A : Type) (B : A -> Type) (a : A) -> B a |- \x1 x2 x3. f x1 x2 x3
    DependentLink A = param("A", Universe());
    DependentLink params = params(A, param("B", Pi(Reference(A), Universe())), param("a", Reference(A)));
    Expression type = Pi(params, Apps(Reference(params.getNext()), Reference(params.getNext().getNext())));
    List<Binding> context = new ArrayList<>();
    context.add(new TypedBinding("f", type));

    CheckTypeVisitor.Result result = typeCheckExpr(context, "\\lam x1 x2 x3 => f x1 x2 x3", null);
    assertEquals(type, result.type);
  }

  @Test
  public void untypedLambdaError() {
    // f : (A : Type) (B : A -> Type) (a : A) -> B a |- \x1 x2 x3. f x2 x1 x3
    DependentLink A = param("A", Universe());
    DependentLink params = params(A, param("B", Pi(Reference(A), Universe())), param("a", Reference(A)));
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
}
