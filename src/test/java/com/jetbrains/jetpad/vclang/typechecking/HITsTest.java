package com.jetbrains.jetpad.vclang.typechecking;

import org.junit.Test;

public class HITsTest extends TypeCheckingTestCase {
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
