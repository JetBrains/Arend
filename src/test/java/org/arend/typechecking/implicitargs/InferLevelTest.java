package org.arend.typechecking.implicitargs;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class InferLevelTest extends TypeCheckingTestCase {
  @Test
  public void noEquations() {
    // no equations
    // error: cannot infer ?l
    typeCheckModule(
        "\\func A => \\Type\n" +
        "\\func f => A");
  }

  @Test
  public void metaVarEquation() {
    // ?l <= ?l'
    // error: cannot infer ?l, ?l'
    typeCheckModule(
        "\\func A => \\Type\n" +
        "\\func f (A : \\Type) => A\n" +
        "\\func g => f A");
  }

  @Test
  public void universeTest() {
    typeCheckModule("\\func f (A : \\Type) : \\Type => A = A");
  }

  @Test
  public void belowTen() {
    // ?l <= 10
    // error: cannot infer ?l
    typeCheckModule(
        "\\func A => \\oo-Type\n" +
        "\\func f : \\oo-Type10 => A");
  }

  @Test
  public void belowParam() {
    // ?l <= c
    // error: cannot infer ?l
    typeCheckModule(
        "\\func A => \\Type\n" +
        "\\func f : \\Type (\\suc \\lp) (\\suc \\lh) => A");
  }

  @Test
  public void belowParam2() {
    typeCheckModule(
        "\\func A => \\Type\n" +
        "\\func f : \\oo-Type (\\suc \\lp) => A");
  }

  @Test
  public void belowParam3() {
    typeCheckModule(
        "\\func A => \\oo-Type\n" +
        "\\func f : \\oo-Type => A");
  }

  @Test
  public void belowParamError() {
    // ?l + 1 <= c
    // error: cannot infer ?l
    typeCheckModule(
        "\\func A => \\oo-Type\n" +
        "\\func f : \\oo-Type \\lp => A", 1);
  }

  @Test
  public void btwZeroAndParam() {
    // 0 <= ?l, 0 <= c
    // ok: ?l = 0
    typeCheckModule(
        "\\func f (A : \\oo-Type) => A\n" +
        "\\func g : \\oo-Type => f Nat");
  }

  @Test
  public void btwOneAndParam() {
    // 1 <= ?l, 1 <= c
    // error: cannot solve 1 <= c
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g : \\Type \\lp (\\suc \\lh) => f \\Type0", 1);
  }

  @Test
  public void btwOneAndParamWithH() {
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g : \\Type => f \\Type0");
  }

  @Test
  public void btwOneAndParamWithHError() {
    // 1 <= ?l, 1 <= c
    // error: cannot solve 1 <= c
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g : \\Type \\lp \\lh => f \\Type0", 2);
  }

  @Test
  public void btwZeroAndTen() {
    // 0 <= ?l <= 10
    // ok: ?l = 0
    typeCheckModule(
        "\\func f (A : \\oo-Type) => A\n" +
        "\\func g : \\oo-Type10 => f Nat");
  }

  @Test
  public void btwOneAndTen() {
    // 1 <= ?l <= 10
    // ok: ?l = 1
    typeCheckModule(
        "\\func f (A : \\oo-Type) => A\n" +
        "\\func g : \\oo-Type10 => f \\oo-Type0");
  }

  @Test
  public void greaterThanZero() {
    // 0 <= ?l
    // ok: ?l = 0
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g => f Nat");
  }

  @Test
  public void greaterThanOne() {
    // 1 <= ?l
    // ok: ?l = 1
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g => f \\Type0");
  }

  @Test
  public void propImpredicative() {
    typeCheckModule("\\func g (X : \\Set10) (P : X -> \\Prop) : \\Prop => \\Pi (a : X) -> P a");
  }

  @Test
  public void propImpredicative2() {
    typeCheckModule(
      "\\func f (X : \\Set10) (P : X -> \\Type) => \\Pi (a : X) -> P a\n" +
      "\\func g (X : \\Set10) (P : X -> \\Prop) : \\Prop => f X P", 1);
  }

  @Test
  public void levelOfPath() {
    typeCheckModule("\\func f (X : \\Set10) (x : X) : \\Prop => x = x");
  }

  @Test
  public void levelOfPath2() {
    typeCheckModule("\\func f (X : \\Set10) (x : X) : \\1-Type1 => x = x -> \\Set0", 1);
  }

  @Test
  public void levelOfPath3() {
    typeCheckModule("\\func f (X : \\Set10) (x : X) : \\1-Type1 => (x = x : \\Prop) -> \\Set0");
  }

  @Test
  public void constantUpperBound() {
    typeCheckModule(
      "\\func f (A : \\Type) => A\n" +
      "\\func g (B : \\Type) : \\Set => f B", 1);
  }

  @Test
  public void expectedType() {
    typeCheckModule(
      "\\func X => \\Type\n" +
      "\\func f : X => \\Type"
    );
  }

  @Test
  public void parameters() {
    typeCheckModule(
      "\\func X => \\Type\n" +
      "\\func f (A : X) => 0\n" +
      "\\func g => f \\Set0"
    );
  }

  @Test
  public void lhLessThanInf() {
    typeCheckModule(
      "\\func f (A : \\Type) (a a' : A) (p : a = a') => p\n" +
      "\\func X : \\oo-Type => Nat\n" +
      "\\func g : X = X => f \\Type X X (path (\\lam _ => X))");
  }

  @Test
  public void pLevelTest() {
    typeCheckModule(
      "\\func idp  {A : \\Type} {a : A} =>\n" +
      "  path (\\lam _ => a)\n" +
      "\\func squeeze1 (i j : I) : I =>\n" +
      "  coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
      "\\func squeeze (i j : I) =>\n" +
      "  coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
      "\\func psqueeze {A : \\Type} {a a' : A} (p : a = a') (i : I) : a = p @ i =>\n" +
      "  path (\\lam j => p @ squeeze i j)\n" +
      "\\func Jl {A : \\Type} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type) (b : B a idp) {a' : A} (p : a = a') : B a' p =>\n" +
      "  coe (\\lam i => B (p @ i) (psqueeze p i)) b right\n" +
      "\\func foo (A : \\Type) (a0 a1 : A) (p : a0 = a1) =>\n" +
      "  Jl (\\lam _ q => (idp {A} {a0} = idp {A} {a0}) = (q = q)) idp p");
  }

  @Test
  public void classLevelTest() {
    typeCheckModule(
      "\\class A {\n" +
      "  | X : \\oo-Type\n" +
      "}\n" +
      "\\func f : A (\\levels 0 _) => \\new A { | X => \\oo-Type0 }", 1);
  }

  @Test
  public void setIsNotProp() {
    typeCheckDef(
      "\\func isSur {A B : \\Set} (f : A -> B) : \\Prop =>\n" +
      "  \\Pi (b : B) -> \\Sigma (a : A) (b = f a)", 1);
  }

  @Test
  public void idTest() {
    typeCheckModule(
      "\\class Functor (F : \\Type -> \\Type)\n" +
      "  | fmap {A B : \\Type} : (A -> B) -> F A -> F B\n" +
      "\n" +
      "\\data Maybe (A : \\Type) | nothing | just A\n" +
      "\\func id' {A : \\Type} (a : A) => a\n" +
      "\\func idTest : \\Type1 => id' (\\suc \\lp) (Functor Maybe)", 1);
  }

  @Test
  public void idTest2() {
    typeCheckModule(
      "\\class Functor (F : \\Type -> \\Type)\n" +
      "  | fmap {A B : \\Type} : (A -> B) -> F A -> F B\n" +
      "\n" +
      "\\data Maybe (A : \\Type) | nothing | just A\n" +
      "\\func id' {A : \\Type} (a : A) => a\n" +
      "\\func idTest : \\Type2 => id' (\\suc (\\suc \\lp)) (Functor Maybe)", 1);
  }

  @Test
  public void idTest3() {
    typeCheckModule(
      "\\class Functor (F : \\Type -> \\Type)\n" +
      "  | fmap {A B : \\Type} : (A -> B) -> F A -> F B\n" +
      "\n" +
      "\\data Maybe (A : \\Type) | nothing | just A\n" +
      "\\func id' {A : \\Type} (a : A) => a\n" +
      "\\func idTest => id' (\\suc (\\suc \\lp)) (Functor Maybe)");
  }

  @Test
  public void dataLevelsTest1() {
    typeCheckModule(
      "\\data D | con \\Type\n" +
      "\\func f (d : D \\levels 1 _) : D \\levels 0 _ => d", 1);
  }

  @Test
  public void dataLevelsTest2() {
    typeCheckModule(
      "\\data D | con \\Type\n" +
      "\\func fromD (d : D) : \\Type | con A => A\n" +
      "\\func ddd : \\Type0 => fromD (con \\Type0)", 1);
  }

  @Test
  public void funcLevelsTest() {
    typeCheckModule(
      "\\func F => \\Type\n" +
      "\\func f (d : F \\levels 1 _) : F \\levels 0 _ => d", 1);
  }
}
