package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.term.error.InferedArgumentsMismatch;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ImplicitArgumentsTest {
  @Test
  public void inferId() {
    // f : {A : Type0} -> A -> A |- f 0 : N
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Index(0), Index(0))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(App(Index(0), Nat(), false), Zero()), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void unexpectedImplicit() {
    // f : N -> N |- f {0} 0 : N
    Expression expr = Apps(App(Index(0), Zero(), false), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(Nat(), Nat()))));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void tooManyArguments() {
    // f : (x : N) {y : N} (z : N) -> N |- f 0 0 0 : N
    Expression expr = Apps(Index(0), Zero(), Zero(), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi("x", Nat(), Pi(false, "y", Nat(), Pi("z", Nat(), Nat()))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void cannotInfer() {
    // f : {A B : Type0} -> A -> A |- f 0 : N
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(false, "B", Universe(0), Pi(Index(0), Index(0)))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, null, errors));
    assertEquals(1, errors.size());
    assertTrue(errors.get(0) instanceof ArgInferenceError);
  }

  @Test
  public void cannotInferLam() {
    // f : {A : Type0} -> ((A -> Nat) -> Nat) -> A |- f (\g. g 0) : Nat
    Expression expr = Apps(Index(0), Lam("g", Apps(Index(0), Zero())));
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Pi(Pi(Index(0), Nat()), Nat()), Index(0))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void inferFromFunction() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f S : Nat
    Expression expr = Apps(Index(0), Suc());
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(App(Index(0), Nat(), false), Suc()), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromLam() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x. S) : Nat -> Nat
    Expression expr = Apps(Index(0), Lam("x", Suc()));
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(App(Index(0), Pi(Nat(), Nat()), false), Lam("x", Suc())), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromLamType() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f (\x (y : Nat -> Nat). y x) : (Nat -> Nat) -> Nat
    Expression arg = Lam(lamArgs(Name("x"), Tele(vars("y"), Pi(Nat(), Nat()))), Apps(Index(0), Index(1)));
    Expression expr = Apps(Index(0), arg);
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(App(Index(0), Pi(Pi(Nat(), Nat()), Nat()), false), arg), result.expression);
    assertEquals(Pi(Pi(Nat(), Nat()), Nat()), result.type);
  }

  @Test
  public void inferFromSecondArg() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\x:Nat. x) : Nat
    Expression expr = Apps(Index(0), Lam("x", Index(0)), Lam(lamArgs(Tele(vars("x"), Nat())), Index(0)));
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Pi(Index(0), Index(0)), Pi(Pi(Index(0), Nat()), Nat()))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(App(Index(0), Nat(), false), Lam("x", Index(0)), Lam(lamArgs(Tele(vars("x"), Nat())), Index(0))), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void inferFromTheGoal() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Nat(), Pi(Index(0), Index(0)))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, Pi(Nat(), Nat()), errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(App(Index(0), Nat(), false), Zero()), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromTheGoalError() {
    // f : {A : Type0} -> Nat -> A -> A |- f 0 : Nat -> Nat -> Nat
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Nat(), Pi(Index(0), Index(0)))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, Pi(Nat(), Pi(Nat(), Nat())), errors));
    assertEquals(1, errors.size());
    assertTrue(errors.get(0) instanceof InferedArgumentsMismatch);
  }

  @Test
  public void inferCheckTypeError() {
    // I : Type1 -> Type1, i : I Type0, f : {A : Type0} -> I A -> Nat |- f i : Nat
    Expression expr = Apps(Index(0), Index(1));
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("I", new Signature(Pi(Universe(1), Universe(1)))));
    defs.add(new Binding("i", new Signature(Apps(Index(0), Universe(0)))));
    defs.add(new Binding("f", new Signature(Pi(false, "A", Universe(0), Pi(Apps(Index(2), Index(0)), Nat())))));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void inferTail() {
    // I : Nat -> Type0, i : {x : Nat} -> I (suc x) |- i : I (suc (suc 0))
    Expression expr = Index(0);
    Expression type = Apps(Index(1), Apps(Suc(), Apps(Suc(), Zero())));
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("I", new Signature(Pi(Nat(), Universe(0)))));
    defs.add(new Binding("i", new Signature(Pi(false, "x", Nat(), Apps(Index(1), Apps(Suc(), Index(0)))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, type, errors);
    assertEquals(0, errors.size());
    assertEquals(App(Index(0), Apps(Suc(), Zero()), false), result.expression);
    assertEquals(type, result.type);
  }

  @Test
  public void inferTail2() {
    // I : Nat -> Type0, i : {x : Nat} -> I x |- i : {x : Nat} -> I x
    Expression expr = Index(0);
    Expression type = Pi(false, "x", Nat(), Apps(Index(1), Index(0)));
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("I", new Signature(Pi(Nat(), Universe(0)))));
    defs.add(new Binding("i", new Signature(type)));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, type.liftIndex(0, 1), errors);
    assertEquals(0, errors.size());
    assertEquals(Index(0), result.expression);
    assertEquals(type.liftIndex(0, 1), result.type);
  }

  @Test
  public void inferTailError() {
    // I : Type1 -> Type1, i : {x : Type0} -> I x |- i : I Type0
    Expression expr = Index(0);
    List<Binding> defs = new ArrayList<>();
    defs.add(new Binding("I", new Signature(Pi(Universe(1), Universe(1)))));
    defs.add(new Binding("i", new Signature(Pi(false, "x", Universe(0), Apps(Index(1), Index(0))))));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, Apps(Index(1), Universe(0)), errors));
    assertEquals(1, errors.size());
  }
}
