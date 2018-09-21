package org.arend.typechecking;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.sort.Sort;
import org.arend.prelude.Prelude;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import static org.arend.ExpressionFactory.FunCall;
import static org.arend.ExpressionFactory.Ref;
import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.arend.typechecking.Matchers.typeMismatchError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TypeCheckingTest extends TypeCheckingTestCase {
  @Test
  public void typeCheckDefinition() {
    typeCheckModule(
        "\\func x : Nat => zero\n" +
        "\\func y : Nat => x");
  }

  @Test
  public void typeCheckDefType() {
    typeCheckModule(
        "\\func x : \\Set0 => Nat\n" +
        "\\func y : x => zero");
  }

  @Test
  public void typeCheckInfixDef() {
    typeCheckModule(
        "\\func \\infixr 9 + : Nat -> Nat -> Nat => \\lam x y => x\n" +
        "\\func * : Nat -> Nat => \\lam x => x + zero");
  }

  @Test
  public void typeCheckConstructor1() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\func f : con {1} {2} {3} = con => idp");
  }

  @Test
  public void typeCheckConstructor2() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\func f : con {0} (path (\\lam _ => 1)) = con {0} idp => idp");
  }

  @Test
  public void testEither() {
    typeCheckModule(
        "\\data Either (A B : \\Type0) | inl A | inr B\n" +
        "\\func fun {A B : \\Type0} (e : Either A B) : \\Set0 \\elim e\n" +
        "  | inl _ => Nat\n" +
        "  | inr _ => Nat\n" +
        "\\func test : fun (inl {Nat} {Nat} 0) => 0");
  }

  @Test
  public void testPMap1() {
    typeCheckDef("\\func pmap {A B : \\Type1} {a a' : A} (f : A -> B) (p : a = a') : (f a = f a') => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testPMap1Mix() {
    typeCheckDef("\\func pmap {A : \\Type1} {B : \\Type0} {a a' : A} (f : A -> B) (p : a = a') : (f a = f a') => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testPMap1Error() {
    typeCheckDef(
      "\\func pmap {A B : \\Type0} {a a' : A} (f : A -> B) (p : a = a') : f a = {B} f a'" +
      " => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testTransport1() {
    typeCheckDef(
      "\\func transport {A : \\Type1} (B : A -> \\Type1) {a a' : A} (p : a = a') (b : B a) : B a' =>\n" +
      "  coe (\\lam i => B (p @ {\\lam _ => A} {a} {a'} i)) b right");
  }

  @Test
  public void testAt() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (p : suc = suc) => (p @ left) 0", null);
    assertNotNull(result.expression.getType());
  }

  @Test
  public void universeInference() {
    typeCheckModule(
        "\\func transport {A : \\Type} (B : A -> \\Type) {a a' : A} (p : a = a') (b : B a)\n" +
        "  => coe (\\lam i => B (p @ i)) b right\n" +
        "\\func foo (A : \\1-Type0) (B : A -> \\Type0) (a a' : A) (p : a = a') => transport B p");
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
      "\\func ack (m n : Nat) : Nat | zero, n => suc n | suc m, zero => ack m 1 | suc m, suc n => ack m (ack (suc m) n)\n" +
      "\\func t : ack 4 4 = ack 4 4 => path (\\lam _ => ack 4 4)"));
    thread.start();
    thread.interrupt();
    thread.join();
  }

  @Test
  public void parameters() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func f (x : Nat Nat) (p : x = {Nat} x) => p", 1);
    assertEquals(FunCall(Prelude.PATH_INFIX, Sort.SET0, Nat(), Ref(def.getParameters()), Ref(def.getParameters())), def.getResultType());
  }

  @Test
  public void constructorExpectedTypeMismatch() {
    typeCheckModule(
        "\\data Foo\n" +
        "\\data Bar Nat \\with | suc n => bar (n = n)\n" +
        "\\func foo : Foo => bar (path (\\lam _ => zero))", 1);
  }

  @Test
  public void postfixTest() {
    typeCheckModule(
      "\\func \\infix 6 # (n : Nat) : Nat\n" +
      "  | zero => zero\n" +
      "  | suc n => suc (suc (n `#))\n" +
      "\\func \\infix 5 $ (n m : Nat) : Nat \\elim m\n" +
      "  | zero => n\n" +
      "  | suc m => suc (n $ m)\n" +
      "\\func f : (1 $ 1 `#) = 3 => path (\\lam _ => 3)");
  }

  @Test
  public void postfixTest2() {
    typeCheckModule(
      "\\func \\infix 4 d (n : Nat) : Nat\n" +
      "  | zero => zero\n" +
      "  | suc n => suc (suc (n `d))\n" +
      "\\func \\infix 5 $ (n m : Nat) : Nat \\elim m\n" +
      "  | zero => n\n" +
      "  | suc m => suc (n $ m)\n" +
      "\\func f : (1 $ 1 `d) = 4 => path (\\lam _ => 4)");
  }

  @Test
  public void infixLocal() {
    typeCheckExpr("\\lam (x # : \\Prop) ($ foo %% : \\Prop -> \\Prop -> \\Prop) => %% (foo ($ # x) #) x", null);
  }

  @Test
  public void elimGoalTest() {
    typeCheckModule(
      "\\func f (n : Nat) : Nat\n" +
      "  | _ => {?}", 1);
  }

  @Test
  public void twoDefinitions() {
    typeCheckModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\func test : Nat => 0", 1);
    assertThatErrorsAre(typeMismatchError());
  }
}
