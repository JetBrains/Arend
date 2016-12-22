package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NameResolverTest extends NameResolverTestCase {
  @Test
  public void parserInfix() {
    Abstract.Definition plus = new Concrete.FunctionDefinition(POSITION, "+", new Abstract.Precedence(Abstract.Precedence.Associativity.LEFT_ASSOC, (byte) 6), Collections.<Concrete.Argument>emptyList(), null, Abstract.Definition.Arrow.RIGHT, null, Collections.<Concrete.Statement>emptyList());
    Abstract.Definition mul = new Concrete.FunctionDefinition(POSITION, "*", new Abstract.Precedence(Abstract.Precedence.Associativity.LEFT_ASSOC, (byte) 7), Collections.<Concrete.Argument>emptyList(), null, Abstract.Definition.Arrow.RIGHT, null, Collections.<Concrete.Statement>emptyList());

    SimpleNamespace namespace = new SimpleNamespace();
    namespace.addDefinition(plus);
    namespace.addDefinition(mul);

    Concrete.Expression result = resolveNamesExpr(namespace, "0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)");
    assertNotNull(result);
    assertTrue(compareAbstract(cBinOp(cBinOp(cNum(0), plus, cBinOp(cNum(1), mul, cNum(2))), plus, cBinOp(cBinOp(cNum(3), mul, cBinOp(cNum(4), mul, cNum(5))), mul, cBinOp(cNum(6), plus, cNum(7)))), result));
  }

  @Test
  public void parserInfixError() {
    Abstract.Definition plus = new Concrete.FunctionDefinition(POSITION, "+", new Abstract.Precedence(Abstract.Precedence.Associativity.LEFT_ASSOC, (byte) 6), Collections.<Concrete.Argument>emptyList(), null, Abstract.Definition.Arrow.RIGHT, null, Collections.<Concrete.Statement>emptyList());
    Abstract.Definition mul = new Concrete.FunctionDefinition(POSITION, "*", new Abstract.Precedence(Abstract.Precedence.Associativity.RIGHT_ASSOC, (byte) 6), Collections.<Concrete.Argument>emptyList(), null, Abstract.Definition.Arrow.RIGHT, null, Collections.<Concrete.Statement>emptyList());

    SimpleNamespace namespace = new SimpleNamespace();
    namespace.addDefinition(plus);
    namespace.addDefinition(mul);

    resolveNamesExpr(namespace, "11 + 2 * 3", 1);
  }

  @Test
  public void whereTest() {
    resolveNamesClass(
        "\\function f (x : \\Type0) => B.b (a x) \\where {\n" +
            "  \\function a (x : \\Type0) => x\n" +
            "  \\data D | D1 | D2\n" +
            "  \\class B \\where { \\data C | cr \\function b (x : \\Type0) => D1 }\n" +
            "}");
  }

  @Test
  public void whereTestDefCmd() {
    resolveNamesClass(
        "\\function f (x : \\Type0) => a \\where {\n" +
        "  \\class A \\where { \\function a => 0 }\n" +
        "  \\open A\n" +
        "}");
  }

  @Test
  public void whereOpenFunction() {
    resolveNamesClass(
        "\\function f => x \\where {\n" +
        "  \\function b => 0 \\where\n" +
        "    \\function x => 0\n" +
        "  \\open b(x)\n" +
        "}");
  }

  @Test
  public void whereNested() {
    resolveNamesClass(
        "\\function f => x \\where {\n" +
        "  \\data B | b\n" +
        "  \\function x => a \\where\n" +
        "    \\function a => b\n" +
        "}");
  }

  @Test
  public void whereOuterScope() {
    resolveNamesClass(
        "\\function f => 0 \\where {\n" +
        "  \\function g => 0\n" +
        "  \\function h => g\n" +
        "}");
  }

  @Test
  public void whereInSignature() {
    resolveNamesClass(
        "\\function f : D => d \\where\n" +
        "  \\data D | d");
  }

  @Test
  public void whereAccessOuter() {
    resolveNamesClass(
        "\\function f => 0 \\where\n" +
        "  \\function x => 0\n" +
        "\\function g => f.x");
  }

  @Test
  public void whereNonStaticOpen() {
    resolveNamesClass(
        "\\function f => 0 \\where {\n" +
        "  \\function x => 0\n" +
        "  \\function y => x\n" +
        "}\n" +
        "\\function g => 0 \\where\n" +
        "  \\open f(y)");
  }

  @Test
  public void openTest() {
    resolveNamesClass("\\class A \\where { \\function x => 0 } \\open A \\function y => x");
  }

  @Ignore
  @Test
  public void exportTest() {
    resolveNamesClass("\\class A \\where { \\class B \\where { \\function x => 0 } \\export B } \\function y => A.x");
  }

  @Test
  public void staticFieldAccCallTest() {
    resolveNamesClass("\\class A { \\field x : \\Type0 \\class B \\where { \\function y => x } } \\function f (a : A) => a.B.y");
  }

  @Ignore
  @Test
  public void exportPublicFieldsTest() {
    /*
    resolveNamesClass("test", "\\class A \\where { \\function x => 0 \\class B \\where { \\function y => x } \\export B } \\function f => A.y");
    assertNotNull(Root.getModule(new NameModuleSourceId("test")));
    Root.getModule(new NameModuleSourceId("test")).namespace;

    assertEquals(2, staticNamespace.getMembers().size());
    assertNotNull(staticNamespace.getMember("A"));
    assertEquals(3, staticNamespace.getMember("A").namespace.getMembers().size());
    assertEquals(3, ((Abstract.ClassDefinition) staticNamespace.getMember("A").abstractDefinition).getStatements().size());
    */
    assertTrue(false);
  }

  @Ignore
  @Test
  public void exportTest2() {
    /*
    resolveNamesDef(
        "\\class Test {\n" +
        "  \\field (+) (x y : \\Type0) : \\Type0\n" +
        "  \\class A {\n" +
        "    \\field x : \\Type0\n" +
        "    \\class B {\n" +
        "      \\field y : \\Type0\n" +
        "      \\class C \\where {\n" +
        "        \\function z => x + y\n" +
        "        \\function w => x\n" +
        "      }\n" +
        "      \\export C\n" +
        "    }\n" +
        "    \\class D { \\export B }\n" +
        "    \\function f (b : B) : b.C.z = b.z => path (\\lam _ => b.w + b.y)\n" +
        "  }\n" +
        "}");
    assertNotNull(Root.getModule(new NameModuleSourceId("test")));
    Namespace namespace = Root.getModule(new NameModuleSourceId("test")).namespace;

    assertEquals(namespace.getMembers().toString(), 2, namespace.getMembers().size());
    assertNotNull(namespace.getMember("A"));
    Abstract.ClassDefinition classA = (Abstract.ClassDefinition) namespace.getMember("A").abstractDefinition;
    assertEquals(classA.getStatements().toString(), 4, classA.getStatements().size());
    assertEquals(namespace.getMember("A").namespace.getMembers().toString(), 4, namespace.getMember("A").namespace.getMembers().size());
    Abstract.ClassDefinition classB = (Abstract.ClassDefinition) getField(classA, "B");
    assertNotNull(classB);
    assertEquals(classB.getStatements().toString(), 3, classB.getStatements().size());
    Abstract.ClassDefinition classC = (Abstract.ClassDefinition) getField(classB, "C");
    assertNotNull(classC);
    assertEquals(classC.getStatements().toString(), 2, classC.getStatements().size());
    Abstract.ClassDefinition classD = (Abstract.ClassDefinition) getField(classA, "D");
    assertNotNull(classD);
    assertEquals(classD.getStatements().toString(), 1, classD.getStatements().size());
    */
    assertTrue(false);
  }

  private Abstract.Definition getField(Abstract.ClassDefinition classDefinition, String name) {
    /*
    for (Abstract.Statement statement : classDefinition.getStatements()) {
      if (statement instanceof Abstract.DefineStatement && ((Abstract.DefineStatement) statement).getDefinition().getName().equals(name)) {
        return ((Abstract.DefineStatement) statement).getDefinition();
      }
    }
    */
    return null;
  }

  @Test
  public void defineExistingTestError() {
    resolveNamesDef("\\class Test { \\function A => 0 \\function B => A } \\where { \\class A { } }", 1);
  }

  @Test
  public void defineExistingStaticTestError() {
    resolveNamesClass("\\class A { } \\function A => 0", 1);
  }

  @Test
  public void defineExistingDynamicTestError() {
    resolveNamesDef("\\class Test { \\class A \\function A => 0 }", 1);
  }

  @Ignore
  @Test
  public void exportExistingTestError() {
    resolveNamesClass("\\class A \\where { \\class B \\where { \\function x => 0 } } \\export A \\class B \\where { \\function y => 0 }", 1);
  }

  @Ignore
  @Test
  public void exportExistingTestError2() {
    resolveNamesClass("\\class B \\where { \\function y => 0 } \\class A \\where { \\class B \\where { \\function x => 0 } } \\export A", 1);
  }

  @Test
  public void openInsideTest() {
    resolveNamesClass("\\class A \\where { \\class B \\where { \\function x => 0 } \\open B } \\function y => A.x", 1);
  }

  @Ignore
  @Test
  public void exportInsideTest() {
    resolveNamesClass("\\class A \\where { \\class B \\where { \\function x => 0 } \\export B } \\function y => A.x");
  }

  @Test
  public void classExtensionWhereTestError() {
    resolveNamesClass(
        "\\function f => 0 \\where {\n" +
        "  \\class A {}\n" +
        "  \\class A { \\function x => 0 }\n" +
        "}", 1);
  }

  @Test
  public void multipleDefsWhere() {
    resolveNamesClass(
        "\\function f => 0 \\where {\n" +
        "  \\function d => 0\n" +
        "  \\function d => 1\n" +
        "}", 1);
  }

  @Test
  public void dataConstructor() {
    resolveNamesClass("\\data D | d \\function f => D.d");
  }

  @Test
  public void testPreludeSuc() {
    loadPrelude();
    resolveNamesDef("\\function test' => ::Prelude.suc");
  }

  @Test
  public void testPreludeNonExistentMember() {
    loadPrelude();
    resolveNamesDef("\\function test' => ::Prelude.foo", 1);
  }

  @Test
  public void testPreludeNotLoaded() {
    resolveNamesDef("\\function test' => ::Prelude.suc", 1);
  }

  @Test
  public void openDuplicate() {
    resolveNamesClass(
        "\\function f => \\Type0\n" +
        "\\class X \\where { \\function f => \\Type0 }\n" +
        "\\open X\n" +
        "\\function g => f", 1);
  }

  @Test
  public void openDuplicateModule() {
    resolveNamesClass(
        "\\class X \\where { \\function f => \\Type0 }\n" +
        "\\class Y \\where { \\function f => \\Type0 }\n" +
        "\\open X\n" +
        "\\open Y\n" +
        "\\function g => f", 1);
  }

  @Test
  public void openDuplicateModuleHiding() {
    resolveNamesClass(
        "\\class X \\where { \\function f => \\Type0 }\n" +
        "\\class Y \\where { \\function f => \\Type0 }\n" +
        "\\open X\n" +
        "\\open Y \\hiding (f)\n" +
        "\\function g => f");
  }
}
