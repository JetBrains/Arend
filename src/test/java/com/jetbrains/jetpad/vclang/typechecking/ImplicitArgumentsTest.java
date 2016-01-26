package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.InferredArgumentsMismatch;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ImplicitArgumentsTest {
  @Test
  public void inferId() {
    // f : {A : Type0} -> A -> A |- f 0 : N
    Concrete.Expression expr = cApps(cVar("f"), cZero());
    List<Binding> defs = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Reference(A), Reference(A)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Reference(defs.get(0)), Nat(), false, false), Zero()), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void unexpectedImplicit() {
    // f : N -> N |- f {0} 0 : N
    Concrete.Expression expr = cApps(cApps(cVar("f"), cZero(), false, false), cZero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(Nat(), Nat())));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void tooManyArguments() {
    // f : (x : N) {y : N} (z : N) -> N |- f 0 0 0 : N
    Concrete.Expression expr = cApps(cVar("f"), cZero(), cZero(), cZero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(param("x", Nat()), Pi(param(false, "y", Nat()), Pi(param("z", Nat()), Nat())))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void cannotInfer() {
    // f : {A B : Type0} -> A -> A |- f 0 : N
    Concrete.Expression expr = cApps(cVar("f"), cZero());
    List<Binding> defs = new ArrayList<>();
    DependentLink params = param(false, vars("A", "B"), Universe(0));
    defs.add(new TypedBinding("f", Pi(params, Pi(Reference(params), Reference(params)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null));
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof ArgInferenceError);
  }

  @Test
  public void cannotInferLam() {
    // f : {A : Type0} -> ((A -> Nat) -> Nat) -> A |- f (\g. g 0) : Nat
    Concrete.Expression expr = cApps(cVar("f"), cLam("g", cApps(cVar("g"), cZero())));
    List<Binding> defs = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Pi(Pi(Reference(A), Nat()), Nat()), Reference(A)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void inferFromFunction() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f S : Nat
    Concrete.Expression expr = cApps(cVar("f"), cSuc());
    List<Binding> defs = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Reference(A)), Reference(A)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Reference(defs.get(0)), Nat(), false, false), Suc()), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromLam() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x. S) : Nat -> Nat
    Concrete.Expression expr = cApps(cVar("f"), cLam("x", cSuc()));
    List<Binding> defs = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Reference(A)), Reference(A)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Reference(defs.get(0)), Pi(Nat(), Nat()), false, false), Lam(param("x", Nat()), Suc())), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromLamType() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x (y : Nat -> Nat). y x) : (Nat -> Nat) -> Nat
    Concrete.Expression carg = cLam(cargs(cName("x"), cTele(cvars("y"), cPi(cNat(), cNat()))), cApps(cVar("y"), cVar("x")));
    Concrete.Expression expr = cApps(cVar("f"), carg);
    List<Binding> defs = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Pi(Nat(), Reference(A)), Reference(A)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    DependentLink params = params(param("x", Nat()), param("y", Pi(Nat(), Nat())));
    Expression arg = Lam(params, Apps(Reference(params.getNext()), Reference(params)));
    assertEquals(Apps(Apps(Reference(defs.get(0)), Pi(Pi(Nat(), Nat()), Nat()), false, false), arg), result.expression);
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), result.type);
  }

  @Test
  public void inferFromSecondArg() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\x:Nat. x) : Nat
    Concrete.Expression expr = cApps(cVar("f"), cLam("x", cVar("x")), cLam(cargs(cTele(cvars("x"), cNat())), cVar("x")));
    List<Binding> defs = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Pi(Reference(A), Reference(A)), Pi(Pi(Reference(A), Nat()), Nat())))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    DependentLink x = param("x", Nat());
    assertEquals(Apps(Apps(Reference(defs.get(0)), Nat(), false, false), Lam(x, Reference(x)), Lam(x, Reference(x))), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromTheGoal() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat
    Concrete.Expression expr = cApps(cVar("f"), cZero());
    List<Binding> defs = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Nat(), Pi(Reference(A), Reference(A))))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(defs, errorReporter).build(), Pi(Nat(), Nat()));
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Reference(defs.get(0)), Nat(), false, false), Zero()), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromTheGoalError() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat -> Nat
    Concrete.Expression expr = cApps(cVar("f"), cZero());
    List<Binding> defs = new ArrayList<>();
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Nat(), Pi(Reference(A), Reference(A))))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.accept(new CheckTypeVisitor.Builder(defs, errorReporter).build(), Pi(Nat(), Pi(Nat(), Nat()))));
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof InferredArgumentsMismatch);
  }

  @Test
  public void inferCheckTypeError() {
    // I : Type1 -> Type1, i : I Type0, f : {A : Type0} -> I A -> Nat |- f i : Nat
    Concrete.Expression expr = cApps(cVar("f"), cVar("i"));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    defs.add(new TypedBinding("i", Apps(Reference(defs.get(0)), Universe(0))));
    DependentLink A = param(false, "A", Universe(0));
    defs.add(new TypedBinding("f", Pi(A, Pi(Apps(Reference(defs.get(0)), Reference(A)), Nat()))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.accept(new CheckTypeVisitor.Builder(defs, errorReporter).build(), null));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void inferTail() {
    // I : Nat -> Type0, i : {x : Nat} -> I (suc x) |- i : I (suc (suc 0))
    Concrete.Expression expr = cVar("i");
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    DependentLink x = param(false, "x", Nat());
    defs.add(new TypedBinding("i", Pi(x, Apps(Reference(defs.get(0)), Apps(Suc(), Reference(x))))));
    Expression type = Apps(Reference(defs.get(0)), Apps(Suc(), Apps(Suc(), Zero())));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(defs, errorReporter).build(), type);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Reference(defs.get(1)), Apps(Suc(), Zero()), false, false), result.expression);
    assertEquals(type, result.type);
  }

  @Test
  public void inferTail2() {
    // I : Nat -> Type0, i : {x : Nat} -> I x |- i : {x : Nat} -> I x
    Concrete.Expression expr = cVar("i");
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    DependentLink x = param(false, "x", Nat());
    Expression type = Pi(x, Apps(Reference(defs.get(0)), Reference(x)));
    defs.add(new TypedBinding("i", type));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(defs, errorReporter).build(), type);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Reference(defs.get(1)), result.expression);
    assertEquals(type, result.type);
  }

  @Test
  public void inferTailError() {
    // I : Type1 -> Type1, i : {x : Type0} -> I x |- i : I Type0
    Concrete.Expression expr = cVar("i");
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    DependentLink x = param(false, "x", Universe(0));
    defs.add(new TypedBinding("i", Pi(x, Apps(Reference(defs.get(0)), Reference(x)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.accept(new CheckTypeVisitor.Builder(defs, errorReporter).build(), Apps(Reference(defs.get(0)), Universe(0))));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void inferUnderLet() {
    // f : {A : Type0} -> (A -> A) -> A -> A |- let | x {A : Type0} (y : A -> A) = f y | z (x : Nat) = x \in x z :
    Concrete.Expression expr = cLet(clets(
        clet("x", cargs(cTele(false, cvars("A"), cUniverse(0)), cTele(cvars("y"), cPi(cVar("A"), cVar("A")))), cApps(cVar("f"), cVar("y"))),
        clet("z", cargs(cTele(cvars("x"), cNat())), cVar("x"))), cApps(cVar("x"), cVar("z")));
    DependentLink A = param(false, vars("A"), Universe(0));
    List<Binding> defs = new ArrayList<Binding>(Collections.singleton(new TypedBinding("f", Pi(params(A, param(Pi(Reference(A), Reference(A))), param(Reference(A))), Reference(A)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = expr.accept(new CheckTypeVisitor.Builder(defs, errorReporter).build(), null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void untypedLambda() {
    // f : (A : Type) (B : A -> Type) (a : A) -> B a |- \x1 x2 x3. f x1 x2 x3
    Concrete.Expression expr = cLam(cargs(cName("x1"), cName("x2"), cName("x3")), cApps(cVar("f"), cVar("x1"), cVar("x2"), cVar("x3")));
    DependentLink A = param("A", Universe());
    DependentLink params = params(A, param("B", Pi(Reference(A), Universe())), param("a", Reference(A)));
    Expression type = Pi(params, Apps(Reference(params.getNext()), Reference(params.getNext().getNext())));

    List<Binding> context = new ArrayList<Binding>(Collections.singleton(new TypedBinding("f", type)));
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(context, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(type, result.type);
  }

  @Test
  public void untypedLambdaError() {
    // f : (A : Type) (B : A -> Type) (a : A) -> B a |- \x1 x2 x3. f xx x1 x3
    Concrete.Expression expr = cLam(cargs(cName("x1"), cName("x2"), cName("x3")), cApps(cVar("f"), cVar("x2"), cVar("x1"), cVar("x3")));
    DependentLink A = param("A", Universe());
    DependentLink params = params(A, param("B", Pi(Reference(A), Universe())), param("a", Reference(A)));
    Expression type = Pi(params, Apps(Reference(params.getNext()), Reference(params.getNext().getNext())));

    List<Binding> context = new ArrayList<Binding>(Collections.singleton(new TypedBinding("f", type)));
    ListErrorReporter errorReporter = new ListErrorReporter();
    new CheckTypeVisitor.Builder(context, errorReporter).build().checkType(expr, null);
    assertEquals(1, errorReporter.getErrorList().size());
  }
}
