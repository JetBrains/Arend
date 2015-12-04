package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
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
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Index(0), Index(0)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Index(0), Nat(), false, false), Zero()), result.expression);
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
    defs.add(new TypedBinding("f", Pi("x", Nat(), Pi(false, "y", Nat(), Pi("z", Nat(), Nat())))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void cannotInfer() {
    // f : {A B : Type0} -> A -> A |- f 0 : N
    Concrete.Expression expr = cApps(cVar("f"), cZero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(false, "B", Universe(0), Pi(Index(0), Index(0))))));

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
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Pi(Index(0), Nat()), Nat()), Index(0)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void inferFromFunction() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f S : Nat
    Concrete.Expression expr = cApps(cVar("f"), cSuc());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Index(0), Nat(), false, false), Suc()), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromLam() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x. S) : Nat -> Nat
    Concrete.Expression expr = cApps(cVar("f"), cLam("x", cSuc()));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Index(0), Pi(Nat(), Nat()), false, false), Lam("x", Nat(), Suc())), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromLamType() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x (y : Nat -> Nat). y x) : (Nat -> Nat) -> Nat
    Concrete.Expression carg = cLam(cargs(cName("x"), cTele(cvars("y"), cPi(cNat(), cNat()))), cApps(cVar("y"), cVar("x")));
    Concrete.Expression expr = cApps(cVar("f"), carg);
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    Expression arg = Lam(teleArgs(Tele(vars("x"), Nat()), Tele(vars("y"), Pi(Nat(), Nat()))), Apps(Index(0), Index(1)));
    assertEquals(Apps(Apps(Index(0), Pi(Pi(Nat(), Nat()), Nat()), false, false), arg), result.expression);
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), result.type);
  }

  @Test
  public void inferFromSecondArg() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\x:Nat. x) : Nat
    Concrete.Expression expr = cApps(cVar("f"), cLam("x", cVar("x")), cLam(cargs(cTele(cvars("x"), cNat())), cVar("x")));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Index(0), Index(0)), Pi(Pi(Index(0), Nat()), Nat())))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = new CheckTypeVisitor.Builder(defs, errorReporter).build().checkType(expr, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Index(0), Nat(), false, false), Lam("x", Nat(), Index(0)), Lam("x", Nat(), Index(0))), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromTheGoal() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Nat(), Pi(Index(0), Index(0))))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = expr.checkType(defs, Pi(Nat(), Nat()), errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Apps(Index(0), Nat(), false, false), Zero()), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromTheGoalError() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat -> Nat
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Nat(), Pi(Index(0), Index(0))))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.checkType(defs, Pi(Nat(), Pi(Nat(), Nat())), errorReporter));
    assertEquals(1, errorReporter.getErrorList().size());
    assertTrue(errorReporter.getErrorList().iterator().next() instanceof InferredArgumentsMismatch);
  }

  @Test
  public void inferCheckTypeError() {
    // I : Type1 -> Type1, i : I Type0, f : {A : Type0} -> I A -> Nat |- f i : Nat
    Expression expr = Apps(Index(0), Index(1));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    defs.add(new TypedBinding("i", Apps(Index(0), Universe(0))));
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Apps(Index(2), Index(0)), Nat()))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.checkType(defs, null, errorReporter));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void inferTail() {
    // I : Nat -> Type0, i : {x : Nat} -> I (suc x) |- i : I (suc (suc 0))
    Expression expr = Index(0);
    Expression type = Apps(Index(1), Apps(Suc(), Apps(Suc(), Zero())));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    defs.add(new TypedBinding("i", Pi(false, "x", Nat(), Apps(Index(1), Apps(Suc(), Index(0))))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = expr.checkType(defs, type, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Apps(Index(0), Apps(Suc(), Zero()), false, false), result.expression);
    assertEquals(type, result.type);
  }

  @Test
  public void inferTail2() {
    // I : Nat -> Type0, i : {x : Nat} -> I x |- i : {x : Nat} -> I x
    Expression expr = Index(0);
    Expression type = Pi(false, "x", Nat(), Apps(Index(1), Index(0)));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    defs.add(new TypedBinding("i", type));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = expr.checkType(defs, type.liftIndex(0, 1), errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(Index(0), result.expression);
    assertEquals(type.liftIndex(0, 1), result.type);
  }

  @Test
  public void inferTailError() {
    // I : Type1 -> Type1, i : {x : Type0} -> I x |- i : I Type0
    Expression expr = Index(0);
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    defs.add(new TypedBinding("i", Pi(false, "x", Universe(0), Apps(Index(1), Index(0)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    assertNull(expr.checkType(defs, Apps(Index(1), Universe(0)), errorReporter));
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void inferUnderLet() {
    // f : {A : Type0} -> (A -> A) -> A -> A |- let | x {A : Type0} (y : A -> A) = f y | z (x : Nat) = x \in x z :
    Expression expr = Let(lets(
            let("x", typeArgs(Tele(false, vars("A"), Universe(0)), Tele(vars("y"), Pi(Index(0), Index(0)))), Apps(Index(2), Index(0))),
            let("z", typeArgs(Tele(vars("x"), Nat())), Index(0))), Apps(Index(1), Index(0)));
    List<Binding> defs = new ArrayList<Binding>(Collections.singleton(new TypedBinding("f", Pi(typeArgs(Tele(false, vars("A"), Universe(0)), TypeArg(Pi(Index(0), Index(0))), TypeArg(Index(1))), Index(2)))));

    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.OKResult result = expr.checkType(defs, null, errorReporter);
    assertEquals(0, errorReporter.getErrorList().size());
    assertEquals(result.type, Pi(Nat(), Nat()));
  }
}
