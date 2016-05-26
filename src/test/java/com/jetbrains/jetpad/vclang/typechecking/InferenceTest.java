package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.GoalError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolverTestCase.resolveNamesClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InferenceTest {
  @Test
  public void doubleGoalTest() {
    String text =
        "\\static \\data B | t | f\n" +
        "\\static \\data D Nat | con Nat\n" +
        "\\static \\function h : D 0 => con (\\case t\n" +
        "  | t => {?}\n" +
        "  | f => {?})";
    Concrete.ClassDefinition classDefinition = parseClass("test", text);
    resolveNamesClass(classDefinition, 0);
    ListErrorReporter errorReporter = new ListErrorReporter();
    TypecheckingOrdering.typecheck(classDefinition, errorReporter);

    assertEquals(errorReporter.getErrorList().toString(), 2, errorReporter.getErrorList().size());
    for (GeneralError error : errorReporter.getErrorList()) {
      assertTrue(error instanceof GoalError);
      assertEquals(0, ((GoalError) error).getContext().size());
    }
  }

  @Test
  public void inferTailConstructor1() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function f : D 0 {1} 2 => con");
  }

  @Test
  public void inferTailConstructor2() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function f : \\Pi {m : Nat} -> D 0 {1} m => con");
  }

  @Test
  public void inferTailConstructor3() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function f : \\Pi {k m : Nat} -> D 0 {k} m => con");
  }

  @Test
  public void inferTailConstructor4() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function f : \\Pi {n k m : Nat} -> D n {k} m => con");
  }

  @Test
  public void inferConstructor1a() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\static \\function f => con {0} (path (\\lam _ => 1))");
  }

  @Test
  public void inferConstructor1b() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = m)\n" +
        "\\static \\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp ,lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f => con {0} idp idp");
  }

  @Test
  public void inferConstructor2a() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\static \\function f => (D 0).con (path (\\lam _ => 1))");
  }

  @Test
  public void inferConstructor2b() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = m)\n" +
        "\\static \\function idp {lp : Lvl} {lh : CNat} {A : \\Type (lp, lh)} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f => (D 0).con idp idp");
  }

  @Test
  public void inferConstructor3() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (n = k) (k = n)\n" +
        "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f => con {0} idp idp", 1);
  }

  @Test
  public void equations() {
    typeCheckClass(
        "\\static \\data E {lp : Lvl} {lh : CNat} (A B : \\Type (lp, lh)) | inl A | inr B\n" +
        "\\static \\data Empty : \\Prop\n" +
        "\\static \\function neg {lp : Lvl} {lh : CNat} (A : \\Type (lp, lh)) => A -> Empty\n" +
        "\\static \\function test {lp : Lvl} {lh : CNat} (A : \\Type (lp, lh)) => E (neg A) A"
    );
  }
}
