package org.arend.typechecking.definition;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.NotEnoughPatternsError;
import org.junit.Test;

public class HITsTest extends TypeCheckingTestCase {
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
    typeCheckModule("\\data S1 | loop : base = base | base", 1);
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
  public void goalTest() {
    typeCheckModule(
      "\\data S1 | base | loop : base = base\n" +
      "\\func f : base = base => loop\n" +
      "\\func g (x : S1) : S1\n" +
      "  | base => base\n" +
      "  | loop => {?}", 1);
    assertThatErrorsAre(Matchers.goal(0));
  }

  @Test
  public void depTest() {
    typeCheckModule(
      "\\data S1 | base | loop : base = base\n" +
      "\\func f : base = base => loop\n" +
      "\\func g (x : S1) : x = x\n" +
      "  | base => idp\n" +
      "  | loop => path (\\lam i => idp)");
  }

  @Test
  public void s1TestError() {
    typeCheckModule(
      "\\data S1 | base | base' | loop : base = base\n" +
      "\\func g (x : S1) : S1\n" +
      "  | base => base'\n" +
      "  | base' => base'\n" +
      "  | loop => loop", 2);
  }

  @Test
  public void s1TestError2() {
    typeCheckModule(
      "\\data S1 | base | loop I : base = base\n" +
      "\\func f : base = base => loop left\n" +
      "\\func g (x : S1) : S1\n" +
      "  | base => base\n" +
      "  | loop => f", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEnoughPatternsError.class));
  }

  @Test
  public void constructorWithConditions() {
    typeCheckModule(
      "\\data S1 | base | loop : base = base\n" +
      "\\data S1' | base' | loop' Nat : base' = base' \\with { | zero => idp }\n" +
      "\\func f : base' = base' => loop' 0\n" +
      "\\func f' => loop'\n" +
      "\\func f'' : Nat -> base' = base' => \\lam n => f' n\n" +
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
      "\\data S1' | base' | loop' Nat : base' = base' \\with { | zero => idp }\n" +
      "\\func g (x : S1') : S1\n" +
      "  | base' => base\n" +
      "  | loop' _ => loop", 1);
  }

  @Test
  public void s2Test() {
    typeCheckModule(
      "\\func idpe {A : \\Type} (a : A) : a = a => idp\n" +
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
      "\\func idpe {A : \\Type} (a : A) : a = a => idp\n" +
      "\\data S2 | base | loop I : base = base \\with { | left => idp | right => idp }\n" +
      "\\func f : I -> base = base => \\lam i => loop i\n" +
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
      "\\func g (d : D) : Nat | con => 1 | con' => path (\\lam _ => 0)", 2);
  }

  @Test
  public void square() {
    typeCheckModule(
      "\\data Square\n" +
      "  | v00 | v01 | v10 | v11\n" +
      "  | v-0 : v00 = v10 | v-1 : v01 = v11 | v0- : v00 = v01 | v1- : v10 = v11\n" +
      "  | square : Path (\\lam i => v-0 @ i = v-1 @ i) v0- v1-\n" +
      "\\func f : Path (\\lam i => v-0 @ i = v-1 @ i) v0- v1- => square\n" +
      "\\func g {A : \\Type} (a : A) (s : Square) (p : a = a) : A \\elim s\n" +
      "  | v00 => a | v01 => a | v10 => a | v11 => a\n" +
      "  | v0- => p | v1- => p | v-0 => idp | v-1 => idp\n" +
      "  | square => idp {_} {p}");
  }

  @Test
  public void threeArgs() {
    typeCheckModule(
      "\\data S1 | base | loop : base = base\n" +
      "\\func f : base = base => loop\n" +
      "\\func g (x y z : S1) (p : Path (\\lam i => loop i = loop i) (path loop) (path loop))\n" +
      "         (q : Path (\\lam i => Path (\\lam j => p i j = p i j) (p i) (p i)) p p) : S1 \\elim x, y, z\n" +
      "  | base, base, base => base\n" +
      "  | loop, base, base => loop\n" +
      "  | base, loop, base => loop\n" +
      "  | base, base, loop => loop\n" +
      "  | base, loop, loop => p\n" +
      "  | loop, base, loop => p\n" +
      "  | loop, loop, base => p\n" +
      "  | loop, loop, loop => q");
  }

  @Test
  public void recursiveFunction() {
    typeCheckModule(
      "\\data D | con1 | con2 D | con3 D | con4 (d : D) : con2 (con3 d) = con3 (con2 d)\n" +
      "\\func test (d : D) : Nat\n" +
      "  | con1 => 0\n" +
      "  | con2 d => test d\n" +
      "  | con3 d => test d\n" +
      "  | con4 d => idp");
  }

  @Test
  public void sideNotConstructor() {
    typeCheckModule(
      "\\data D | con1 | con2 D | con3 (d : D) : con2 d = d \n" +
      "\\func test (d : D) : Nat\n" +
      "  | con1 => 0\n" +
      "  | con2 d => test d\n" +
      "  | con3 d => idp");
  }

  @Test
  public void freeVarTest() {
    typeCheckModule(
      "\\data D | con1 | con2 | con3 : con1 = con2\n" +
      "\\func test (d : D) (s : d = con1) : Nat \\elim d\n" +
      "  | con1 => 0\n" +
      "  | con2 => 0\n" +
      "  | con3 => idp", 1);
  }
}
