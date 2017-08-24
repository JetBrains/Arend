package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.FunCall;
import static com.jetbrains.jetpad.vclang.ExpressionFactory.Ref;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TypeCheckingTest extends TypeCheckingTestCase {
  @Test
  public void typeCheckDefinition() {
    typeCheckModule(
        "\\function x : Nat => zero\n" +
        "\\function y : Nat => x");
  }

  @Test
  public void typeCheckDefType() {
    typeCheckModule(
        "\\function x : \\Set0 => Nat\n" +
        "\\function y : x => zero");
  }

  @Test
  public void typeCheckInfixDef() {
    typeCheckModule(
        "\\function + : Nat -> Nat -> Nat => \\lam x y => x\n" +
        "\\function * : Nat -> Nat => \\lam x => x + zero");
  }

  @Test
  public void typeCheckConstructor1() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {1} {2} {3} = (D 1 {2} 3).con => idp");
  }

  @Test
  public void typeCheckConstructor1d() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {1} {2} {3} = (D 1 {2} 3).con => idp");
  }

  @Test
  public void typeCheckConstructor2() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {0} (path (\\lam _ => 1)) = (D 0).con idp => idp");
  }

  @Test
  public void typeCheckConstructor2d() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {0} (path (\\lam _ => 1)) = (D 0).con idp => idp");
  }

  @Test
  public void testEither() {
    typeCheckModule(
        "\\data Either (A B : \\Type0) | inl A | inr B\n" +
        "\\function fun {A B : \\Type0} (e : Either A B) : \\Set0 => \\elim e\n" +
        "  | inl _ => Nat\n" +
        "  | inr _ => Nat\n" +
        "\\function test : fun (inl {Nat} {Nat} 0) => 0");
  }

  @Test
  public void testPMap1() {
    typeCheckDef("\\function pmap {A B : \\Type1} {a a' : A} (f : A -> B) (p : a = a') : (f a = f a') => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testPMap1Mix() {
    typeCheckDef("\\function pmap {A : \\Type1} {B : \\Type0} {a a' : A} (f : A -> B) (p : a = a') : (f a = f a') => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testPMap1Error() {
    typeCheckDef("\\function pmap {A B : \\Type0} {a a' : A} (f : A -> B) (p : a = a') : (`= {B} (f a) (f a'))" +
            " => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testTransport1() {
    typeCheckDef("\\function transport {A : \\Type1} (B : A -> \\Type1) {a a' : A} (p : a = a') (b : B a) : B a' =>\n" +
        "coe (\\lam i => B (`@ {\\lam _ => A} {a} {a'} p i)) b right");
  }

  @Test
  public void testAt() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (p : suc = suc) => (p @ left) 0", null);
    assertNotNull(result.expression.getType());
  }

  @Test
  public void universeInference() {
    typeCheckModule(
        "\\function\n" +
        "transport {A : \\Type} (B : A -> \\Type) {a a' : A} (p : a = a') (b : B a)\n" +
        "  => coe (\\lam i => B (p @ i)) b right\n" +
        "\n" +
        "\\function\n" +
        "foo (A : \\1-Type0) (B : A -> \\Type0) (a a' : A) (p : a = a') => transport B p");
  }

  @Test
  public void definitionsWithErrors() {
    resolveNamesModule(
        "\\class C {\n" +
        "  | A : X\n" +
        "  | a : (\\lam (x : Nat) => Nat) A\n" +
        "}", 1);
  }

  @Test
  public void interruptThreadTest() throws InterruptedException {
    Thread thread = new Thread(() -> typeCheckModule(
      "\\function ack (m n : Nat) : Nat => \\elim m, n | zero, n => suc n | suc m, zero => ack m 1 | suc m, suc n => ack m (ack (suc m) n)\n" +
      "\\function t : ack 4 4 = ack 4 4 => path (\\lam _ => ack 4 4)"));
    thread.start();
    thread.interrupt();
    thread.join();
  }

  @Test
  public void parameters() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\function f (x : Nat Nat) (p : `= {Nat} x x) => p", 1);
    assertEquals(FunCall(Prelude.PATH_INFIX, Sort.SET0, Nat(), Ref(def.getParameters()), Ref(def.getParameters())), def.getResultType());
  }

  @Test
  public void constructorExpectedTypeMismatch() {
    typeCheckModule(
        "\\data Foo\n" +
        "\\data Bar Nat \\with | suc n => bar (n = n)\n" +
        "\\function foo : Foo => bar (path (\\lam _ => zero))", 1);
  }

  @Test
  public void postfixTest() {
    typeCheckModule(
      "\\function \\infix 6 # (n : Nat) : Nat\n" +
      "  | zero => zero\n" +
      "  | suc n => suc (suc (n #`))\n" +
      "\\function \\infix 5 $ (n m : Nat) : Nat => \\elim m\n" +
      "  | zero => n\n" +
      "  | suc m => suc (n $ m)\n" +
      "\\function f : (1 $ 1 #`) = 3 => path (\\lam _ => 3)");
  }

  @Test
  public void postfixTest2() {
    typeCheckModule(
      "\\function \\infix 4 d (n : Nat) : Nat\n" +
      "  | zero => zero\n" +
      "  | suc n => suc (suc (n d`))\n" +
      "\\function \\infix 5 $ (n m : Nat) : Nat => \\elim m\n" +
      "  | zero => n\n" +
      "  | suc m => suc (n $ m)\n" +
      "\\function f : (1 $ 1 d`) = 4 => path (\\lam _ => 4)");
  }

  @Test
  public void infixLocal() {
    typeCheckExpr("\\lam (x # : \\Prop) ($ foo %% : \\Prop -> \\Prop -> \\Prop) => (foo (`# $ x) `#) %% x", null);
  }
}
