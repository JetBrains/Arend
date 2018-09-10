package com.jetbrains.jetpad.vclang.typechecking.definition;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class Hits extends TypeCheckingTestCase {
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
      "\\func f' : Nat -> base' = base' => loop'\n" +
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
}
