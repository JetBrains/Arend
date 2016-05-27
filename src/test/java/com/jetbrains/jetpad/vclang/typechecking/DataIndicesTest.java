package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static org.junit.Assert.assertEquals;

public class DataIndicesTest {
  @Test
  public void vectorTest() {
    typeCheckClass(
      "\\static \\data Vector (n : Nat) (A : \\Type0)\n" +
      "  | Vector  zero   A => vnil\n" +
      "  | Vector (suc n) A => \\infixr 5 (:^) A (Vector n A)\n" +
      "\n" +
      "\\static \\function \\infixl 6\n" +
      "(+) (x y : Nat) : Nat <= \\elim x\n" +
      "  | zero => y\n" +
      "  | suc x' => suc (x' + y)\n" +
      "\n" +
      "\\static \\function\n" +
      "(+^) {n m : Nat} {A : \\Type0} (xs : Vector n A) (ys : Vector m A) : Vector (n + m) A <= \\elim n, xs\n" +
      "  | zero, vnil => ys\n" +
      "  | suc n', (:^) x xs' => x :^ xs' +^ ys\n" +
      "\n" +
      "\\static \\function\n" +
      "vnil-vconcat {n : Nat} {A : \\Type0} (xs : Vector n A) : vnil +^ xs = xs => path (\\lam _ => xs)");
  }

  @Test
  public void vectorTest2() {
    typeCheckClass(
      "\\static \\data Vector (n : Nat) (A : \\Type0)\n" +
      "  | Vector  zero   A => vnil\n" +
      "  | Vector (suc n) A => \\infixr 5 (:^) A (Vector n A)\n" +
      "\\static \\function id {n : Nat} (A : \\Type0) (v : Vector n A) => v\n" +
      "\\static \\function test => id Nat vnil");
  }

  @Test
  public void constructorTypeTest() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data NatVec (n : Nat)\n" +
        "  | NatVec zero => nil\n" +
        "  | NatVec (suc n) => cons Nat (NatVec n)");
    DataDefinition data = (DataDefinition) member.namespace.getDefinition("NatVec");
    assertEquals(Apps(DataCall(data), Zero()), data.getConstructor("nil").getType());
    DependentLink param = param(false, "n", Nat());
    param.setNext(params(param((String) null, Nat()), param((String) null, Apps(DataCall(data), Reference(param)))));
    assertEquals(Pi(param, Apps(DataCall(data), Suc(Reference(param)))), data.getConstructor("cons").getType());
  }

  @Test
  public void toAbstractTest() {
    NamespaceMember member = typeCheckClass(
        "\\data Fin (n : Nat)\n" +
        "  | Fin (suc n) => fzero\n" +
        "  | Fin (suc n) => fsuc (Fin n)\n" +
        "\\function f (n : Nat) (x : Fin n) => fsuc (fsuc x)");
    System.out.println(((LeafElimTreeNode) ((FunctionDefinition) member.namespace.getDefinition("f")).getElimTree()).getExpression());
  }
}
