package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.GoalError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseClass;
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
    RootModule.ROOT.addAbstractDefinition(classDefinition);
    ListErrorReporter errorReporter = new ListErrorReporter();
    TypecheckingOrdering.typecheck(new ResolvedName(RootModule.ROOT, classDefinition.getName()), errorReporter);

    assertEquals(errorReporter.getErrorList().toString(), 2, errorReporter.getErrorList().size());
    for (GeneralError error : errorReporter.getErrorList()) {
      assertTrue(error instanceof GoalError);
      assertEquals(0, ((GoalError) error).getContext().size());
    }
  }
}
