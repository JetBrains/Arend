package org.arend.typechecking;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.term.prettyprint.ToAbstractVisitor;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;

import static org.arend.ExpressionFactory.FunCall;
import static org.arend.ExpressionFactory.Ref;
import static org.arend.Matchers.error;
import static org.arend.Matchers.typeMismatchError;
import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.*;

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
        "\\func f : con {1} {2} {3} = con => idp");
  }

  @Test
  public void typeCheckConstructor2() {
    typeCheckModule(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\func f : con {0} (path (\\lam _ => 1)) = con {0} idp => idp");
  }

  @Test
  public void testEither() {
    typeCheckModule("""
      \\data Either (A B : \\Type0) | inl A | inr B
      \\func fun {A B : \\Type0} (e : Either A B) : \\Set0 \\elim e
        | inl _ => Nat
        | inr _ => Nat
      \\func test : fun (inl {Nat} {Nat} 0) => 0
      """);
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
    TypecheckingResult result = typeCheckExpr("\\lam (p : suc = suc) => (p @ left) 0", null);
    assertNotNull(result.expression.getType());
  }

  @Test
  public void universeInference() {
    typeCheckModule("""
      \\func transport {A : \\Type} (B : A -> \\Type) {a a' : A} (p : a = a') (b : B a)
        => coe (\\lam i => B (p @ i)) b right
      \\func foo (A : \\1-Type0) (B : A -> \\Type0) (a a' : A) (p : a = a') => transport B p
      """);
  }

  @Test
  public void definitionsWithErrors() {
    resolveNamesModule("""
      \\class C {
        | A : X
        | a : (\\lam (x : Nat) => Nat) A
      }
      """, 1);
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
    assertEquals(FunCall(Prelude.PATH_INFIX, LevelPair.SET0, Nat(), Ref(def.getParameters()), Ref(def.getParameters())), def.getResultType());
  }

  @Test
  public void constructorExpectedTypeMismatch() {
    typeCheckModule("""
      \\data Foo
      \\data Bar Nat \\with | suc n => bar (n = n)
      \\func foo : Foo => bar (path (\\lam _ => zero))
      """, 1);
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

  @Test
  public void functionImplicitPi() {
    typeCheckModule(
      "\\func ff {A : \\Type} : \\Pi {B : \\Type} -> A -> B -> A => \\lam a _ => a\n" +
      "\\func g {A : \\Type} (a : A) => ff a a");
  }

  @Test
  public void fieldImplicitPi() {
    typeCheckModule(
      "\\class C | ff : \\Pi {A : \\Type} -> A -> A\n" +
      "\\func g {c : C} {A : \\Type} (a : A) => ff a");
  }

  @Test
  public void duplicateFieldName() {
    resolveNamesModule("""
      \\class A {
        | x : Nat
        | x : Nat
      }
      """, 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void isoSet() {
    typeCheckModule("\\func setExt (A B : \\Set) (f : A -> B) (g : B -> A) (p : \\Pi (x : A) -> g (f x) = x) (q : \\Pi (y : B) -> f (g y) = y) => path {\\lam _ => \\Set} (iso f g p q)");
    assertEquals(new UniverseExpression(Sort.SetOfLevel(new Level(LevelVariable.PVAR))), ((FunctionDefinition) getDefinition("setExt")).getResultType().normalize(NormalizationMode.WHNF).cast(DataCallExpression.class).getDefCallArguments().get(0).cast(LamExpression.class).getBody());
  }

  @Test
  public void isoSet2() {
    typeCheckModule("\\func setExt (A B : \\Set) (f : A -> B) (g : B -> A) (p : \\Pi (x : A) -> g (f x) = x) (q : \\Pi (y : B) -> f (g y) = y) : A = {\\Set} B => path (iso f g p q)");
    assertEquals(new UniverseExpression(Sort.SetOfLevel(new Level(LevelVariable.PVAR))), ((FunctionDefinition) getDefinition("setExt")).getResultType().cast(FunCallExpression.class).getDefCallArguments().get(0));
  }

  @Test
  public void isoSetError() {
    typeCheckModule("\\func setExt (A B : \\1-Type0) (f : A -> B) (g : B -> A) (p : \\Pi (x : A) -> g (f x) = x) (q : \\Pi (y : B) -> f (g y) = y) : A = {\\Set0} B => path (iso f g p q)", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void isoPropError() {
    typeCheckModule("\\func setExt (A B : \\Set0) (f : A -> B) (g : B -> A) (p : \\Pi (x : A) -> g (f x) = x) (q : \\Pi (y : B) -> f (g y) = y) : A = {\\Prop} B => path (iso f g p q)", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void piParametersTest() {
    typeCheckModule(
      "\\func pair : \\Sigma (Nat -> Nat) Nat => (\\lam x => x, 1)\n" +
      "\\func test : \\Pi {n : Nat} -> Nat => pair.1", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void longReference() {
    typeCheckModule("\\record Cl | x : Nat\n" +
            "\\func test : Nat => \\let ccl : Cl => \\new Cl { | x => 1 } \\in ccl.x");
    LetExpression body = (LetExpression) Objects.requireNonNull(((FunctionDefinition) getDefinition("test")).getBody());
    var let = body.getExpression();
    var concrete = ToAbstractVisitor.convert(let, PrettyPrinterConfig.DEFAULT);
    typeCheckExpr(Map.of(Objects.requireNonNull(((Concrete.LongReferenceExpression) concrete).getQualifier()).getReferent(), new TypedBinding("ccl", body.getClauses().get(0).getTypeExpr())), concrete, let.getType(), 0);
  }

  @Test
  public void longReferenceClass() {
    typeCheckModule("\\class Cl | x : Nat\n" +
            "\\func test : Nat => \\let ccl : Cl => \\new Cl { | x => 1 } \\in ccl.x");
    LetExpression body = (LetExpression) Objects.requireNonNull(((FunctionDefinition) getDefinition("test")).getBody());
    var let = body.getExpression();
    var concrete = ToAbstractVisitor.convert(let, PrettyPrinterConfig.DEFAULT);
    typeCheckExpr(Map.of(Objects.requireNonNull(((Concrete.LongReferenceExpression) concrete).getQualifier()).getReferent(), new TypedBinding("ccl", body.getClauses().get(0).getTypeExpr())), concrete, let.getType(), 0);
  }

  @Test
  public void testGoalWithArgsUnderElim() {
    typeCheckModule("""
      \\data sample
        | cons (1 = 1)
      \\func insert-comm (x : sample) : Nat \\elim x
        | cons e => {?} e
      """, 1);
    GoalErrorExpression goal = Objects.requireNonNull(((ElimBody)
            Objects.requireNonNull(
                    Objects.requireNonNull((
                                    (FunctionDefinition) getDefinition("insert-comm")))
                            .getBody()))
            .getClauses().get(0).getExpression()).cast(AppExpression.class).getFunction().cast(GoalErrorExpression.class);
    Assert.assertNotNull(goal.getType());
  }

  @Test
  public void testGoalWithArgsUnderElimAndClass() {
    typeCheckModule("""
      \\class StrictPoset (E : \\Set) {
        | \\infix 4 < : E -> E -> \\Prop
      }
      \\data Tri' {A : StrictPoset} (a a' : A)
        | less (a < a')
      \\func insert-comm {A : StrictPoset} (a a' : A) (x : Tri' a a')
        : Nat \\elim x | less a<a' => {?} a<a'
      """, 1);
    GoalErrorExpression goal = Objects.requireNonNull(((ElimBody)
            Objects.requireNonNull(
                    Objects.requireNonNull((
                                    (FunctionDefinition) getDefinition("insert-comm")))
                            .getBody()))
            .getClauses().get(0).getExpression()).cast(AppExpression.class).getFunction().cast(GoalErrorExpression.class);
    Assert.assertNotNull(goal.getType());
  }

  @Test
  public void piGoalTest() {
    typeCheckModule(
      "\\func foo {A B : \\Type} (f : A -> B) => f\n" +
      "\\func test : Nat -> \\Pi (n : Nat) -> n = n => foo (\\lam x y => {?})", 1);
    GeneralError error = errorList.get(0);
    assertTrue(error instanceof GoalError);
    assertNotNull(((GoalError) error).expectedType);
    assertFalse(((GoalError) error).expectedType.getUnderlyingExpression() instanceof InferenceReferenceExpression);
  }
}
