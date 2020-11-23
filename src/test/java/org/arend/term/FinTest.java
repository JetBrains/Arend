package org.arend.term;

import org.arend.Matchers;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.type.Type;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.arend.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class FinTest extends TypeCheckingTestCase {
  @Test
  public void emptySet() {
    typeCheckDef("\\func xy (_ : Fin 0) : Empty | ()" +
      "\\where \\data Empty");
  }

  @Test
  public void zeroAsBoundary() {
    typeCheckDef("\\func meow : Fin 0 => 0", 1);
  }

  @Test
  public void zeroDontAllowOne() {
    typeCheckDef("\\func nyan : Fin 0 => 1", 1);
  }

  @Test
  public void oneDontAllowOne() {
    typeCheckDef("\\func sekai : Fin 1 => 1", 1);
  }

  @Test
  public void literalFin() {
    typeCheckDef("\\func ren : Fin 1 => 0");
    typeCheckDef("\\func xyr : Fin 2 => 1");
    typeCheckDef("\\func xyren : Fin 101 => 100");
  }

  @Test
  public void getTypeModCoerce() {
    typeCheckDef("\\func kiva (a : Nat) : Fin 8 => Nat.mod a 0");
  }

  @Test
  public void getTypeModFailing() {
    typeCheckDef("\\func kiva (a : Nat) : Fin 0 => Nat.mod a 0", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void getTypeModFailing2() {
    typeCheckDef("\\func kiva (a : Nat) : Fin 8 => Nat.mod a 9", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void getTypeMod() {
    typeCheckDef("\\func kiva : Fin 8 => Nat.mod 10 8");
    typeCheckDef("\\func oyama (a : Nat) : Fin (suc a) => Nat.mod 114514 (suc a)");
    typeCheckDef("\\func kiwa : Nat.mod 10 8 = {Fin 8} 2 => idp {Fin 8} {2}");
  }

  @Test
  public void getTypeDivMod() {
    typeCheckDef("\\func emmmer (a : Nat) : \\Sigma Nat (Fin (suc a)) => Nat.divMod 10 (suc a)");
  }

  @Test(timeout = 5000)
  public void fromNatCoercion() {
    typeCheckDef("\\func darkflames : Fin 1919811 => Fin.fromNat 1919810");
  }

  @Test
  public void unfiniteFin() {
    typeCheckDef("\\func dalao : 0 = {Nat} (\\let | x : Fin 1 => 0 \\in x) => idp {Nat}");
    typeCheckDef("\\func julao : 0 = {Fin 1} (\\let | x : Fin 1 => 0 \\in x) => idp {Fin 1} {0}");
  }

  @Test
  public void weakenFin() {
    typeCheckDef("\\func julao : Fin 2 => 0");
    typeCheckDef("\\func juruo : Fin 514 => 114");
  }

  @Test
  public void matchOne() {
    typeCheckDef("\\func wsl (a : Nat) (_ : Fin 1) : Nat" +
      "  | x, zero => x");
  }

  @Test
  public void matchStuck() {
    typeCheckDef("\\func wsl (a : Nat) (_ : Fin a) : Nat" +
      "  | x, zero => x", 1);
  }

  @Test
  public void matchTwo() {
    typeCheckDef("\\func sdl (_ : Fin 2) : Nat" +
      "  | zero => 123" +
      "  | suc n => 666");
  }

  @Test
  public void modType() {
    assertEquals(Fin(7), typeCheckExpr("Nat.mod 17 7", null).type);
    assertEquals(divModType(Fin(7)), typeCheckExpr("Nat.divMod 17 7", null).type);
  }

  @Test
  public void modType2() {
    List<Binding> context = Collections.singletonList(new TypedBinding("n", Nat()));
    assertEquals(Fin(13), typeCheckExpr(context, "Nat.mod n 13", null).type);
    assertEquals(divModType(Fin(13)), typeCheckExpr(context, "Nat.divMod n 13", null).type);
  }

  @Test
  public void modType3() {
    List<Binding> context = Collections.singletonList(new TypedBinding("n", Nat()));
    assertEquals(Nat(), typeCheckExpr(context, "Nat.mod n 0", null).type);
    assertEquals(Prelude.DIV_MOD_TYPE, typeCheckExpr(context, "Nat.divMod n 0", null).type);
  }

  @Test
  public void modType4() {
    TypedBinding binding = new TypedBinding("n", Nat());
    Type type = Fin(Suc(new ReferenceExpression(binding)));
    assertEquals(type, typeCheckExpr(Collections.singletonList(binding), "Nat.mod n (suc n)", null).type);
    assertEquals(divModType(type), typeCheckExpr(Collections.singletonList(binding), "Nat.divMod n (suc n)", null).type);
  }

  @Test
  public void finSubtype() {
    typeCheckDef("\\func test (n : Nat) (x : Fin (n Nat.+ 2)) : Fin (n Nat.+ 5) => x");
  }

  @Test
  public void finSubtypeError() {
    typeCheckDef("\\func test (n : Nat) (x : Fin (n Nat.+ 3)) : Fin (n Nat.+ 2) => x", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void finSubtypeError2() {
    typeCheckDef("\\func test (n m : Nat) (x : Fin (n Nat.+ 2)) : Fin (m Nat.+ 3) => x", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }
}
