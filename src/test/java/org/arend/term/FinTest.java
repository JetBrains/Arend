package org.arend.term;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

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
  public void getTypeModFailing() {
    typeCheckDef("\\func kiva : Fin 8 => Nat.mod 8 0", 1);
  }

  @Test
  public void getTypeMod() {
    typeCheckDef("\\func kiva : Fin 8 => Nat.mod 10 8");
    typeCheckDef("\\func kiwa : Nat.mod 10 8 = {Fin 8} 7 => idp");
  }

  @Test
  public void getTypeDivMod() {
    typeCheckDef("\\func emmmer (a : Nat) : \\Sigma Nat (Fin a) => Nat.divMod 10 (suc a)");
  }

  @Test(timeout = 5000)
  public void fromNatCoercion() {
    typeCheckDef("\\func darkflames => Fin.fromNat 1919810");
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
}
