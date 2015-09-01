package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static org.junit.Assert.*;

public class DefinitionsTest {
  @Test
  public void numberOfFieldsTest() {
    ClassDefinition result = parseDefs("\\static \\class Point { \\function x : Nat \\function y : Nat } \\static \\function C => Point { \\override x => 0 }");
    assertEquals(2, result.getNamespace().getDefinitions().size());
    assertEquals(0, result.getLocalNamespace().getDefinitions().size());
    assertEquals(0, result.getNamespace().getChild(new Utils.Name("Point")).getDefinitions().size());
    assertEquals(2, ((ClassDefinition) result.getNamespace().getDefinition("Point")).getLocalNamespace().getDefinitions().size());
  }

  @Test
  public void numberOfFieldsTest2() {
    ClassDefinition result = parseDefs("\\function f : Nat \\static \\function g => 0 \\class B { \\function h => 0 \\static \\function k => 0 } \\static \\class C { \\function h => 0 \\static \\function k => 0 }");
    assertEquals(2, result.getNamespace().getDefinitions().size());
    assertNotNull(result.getNamespace().getDefinition("g"));
    assertTrue(result.getNamespace().getDefinition("C") instanceof ClassDefinition);
    assertEquals(1, result.getNamespace().getDefinition("C").getNamespace().getDefinitions().size());
    assertEquals(1, ((ClassDefinition) result.getNamespace().getDefinition("C")).getLocalNamespace().getDefinitions().size());
    assertEquals(2, result.getLocalNamespace().getDefinitions().size());
    assertNotNull(result.getLocalNamespace().getDefinition("f"));
    assertTrue(result.getLocalNamespace().getDefinition("B") instanceof ClassDefinition);
    assertEquals(1, result.getLocalNamespace().getDefinition("B").getNamespace().getDefinitions().size());
    assertEquals(1, ((ClassDefinition) result.getLocalNamespace().getDefinition("B")).getLocalNamespace().getDefinitions().size());
  }

  @Test
  public void openTest() {
    parseDefs("\\static \\class A { \\static \\function x => 0 } \\open A \\static \\function y => x");
  }

  @Test
  public void closeTestError() {
    parseDefs("\\static \\class A { \\static \\function x => 0 } \\open A \\static \\function y => x \\close A(x) \\function z => x", 1);
  }

  @Test
  public void exportTest() {
    parseDefs("\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\export B } \\static \\function y => A.x");
  }

  @Test
  public void staticFieldAccCallTest() {
    parseDefs("\\static \\class A { \\function x : Nat \\class B { \\static \\function y => x } } \\static \\function f (a : A) => a.B.y");
  }

  @Test
  public void exportPublicFieldsTest() {
    ClassDefinition result = parseDefs("\\static \\class A { \\function x : Nat \\class B { \\static \\function y => x } \\export B } \\static \\function f (a : A) => a.y");
    assertEquals(2, result.getFields().size());
    assertTrue(result.getField("A") instanceof ClassDefinition);
    assertEquals(3, ((ClassDefinition) result.getField("A")).getFields().size());
    assertTrue(result.getField("A").getNamespace().getChildren().isEmpty());
  }

  @Test
  public void nonStaticClassExportTestError() {
    parseDefs("\\class A { } \\static \\class B { \\export A }", 1);
  }

  @Test
  public void exportTest2() {
    ClassDefinition result = parseDefs(
        "\\function (+) (x y : Nat) : Nat\n" +
            "\\class A {\n" +
            "\\function x : Nat\n" +
            "\\class B {\n" +
            "\\function y : Nat\n" +
            "\\class C {\n" +
            "\\static \\function z => x + y\n" +
            "\\static \\function w => x\n" +
            "}\n" +
            "\\export C\n" +
            "}\n" +
            "\\class D { \\export B }\n" +
            "\\function f (b : B) : b.C.z = x + b.y => path (\\lam _ => x + b.y)\n" +
            "}");
    assertEquals(2, result.getFields().size());
    assertTrue(result.getField("A") instanceof ClassDefinition);
    ClassDefinition classA = (ClassDefinition) result.getField("A");
    assertEquals(4, classA.getFields().size());
    assertTrue(classA.getNamespace().getChildren().isEmpty());
    assertTrue(classA.getField("B") instanceof ClassDefinition);
    ClassDefinition classB = (ClassDefinition) classA.getField("B");
    assertEquals(4, classB.getFields().size());
    assertEquals(1, classB.getNamespace().getChildren().size());
    assertTrue(classB.getField("C") instanceof ClassDefinition);
    ClassDefinition classC = (ClassDefinition) classB.getField("C");
    assertEquals(2, classC.getFields().size());
    assertEquals(2, classC.getNamespace().getChildren().size());
    assertEquals(classC.getField("w"), classB.getNamespace().getDefinition("w"));
    assertTrue(classA.getField("D") instanceof ClassDefinition);
    ClassDefinition classD = (ClassDefinition) classA.getField("D");
    assertEquals(1, classD.getFields().size());
    assertEquals(1, classD.getNamespace().getChildren().size());
    assertEquals(classC.getField("w"), classD.getNamespace().getDefinition("w"));
    assertEquals(classC.getField("w"), classD.getField("w"));
  }

  @Test
  public void neverCloseField() {
    parseDefs("\\static \\class A { \\static \\function x => 0 } \\static \\class B { \\open A \\export A \\close A } \\static \\class C { \\static \\function y => B.x }");
  }

  @Test
  public void exportExistingTestError() {
    parseDefs("\\static \\class A { \\static \\class B { \\static \\function x => 0 } } \\export A \\static \\class B { \\static \\function y => 0 }", 1);
  }

  @Test
  public void exportExistingTestError2() {
    parseDefs("\\static \\class B { \\static \\function y => 0 } \\static \\class A { \\static \\class B { \\static \\function x => 0 } } \\export A", 1);
  }

  @Test
  public void openExportTestError() {
    parseDefs("\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\open B } \\static \\function y => A.x", 1);
  }

  @Test
  public void export2TestError() {
    parseDefs("\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\export B } \\static \\function y => x", 1);
  }

  @Test
  public void openAbstractTestError() {
    parseDefs("\\static \\class A { \\function x : Nat } \\open A \\function y => x", 1);
  }

  @Test
  public void openAbstractTestError2() {
    parseDefs("\\static \\class A { \\function x : Nat \\function y => x } \\open A \\function z => y", 1);
  }

  @Test
  public void staticInOnlyStaticTestError() {
    parseDefs("\\function B : \\Type0 \\static \\class A {} \\static \\class A { \\static \\function s => 0 \\static \\data D (A : Nat) | foo Nat | bar }", 1);
  }

  @Test
  public void classExtensionWhereTestError() {
    parseDefs("\\static \\function f => 0 \\where \\static \\class A {} \\static \\class A { \\function x => 0 }", 1);
  }

  @Test
  public void multipleDefsWhere() {
    parseDefs("\\static \\function f => 0 \\where \\static \\function d => 0 \\static \\function d => 1", 1);
  }

  @Test
  public void overrideWhere() {
    parseDefs("\\static \\class A { \\function x => 0 } \\static \\function C => A { \\override x => y \\where \\function y => 0 }", 1);
  }
}
