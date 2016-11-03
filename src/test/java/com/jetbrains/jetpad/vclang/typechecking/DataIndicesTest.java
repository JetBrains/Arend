package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelArguments;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class DataIndicesTest extends TypeCheckingTestCase {
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
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data NatVec (n : Nat)\n" +
        "  | NatVec zero => nil\n" +
        "  | NatVec (suc n) => cons Nat (NatVec n)");
    DataDefinition data = (DataDefinition) result.getDefinition("NatVec");
    assertEquals(DataCall(data, new LevelArguments(), Zero()), data.getConstructor("nil").getTypeWithParams(new ArrayList<DependentLink>(), new LevelArguments()));
    DependentLink param = param(false, "n", Nat());
    param.setNext(params(param((String) null, Nat()), param((String) null, DataCall(data, new LevelArguments(), Reference(param)))));
    List<DependentLink> consParams = new ArrayList<>();
    Type consType = data.getConstructor("cons").getTypeWithParams(consParams, new LevelArguments());
    assertEquals(Pi(param, DataCall(data, new LevelArguments(), Suc(Reference(param)))), consType.fromPiParameters(consParams));
  }

  @Test
  public void toAbstractTest() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data Fin (n : Nat)\n" +
        "  | Fin (suc n) => fzero\n" +
        "  | Fin (suc n) => fsuc (Fin n)\n" +
        "\\static \\function f (n : Nat) (x : Fin n) => fsuc (fsuc x)");
    assertEquals("(Fin (suc (suc n))).fsuc ((Fin (suc n)).fsuc x)", ((LeafElimTreeNode) ((FunctionDefinition) result.getDefinition("f")).getElimTree()).getExpression().normalize(NormalizeVisitor.Mode.NF).toString());
  }
}
