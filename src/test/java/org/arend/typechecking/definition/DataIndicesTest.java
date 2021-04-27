package org.arend.typechecking.definition;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class DataIndicesTest extends TypeCheckingTestCase {
  @Test
  public void vectorTest() {
    typeCheckModule(
      "\\data Vector (n : Nat) (A : \\Set0) \\elim n\n" +
      "  | zero  => vnil\n" +
      "  | suc n => \\infixr 5 :^ A (Vector n A)\n" +
      "\n" +
      "\\func \\infixl 6 + (x y : Nat) : Nat \\elim x\n" +
      "  | zero => y\n" +
      "  | suc x' => suc (x' + y)\n" +
      "\n" +
      "\\func \\infixr 9 +^ {n m : Nat} {A : \\Set0} (xs : Vector n A) (ys : Vector m A) : Vector (n + m) A \\elim n, xs\n" +
      "  | zero, vnil => ys\n" +
      "  | suc n', :^ x xs' => x :^ xs' +^ ys\n" +
      "\n" +
      "\\func vnil-vconcat {n : Nat} {A : \\Set0} (xs : Vector n A) : vnil +^ xs = xs => idp");
  }

  @Test
  public void vectorTest2() {
    typeCheckModule(
      "\\data Vector (n : Nat) (A : \\Set0) \\elim n\n" +
      "  | zero  => vnil\n" +
      "  | suc n => \\infixr 5 :^ A (Vector n A)\n" +
      "\\func id {n : Nat} (A : \\Set0) (v : Vector n A) => v\n" +
      "\\func test => id Nat vnil");
  }

  @Test
  public void constructorTypeTest() {
    typeCheckModule(
        "\\data NatVec Nat \\with\n" +
        "  | zero  => nil\n" +
        "  | suc n => cons Nat (NatVec n)");
    LevelPair set0 = LevelPair.SET0;
    DataDefinition data = (DataDefinition) getDefinition("NatVec");
    assertEquals(DataCall(data, set0, Zero()), data.getConstructor("nil").getTypeWithParams(new ArrayList<>(), set0));
    SingleDependentLink param = singleParams(false, vars("n"), Nat());
    List<DependentLink> consParams = new ArrayList<>();
    Expression consType = data.getConstructor("cons").getTypeWithParams(consParams, set0);
    assertEquals(Pi(param, Pi(Nat(), Pi(DataCall(data, set0, Ref(param)), DataCall(data, set0, Suc(Ref(param)))))), fromPiParameters(consType, consParams));
  }

  @Test
  public void toAbstractTest() {
    typeCheckModule(
        "\\data Fin Nat \\with\n" +
        "  | suc n => fzero\n" +
        "  | suc n => fsuc (Fin n)\n" +
        "\\func f (n : Nat) (x : Fin n) => fsuc (fsuc x)");
    assertEquals("fsuc {suc n} (fsuc {n} x)", ((Expression) Objects.requireNonNull(((FunctionDefinition) getDefinition("f")).getBody())).normalize(NormalizationMode.NF).toString());
  }
}
