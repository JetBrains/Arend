package com.jetbrains.jetpad.vclang.typechecking.constructions;


import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeOmega extends TypeCheckingTestCase {
  @Test
  public void dataExpansion() {
    typeCheckClass(
      "\\data D (A : \\Type) (a : A) | d (B : A -> \\Type)\n" +
      "\\function f {lp : Lvl} {lh : CNat} : (\\Pi {A : \\Type (lp, lh)} {a : A} -> (A -> \\Type (lp, lh)) -> D A a) => d\n" +
      "\\function test => f {\\Set0} {\\Prop} (\\lam _ => \\Type0)");
  }

  @Test
  public void notAllowedInTypeArgs() {
    typeCheckClass(
      "\\function f (A : \\Type -> \\Type) => 0\n" +
      "\\function g : \\Type -> \\Type => 0", 2);
  }

  @Test
  public void notAllowedAsExpression() {
    typeCheckClass(
      "\\function f => \\Type", 1);
  }

  @Test
  public void noEqualityOfTypes() {
    typeCheckClass(
            "\\function f (A B : \\Type) => A = B", 1);
  }
}
