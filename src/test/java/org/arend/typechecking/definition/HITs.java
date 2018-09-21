package org.arend.typechecking.definition;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class HITs extends TypeCheckingTestCase {
  @Test
  public void typedData() {
    typeCheckModule("\\data Nat' | zero' : Nat' | suc' Nat' : Nat'");
  }

  @Test
  public void typedDataError() {
    typeCheckModule("\\data Nat' | zero' : Nat | suc Nat : Nat'", 1);
  }

  @Test
  public void pathTypedDataError() {
    typeCheckModule("\\data S1 | base | loop : zero = zero", 1);
  }

  @Test
  public void pathTypedDataError2() {
    typeCheckModule("\\data S1 | loop : base = base | base", 2);
  }

  @Test
  public void s1Test() {
    typeCheckModule(
      "\\data S1 | base | loop : base = base\n" +
      "\\func f : base = base => loop\n" +
      "\\func g (x : S1) : S1\n" +
      "  | base => base\n" +
      "  | loop => f");
  }

  @Test
  public void s1TestError() {
    typeCheckModule(
      "\\data S1 | base | base' | loop : base = base\n" +
      "\\func g (x : S1) : S1\n" +
      "  | base => base'\n" +
      "  | base' => base'\n" +
      "  | loop => loop", 1);
  }

  @Test
  public void constructorWithConditions() {
    typeCheckModule(
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\data S1 | base | loop : base = base\n" +
      "\\data S1' | base' | loop' Nat : base' = base' \\with { | zero => idp }\n" +
      "\\func f : base' = base' => loop' 0\n" +
      "\\func f' => loop'\n" +
      "\\func f'' : Nat -> base' = base' => f'\n" +
      "\\func fTest : f = idp => idp\n" +
      "\\func g (x : S1') : S1\n" +
      "  | base' => base\n" +
      "  | loop' 0 => idp\n" +
      "  | loop' (suc _) => loop");
  }

  @Test
  public void constructorWithConditionsError() {
    typeCheckModule(
      "\\data S1 | base | loop : base = base\n" +
      "\\data S1' | base' | loop' Nat : base' = base' \\with { | zero => path (\\lam _ => base') }\n" +
      "\\func g (x : S1') : S1\n" +
      "  | base' => base\n" +
      "  | loop' _ => loop", 1);
  }

  @Test
  public void s2Test() {
    typeCheckModule(
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func idpe {A : \\Type} (a : A) => path (\\lam _ => a)\n" +
      "\\data S2 | base | loop : idpe base = idpe base\n" +
      "\\func f : idpe base = idpe base => loop\n" +
      "\\func fLeft : loop @ left = idpe base => idp\n" +
      "\\func fRight : loop @ right = idpe base => idp\n" +
      "\\func fTop : path (\\lam i => loop @ i @ left) = idpe base => idp\n" +
      "\\func fBottom : path (\\lam i => loop @ i @ right) = idpe base => idp\n" +
      "\\func g (x : S2) : S2\n" +
      "  | base => base\n" +
      "  | loop => path (\\lam i => path (\\lam j => loop @ j @ i))");
  }

  @Test
  public void mixedS2Test() {
    typeCheckModule(
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func idpe {A : \\Type} (a : A) => path (\\lam _ => a)\n" +
      "\\data S2 | base | loop I : base = base \\with { | left => idp | right => idp }\n" +
      "\\func f : I -> base = base => loop\n" +
      "\\func f' (i : I) : base = base => loop i\n" +
      "\\func fLeft : loop left = idpe base => idp\n" +
      "\\func fRight : loop right = idpe base => idp\n" +
      "\\func fTop : path (\\lam i => loop i @ left) = idpe base => idp\n" +
      "\\func fBottom : path (\\lam i => loop i @ right) = idpe base => idp\n" +
      "\\func g (x : S2) : S2\n" +
      "  | base => base\n" +
      "  | loop i => path (\\lam j => loop j @ i)");
  }

  @Test
  public void typed0() {
    typeCheckModule(
      "\\data D1 | con1 : D1\n" +
      "\\data D2 (n : Nat) | con2 : D2 n\n" +
      "\\data D3 (n : Nat) \\with | 0 => con3 : D3 0 | suc m => con3' : D3 (suc m)");
  }

  @Test
  public void typed0Error() {
    typeCheckDef("\\data D (n : Nat) | con : D 0", 1);
  }

  @Test
  public void typed0Error2() {
    resolveNamesDef("\\data D (n : Nat) \\with | 0 => con : D 0 | suc m => con' : D n", 1);
  }

  @Test
  public void typed0Error3() {
    typeCheckDef("\\data D (n : Nat) \\with | 0 => con : D 0 | suc m => con' : D m", 1);
  }

  @Test
  public void typed1() {
    typeCheckModule(
      "\\data D | con | con' : con = con\n" +
      "\\func f : con = con => con'\n" +
      "\\func g (d : D) : Nat | con => 0 | con' => path (\\lam _ => 0)");
  }

  @Test
  public void typed1Error() {
    typeCheckModule(
      "\\data D | con | con' : con = con\n" +
      "\\func g (d : D) : Nat | con => 1 | con' => path (\\lam _ => 0)", 1);
  }

  @Test
  public void typed2() {
    typeCheckModule(
      "\\data D | con | con' (p : con = con) : p = p\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func f : idp = idp => con' idp\n" +
      "\\func g (d : D) : Nat | con => 0 | con' p => idp\n" +
      "\\func h (d : D) : Nat | con => 0 | con' p => idp {_} {path (\\lam i => h (p @ i))}");
  }

  @Test
  public void square() {
    typeCheckModule(
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func \\infixr 9 *> {A : \\Type} {a a' a'' : A} (p : a = a') (q : a' = a'') => coe (\\lam i => a = q @ i) p right\n" +
      "\\data Square\n" +
      "  | v00 | v01 | v10 | v11\n" +
      "  | v-0 : v00 = v10 | v-1 : v01 = v11 | v0- : v00 = v01 | v1- : v10 = v11\n" +
      "  | square : v-0 *> v1- = v0- *> v-1\n" +
      "\\func f : v-0 *> v1- = v0- *> v-1 => square\n" +
      "\\func g (s : Square) : Nat\n" +
      "  | v00 => 0 | v01 => 0 | v10 => 0 | v11 => 0\n" +
      "  | v0- => idp | v1- => idp | v-0 => idp | v-1 => idp\n" +
      "  | square => idp {_} {idp}\n" +
      "\\func h (s : Square) (p : 0 = 0) (q : p *> p = p) \\elim s\n" +
      "  | v00 => 0 | v01 => 0 | v10 => 0 | v11 => 0\n" +
      "  | v0- => p | v-1 => idp | v-0 => p | v1- => p\n" +
      "  | square => q");
  }

  @Test
  public void square2() {
    typeCheckModule(
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\data Square\n" +
      "  | v00 | v01 | v10 | v11\n" +
      "  | v-0 : v00 = v10 | v-1 : v01 = v11 | v0- : v00 = v01 | v1- : v10 = v11\n" +
      "  | square : Path (\\lam i => v-0 @ i = v-1 @ i) v0- v1-\n" +
      "\\func f : Path (\\lam i => v-0 @ i = v-1 @ i) v0- v1- => square\n" +
      "\\func g (s : Square) (p : 0 = 0) : Nat \\elim s\n" +
      "  | v00 => 0 | v01 => 0 | v10 => 0 | v11 => 0\n" +
      "  | v0- => p | v1- => p | v-0 => idp | v-1 => idp\n" +
      "  | square => idp {_} {p}");
  }
}
