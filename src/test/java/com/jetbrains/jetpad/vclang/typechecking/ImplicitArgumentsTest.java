package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.VcError;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.term.error.InferredArgumentsMismatch;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ImplicitArgumentsTest {
  @Test
  public void inferId() {
    // f : {A : Type0} -> A -> A |- f 0 : N
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Index(0), Index(0)))));

    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(Prelude.getDefinitions(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(Apps(Index(0), Nat(), false, false), Zero()), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void unexpectedImplicit() {
    // f : N -> N |- f {0} 0 : N
    Expression expr = Apps(Apps(Index(0), Zero(), false, false), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(Nat(), Nat())));

    List<VcError> errors = new ArrayList<>();
    assertNull(expr.checkType(Prelude.getDefinitions(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void tooManyArguments() {
    // f : (x : N) {y : N} (z : N) -> N |- f 0 0 0 : N
    Expression expr = Apps(Index(0), Zero(), Zero(), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi("x", Nat(), Pi(false, "y", Nat(), Pi("z", Nat(), Nat())))));

    List<VcError> errors = new ArrayList<>();
    assertNull(expr.checkType(Prelude.getDefinitions(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void cannotInfer() {
    // f : {A B : Type0} -> A -> A |- f 0 : N
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(false, "B", Universe(0), Pi(Index(0), Index(0))))));

    List<VcError> errors = new ArrayList<>();
    assertNull(expr.checkType(Prelude.getDefinitions(), defs, null, errors));
    assertEquals(1, errors.size());
    assertTrue(errors.get(0) instanceof ArgInferenceError);
  }

  @Test
  public void cannotInferLam() {
    // f : {A : Type0} -> ((A -> Nat) -> Nat) -> A |- f (\g. g 0) : Nat
    Expression expr = Apps(Index(0), Lam("g", Apps(Index(0), Zero())));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Pi(Index(0), Nat()), Nat()), Index(0)))));

    List<VcError> errors = new ArrayList<>();
    assertNull(expr.checkType(Prelude.getDefinitions(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void inferFromFunction() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f S : Nat
    Expression expr = Apps(Index(0), Suc());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0)))));

    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(Prelude.getDefinitions(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(Apps(Index(0), Nat(), false, false), Suc()), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromLam() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x. S) : Nat -> Nat
    Expression expr = Apps(Index(0), Lam("x", Suc()));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0)))));

    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(Prelude.getDefinitions(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(Apps(Index(0), Pi(Nat(), Nat()), false, false), Lam("x", Suc())), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromLamType() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x (y : Nat -> Nat). y x) : (Nat -> Nat) -> Nat
    Expression arg = Lam(lamArgs(Name("x"), Tele(vars("y"), Pi(Nat(), Nat()))), Apps(Index(0), Index(1)));
    Expression expr = Apps(Index(0), arg);
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0)))));

    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(Prelude.getDefinitions(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(Apps(Index(0), Pi(Pi(Nat(), Nat()), Nat()), false, false), arg), result.expression);
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), result.type);
  }

  @Test
  public void inferFromSecondArg() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\x:Nat. x) : Nat
    Expression expr = Apps(Index(0), Lam("x", Index(0)), Lam(lamArgs(Tele(vars("x"), Nat())), Index(0)));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Pi(Index(0), Index(0)), Pi(Pi(Index(0), Nat()), Nat())))));

    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(Prelude.getDefinitions(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(Apps(Index(0), Nat(), false, false), Lam("x", Index(0)), Lam(lamArgs(Tele(vars("x"), Nat())), Index(0))), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromTheGoal() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Nat(), Pi(Index(0), Index(0))))));

    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(Prelude.getDefinitions(), defs, Pi(Nat(), Nat()), errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(Apps(Index(0), Nat(), false, false), Zero()), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromTheGoalError() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat -> Nat
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Nat(), Pi(Index(0), Index(0))))));

    List<VcError> errors = new ArrayList<>();
    assertNull(expr.checkType(Prelude.getDefinitions(), defs, Pi(Nat(), Pi(Nat(), Nat())), errors));
    assertEquals(1, errors.size());
    assertTrue(errors.get(0) instanceof InferredArgumentsMismatch);
  }

  @Test
  public void inferCheckTypeError() {
    // I : Type1 -> Type1, i : I Type0, f : {A : Type0} -> I A -> Nat |- f i : Nat
    Expression expr = Apps(Index(0), Index(1));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Universe(1), Universe(1))));
    defs.add(new TypedBinding("i", Apps(Index(0), Universe(0))));
    defs.add(new TypedBinding("f", Pi(false, "A", Universe(0), Pi(Apps(Index(2), Index(0)), Nat()))));

    List<VcError> errors = new ArrayList<>();
    assertNull(expr.checkType(Prelude.getDefinitions(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void inferTail() {
    // I : Nat -> Type0, i : {x : Nat} -> I (suc x) |- i : I (suc (suc 0))
    Expression expr = Index(0);
    Expression type = Apps(Index(1), Apps(Suc(), Apps(Suc(), Zero())));
    List<Binding> defs = new ArrayList<>();
    defs.add(new TypedBinding("I", Pi(Nat(), Universe(0))));
    defs.add(new TypedBinding("i", Pi(false, "x", Nat(), Apps(Index(1), Apps(Suc(), Index(0))))));

    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(Prelude.getDefinitions(), defs, type, errors);
    assertEquals(0, errors.size());
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

    List<VcError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(Prelude.getDefinitions(), defs, type.liftIndex(0, 1), errors);
    assertEquals(0, errors.size());
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

    List<VcError> errors = new ArrayList<>();
    assertNull(expr.checkType(Prelude.getDefinitions(), defs, Apps(Index(1), Universe(0)), errors));
    assertEquals(1, errors.size());
  }
}
