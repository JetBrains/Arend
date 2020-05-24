package org.arend.typechecking.patternmatching;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.elimtree.ElimBody;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.HigherConstructorMatchingError;
import org.junit.Test;

import java.util.Collections;

import static org.arend.Matchers.*;
import static org.arend.core.expr.ExpressionFactory.*;

public class ConditionsTest extends TypeCheckingTestCase {
  @Test
  public void dataTypeWithConditions() {
    typeCheckModule(
        "\\data Z | zneg Nat | zpos Nat { zero => zneg zero }");
  }

  @Test
  public void dataTypeWithConditionsWrongType() {
    typeCheckModule(
        "\\data Z | zneg Nat | zpos Nat { zero => zero }", 1);
  }

  @Test
  public void dataTypeWithConditionsTCFailed1() {
    typeCheckModule(
        "\\data Z | zneg Nat | zpos Nat { zero => zpos 1 }", 1);
  }

  @Test
  public void dataTypeWithConditionsTCFailed2() {
    typeCheckModule(
        "\\data Unit | unit\n" +
        "\\data Z | zneg | zpos Unit { _ => zpos }", 1);
  }

  @Test
  public void dataTypeWithConditionsMutualDep() {
    typeCheckModule(
        "\\data Unit | unit\n" +
        "\\data Z | zpos Unit { _ => zneg unit } | zneg Unit { _ => zpos unit }", 1);
  }

  @Test
  public void simpleTest() {
    typeCheckModule(
        "\\data Z | zpos Nat | zneg Nat { zero => zpos zero }\n" +
        "\\func test (x : Z) : Nat\n" +
        "  | zneg (suc (suc _)) => 0\n" +
        "  | zneg (suc zero) => 1\n" +
        "  | zneg zero => 2\n" +
        "  | zpos x => suc (suc x)");
  }

  @Test
  public void simpleTestError() {
    typeCheckModule(
        "\\data Z | zpos Nat | zneg Nat { zero => zpos zero }\n" +
        "\\func test (x : Z) : Nat\n" +
        "  | zneg (suc (suc _)) => 0\n" +
        "  | zneg (suc zero) => 1\n" +
        "  | zneg zero => 2\n" +
        "  | zpos x => suc x", 1);
  }

  @Test
  public void multipleArgTest() {
    typeCheckModule(
        "\\data Z | negative Nat | positive Nat { zero => negative zero }\n" +
        "\\func test (x : Z) (y : Nat) : Nat\n" +
        "  | positive (suc n), m => n\n" +
        "  | positive zero, m => m\n" +
        "  | negative n, zero => zero\n" +
        "  | negative n, suc m => suc m", 1);
  }

  @Test
  public void multipleArgTestError() {
    typeCheckModule(
        "\\data Z | negative Nat | positive Nat { zero => negative zero }\n" +
        "\\func test (x : Z) (y : Nat) : Nat\n" +
        "  | positive (suc n), m => n\n" +
        "  | positive zero, m => m\n" +
        "  | negative n, zero => zero\n" +
        "  | negative n, suc m => suc (suc m)", 1);
  }

  @Test
  public void bidirectionalList() {
    typeCheckModule(
        "\\data BD-list (A : \\Type0) | nil | cons A (BD-list A) | snoc (xs : BD-list A) (y : A) \\elim xs\n" +
        "  { | cons x xs => cons x (snoc xs y) | nil => cons y nil }\n" +
        "\\func length {A : \\Type0} (x : BD-list A) : Nat \\elim x\n" +
        "  | nil => 0\n" +
        "  | cons x xs => suc (length xs)\n" +
        "  | snoc xs x => suc (length xs)", 1);
  }

  @Test
  public void dataTypeWithIndices() {
    typeCheckModule(
        "\\data S | base | loop I \n" +
        "  { left => base\n" +
        "  | right => base\n" +
        "  }\n" +
        "\\data D Nat \\with | _ => d | zero => di I\n" +
        "  { | left => d | right => d }\n" +
        "\\func test (x : Nat) (y : D x) : S\n" +
        "  | suc _, d => base\n" +
        "  | zero, d => base\n" +
        "  | zero, di i => loop i");
  }

  @Test
  public void testSelfConditionsError() {
    typeCheckModule(
      "\\data Unit | unit\n" +
      "\\data D\n" +
      "  | nil0\n" +
      "  | nil1 Unit { _ => nil0 }\n" +
      "  | cons1 D\n" +
      "  | cons2 D\n" +
      "  | cons0 D { | nil0 => cons1 nil0 | nil1 x => cons2 (nil1 x) }", 1);
  }

  @Test
  public void testSelfConditions() {
    typeCheckModule(
      "\\data Unit | unit\n" +
      "\\data D\n" +
      "  | nil0\n" +
      "  | nil1 Unit { _ => nil0 }\n" +
      "  | cons1 D\n" +
      "  | cons2 D { x => cons1 x }\n" +
      "  | cons0 D { | nil0 => cons1 nil0 | nil1 x => cons2 (nil1 x) }");
  }

  @Test
  public void nestedCheck() {
    typeCheckModule(
      "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
      "\\func test (x y z : Z) : Nat\n" +
      "  | pos zero, pos zero, neg zero => 0\n" +
      "  | _, _, _ => 1", -1);
  }

  @Test
  public void nonStatic() {
    typeCheckClass(
      "| S' : \\Type0\n" +
      "| base' : S'\n" +
      "| loop' : I -> S'\n" +
      "\\data S | base | loop I\n" +
      "  { left => base\n" +
      "  | right => base\n" +
      "  }\n" +
      "\\func test (s : S) : S'\n" +
      "  | base => base'\n" +
      "  | loop i => loop' i", "", 2);
  }

  @Test
  public void constructorArgumentWithCondition() {
    typeCheckModule(
        "\\data S | base | loop Nat { zero => base }\n" +
        "\\data D | cons' | cons S { loop zero => cons' }", 1);
  }

  @Test
  public void cc() {
    typeCheckModule(
      "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
      "\\func test (z : Z) : Nat\n" +
      "  | pos n => 0\n" +
      "  | neg (suc n) => 1");
  }

  @Test
  public void ccOtherDirectionError() {
    typeCheckModule(
      "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
      "\\func test (z : Z) : Nat\n" +
      "  | pos (suc n) => 0\n" +
      "  | neg n => 1", 2);
  }

  @Test
  public void ccComplexBranch() {
    typeCheckModule(
      "\\data D | snd | fst Nat { | zero => snd | suc _ => snd }\n" +
      "\\func test (d : D) : Nat\n" +
      "  | snd => zero", 1);
  }

  @Test
  public void whatIfNormalizeError() {
    typeCheckModule(
        "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
        "\\func test (x : Z) : Nat\n" +
        " | neg x => 1\n" +
        " | pos x => 2", 1);
  }

  @Test
  public void whatIfDontNormalizeConditionRHS() {
    typeCheckModule(
        "\\data Unit | unit\n" +
        "\\data D | d2 | d1 Unit { _ => d2 }\n"+
        "\\data E | e1 D | e2 D { _ => e1 (d1 unit) }\n"+
        "\\func test (e : E) : Nat\n" +
        "  | e2 d2 => 1\n" +
        "  | e1 (d1 _) => 2\n" +
        "  | e1 d2 => 1", 1);
  }

  @Test
  public void dataIntervalCondition() {
    typeCheckModule("\\data D I \\with | left => c", 1);
  }

  @Test
  public void dataCondition() {
    typeCheckModule(
      "\\data D | c | c' | l I\n" +
      "  { left => c\n" +
      "  | right => c'\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | c => e");
  }

  @Test
  public void dataCondition2() {
    typeCheckModule(
      "\\data D | c | l Nat\n" +
      "  { 3 => c\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | l 2 => e\n" +
      "\\data E' D \\with\n" +
      " | l (suc (suc (suc (suc x)))) => e'");
  }

  @Test
  public void dataConditionError() {
    typeCheckModule(
      "\\data D | c | c' | l I\n" +
      "  { left => c\n" +
      "  | right => c'\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | l i => e", 1);
  }

  @Test
  public void dataConditionError2() {
    typeCheckModule(
      "\\data D | c | l Nat\n" +
      "  { 3 => c\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | l (suc (suc (suc x))) => e", 1);
  }

  @Test
  public void dataConditionError3() {
    typeCheckModule(
      "\\data D | c | l Nat\n" +
      "  { suc x => c\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | l (suc (suc x)) => e", 1);
  }

  @Test
  public void dataConditionEmptyPatternError() {
    typeCheckModule(
      "\\data D | c | c' | l I\n" +
      "  { left => c\n" +
      "  | right => c'\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | () => e", 1);
  }

  @Test
  public void partialIntervalCondition() {
    typeCheckModule("\\data D | con1 | con2 I { | left => con1 }");
  }

  @Test
  public void partialIntervalConditionError() {
    typeCheckModule("\\data D | con1 | con2 I { | left => con2 right }", 1);
  }

  @Test
  public void goalTest() {
    typeCheckModule(
      "\\data II | point1 | point2 | seg (i : I) \\with { | left => point1 | right => point2 }\n" +
      "\\func f (x : II) : Nat\n" +
      "  | point2 => 7\n" +
      "  | point1 => 3\n" +
      "  | seg i => {?}", 1);
    DependentLink binding = ((ElimBody) ((FunctionDefinition) getDefinition("f")).getBody()).getClauses().get(2).getPatterns().get(0).getFirstBinding();
    assertThatErrorsAre(goalError(
      new Condition(null, new ExprSubstitution(binding, Left()), new SmallIntegerExpression(3)),
      new Condition(null, new ExprSubstitution(binding, Right()), new SmallIntegerExpression(7))));
  }

  @Test
  public void goalCaseTest() {
    typeCheckModule(
      "\\data II | point1 | point2 | seg (i : I) \\with { | left => point1 | right => point2 }\n" +
      "\\func f (x : II) : Nat => \\case x \\with {\n" +
      "  | point2 => 7\n" +
      "  | point1 => 3\n" +
      "  | seg i => {?}\n" +
      "}", 1);
    DependentLink binding = ((CaseExpression) ((FunctionDefinition) getDefinition("f")).getBody()).getElimBody().getClauses().get(2).getPatterns().get(0).getFirstBinding();
    assertThatErrorsAre(goalError(
      new Condition(null, new ExprSubstitution(binding, Left()), new SmallIntegerExpression(3)),
      new Condition(null, new ExprSubstitution(binding, Right()), new SmallIntegerExpression(7))));
  }

  @Test
  public void goalTest2() {
    typeCheckModule(
      "\\data S1 | base | loop (i : I) \\with { | left => base | right => base }\n" +
      "\\func f (x y : S1) : S1\n" +
      "  | base, y => y\n" +
      "  | loop i, base => loop i\n" +
      "  | loop i, loop j => {?}", 1);

    DependentLink i = ((ElimBody) ((FunctionDefinition) getDefinition("f")).getBody()).getClauses().get(2).getPatterns().get(0).getFirstBinding();
    DependentLink j = i.getNext();
    Constructor loop = (Constructor) getDefinition("S1.loop");
    Expression iResult = ConCallExpression.make(loop, Sort.STD, Collections.emptyList(), Collections.singletonList(new ReferenceExpression(j)));
    Expression jResult = ConCallExpression.make(loop, Sort.STD, Collections.emptyList(), Collections.singletonList(new ReferenceExpression(i)));

    assertThatErrorsAre(goalError(
      new Condition(null, new ExprSubstitution(i, Left()), iResult), new Condition(null, new ExprSubstitution(i, Right()), iResult),
      new Condition(null, new ExprSubstitution(j, Left()), jResult), new Condition(null, new ExprSubstitution(j, Right()), jResult)));
  }

  @Test
  public void goalCaseTest2() {
    typeCheckModule(
      "\\data S1 | base | loop (i : I) \\with { | left => base | right => base }\n" +
      "\\func f (x y : S1) : S1 => \\case x, y \\with {\n" +
      "  | base, y => y\n" +
      "  | loop i, base => loop i\n" +
      "  | loop i, loop j => {?}\n" +
      "}", 1);

    DependentLink i = ((CaseExpression) ((FunctionDefinition) getDefinition("f")).getBody()).getElimBody().getClauses().get(2).getPatterns().get(0).getFirstBinding();
    DependentLink j = i.getNext();
    Constructor loop = (Constructor) getDefinition("S1.loop");
    Expression iResult = ConCallExpression.make(loop, Sort.STD, Collections.emptyList(), Collections.singletonList(new ReferenceExpression(j)));
    Expression jResult = ConCallExpression.make(loop, Sort.STD, Collections.emptyList(), Collections.singletonList(new ReferenceExpression(i)));

    assertThatErrorsAre(goalError(
      new Condition(null, new ExprSubstitution(i, Left()), iResult), new Condition(null, new ExprSubstitution(i, Right()), iResult),
      new Condition(null, new ExprSubstitution(j, Left()), jResult), new Condition(null, new ExprSubstitution(j, Right()), jResult)));
  }

  @Test
  public void goalTest3() {
    typeCheckModule(
      "\\func f (x : Int) : Nat\n" +
      "  | pos n => suc (suc n)\n" +
      "  | neg n => {?}", 1);
    DependentLink binding = ((ElimBody) ((FunctionDefinition) getDefinition("f")).getBody()).getClauses().get(1).getPatterns().get(0).getFirstBinding();
    assertThatErrorsAre(goalError(new Condition(null, new ExprSubstitution(binding, Zero()), new SmallIntegerExpression(2))));
  }

  @Test
  public void goalTest4() {
    typeCheckModule(
      "\\func f (x : Int) : Nat\n" +
      "  | pos n => n\n" +
      "  | neg n => {?(suc n)}", 1);
    DependentLink binding = ((ElimBody) ((FunctionDefinition) getDefinition("f")).getBody()).getClauses().get(1).getPatterns().get(0).getFirstBinding();
    assertThatErrorsAre(goalError(new Condition(null, new ExprSubstitution(binding, Zero()), new SmallIntegerExpression(0))));
  }

  @Test
  public void goalPathConditionsTest() {
    typeCheckModule(
      "\\data S1 | base | loop (i : I) \\with { | left => base | right => base }\n" +
      "\\func f (x : S1) : base = x => path (\\lam i => {?})", 1);

    FunctionDefinition f = (FunctionDefinition) getDefinition("f");
    DependentLink binding = ((LamExpression) ((ConCallExpression) f.getBody()).getDefCallArguments().get(0)).getParameters();
    Constructor base = (Constructor) getDefinition("S1.base");
    assertThatErrorsAre(goalError(
      new Condition(null, new ExprSubstitution(binding, Left()), ConCallExpression.make(base, Sort.STD, Collections.emptyList(), Collections.emptyList())),
      new Condition(null, new ExprSubstitution(binding, Right()), new ReferenceExpression(f.getParameters()))));
  }

  @Test
  public void varPattern() {
    typeCheckModule(
      "\\data D | con1 | con2 | con3 (i : I) \\with { | left => con1 | right => con2 }\n" +
      "\\func f (d : D) : Nat\n" +
      "  | con1 => 0\n" +
      "  | con2 => 1\n" +
      "  | _ => 2", 1);
    assertThatErrorsAre(typecheckingError(HigherConstructorMatchingError.class));
  }

  @Test
  public void varPattern2() {
    typeCheckModule(
      "\\data S1 | base | base2 | loop I \\with { | left => base | right => base }\n" +
      "\\func f (x y : S1) : S1\n" +
      "  | base, y => y\n" +
      "  | base2, y => y\n" +
      "  | x, base => x\n" +
      "  | x, base2 => x\n" +
      "  | loop i, loop j => {?}", 2);
    assertThatErrorsAre(goal(2), typecheckingError(HigherConstructorMatchingError.class));
  }

  @Test
  public void constructorsOnlyOnTopLevel() {
    typeCheckModule(
      "\\func \\infixr 5 *> {A : \\Type} {a a' a'' : A} (p : a = a') (q : a' = a'')\n" +
      "  => coe (\\lam i => a = q @ i) p right\n" +
      "\\data D\n" +
      "  | base\n" +
      "  | loop I \\with { | left => base | right => base }\n" +
      "  | loop2 (i j : I) \\elim i { | left => base | right => (path loop *> path loop) @ j }", 1);
  }
}
