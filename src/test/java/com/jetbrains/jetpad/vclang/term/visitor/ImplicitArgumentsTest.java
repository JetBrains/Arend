package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.error.ArgInferenceError;
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
    defs.add(new FunctionDefinition("f", new Signature(Pi(false, "A", Universe(0), Pi(Index(0), Index(0)))), Var("f")));

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
    defs.add(new FunctionDefinition("f", new Signature(Pi(Nat(), Nat())), Var("f")));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void tooManyArguments() {
    // f : (x : N) {y : N} (z : N) -> N |- f 0 0 0 : N
    Expression expr = Apps(Index(0), Zero(), Zero(), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new FunctionDefinition("f", new Signature(Pi("x", Nat(), Pi(false, "y", Nat(), Pi("z", Nat(), Nat())))), Var("f")));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void cannotInfer() {
    // f : {A B : Type0} -> A -> A |- f 0 : N
    Expression expr = Apps(Index(0), Zero());
    List<Binding> defs = new ArrayList<>();
    defs.add(new FunctionDefinition("f", new Signature(Pi(false, "A", Universe(0), Pi(false, "B", Universe(0), Pi(Index(0), Index(0))))), Var("f")));

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
    defs.add(new FunctionDefinition("f", new Signature(Pi(false, "A", Universe(0), Pi(Pi(Pi(Index(0), Nat()), Nat()), Index(0)))), Var("f")));

    List<TypeCheckingError> errors = new ArrayList<>();
    assertNull(expr.checkType(new HashMap<String, Definition>(), defs, null, errors));
    assertEquals(1, errors.size());
  }

  @Test
  public void inferFromFunction() {
    // f : {A : Type0} -> (Nat -> A) -> A |- f S : Nat
    Expression expr = Apps(Index(0), Suc());
    List<Binding> defs = new ArrayList<>();
    defs.add(new FunctionDefinition("f", new Signature(Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0)))), Var("f")));

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
    defs.add(new FunctionDefinition("f", new Signature(Pi(false, "A", Universe(0), Pi(Pi(Nat(), Index(0)), Index(0)))), Var("f")));

    List<TypeCheckingError> errors = new ArrayList<>();
    CheckTypeVisitor.OKResult result = expr.checkType(new HashMap<String, Definition>(), defs, null, errors);
    assertEquals(0, errors.size());
    assertEquals(Apps(App(Index(0), Pi(Nat(), Nat()), false), Lam("x", Suc())), result.expression);
    assertEquals(Pi(Nat(), Nat()), result.type);
  }

  @Test
  public void inferFromSecondArg() {
    // f : {A : Type0} -> (A -> A) -> (A -> Nat) -> Nat |- f (\x. x) (\x:Nat. x) : Nat
    // TODO: Write it.
  }
}
