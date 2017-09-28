package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.error.DummyErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.naming.scope.NamespaceScope;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.Precedence;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.cBinOp;
import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.cNum;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NameResolverTest extends NameResolverTestCase {
  @Test
  public void parserInfix() {
    GlobalReference plusRef = new GlobalReference(null, "+", new Precedence(Precedence.Associativity.LEFT_ASSOC, (byte) 6));
    Concrete.Definition plus = new Concrete.FunctionDefinition(plusRef, Collections.emptyList(), null, null);
    plusRef.setDefinition(plus);
    GlobalReference mulRef = new GlobalReference(null, "*", new Precedence(Precedence.Associativity.LEFT_ASSOC, (byte) 7));
    Concrete.Definition mul = new Concrete.FunctionDefinition(mulRef, Collections.emptyList(), null, null);
    mulRef.setDefinition(mul);

    SimpleNamespace namespace = new SimpleNamespace();
    namespace.addDefinition(plus.getData(), DummyErrorReporter.INSTANCE);
    namespace.addDefinition(mul.getData(), DummyErrorReporter.INSTANCE);

    Concrete.Expression result = resolveNamesExpr(new NamespaceScope(namespace), "0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)");
    assertNotNull(result);
    assertTrue(compareAbstract(cBinOp(cBinOp(cNum(0), plusRef, cBinOp(cNum(1), mulRef, cNum(2))), plusRef, cBinOp(cBinOp(cNum(3), mulRef, cBinOp(cNum(4), mulRef, cNum(5))), mulRef, cBinOp(cNum(6), plusRef, cNum(7)))), result));
  }

  @Test
  public void parserInfixError() {
    GlobalReference plusRef = new GlobalReference(null, "+", new Precedence(Precedence.Associativity.LEFT_ASSOC, (byte) 6));
    Concrete.Definition plus = new Concrete.FunctionDefinition(plusRef, Collections.emptyList(), null, null);
    plusRef.setDefinition(plus);
    GlobalReference mulRef = new GlobalReference(null, "*", new Precedence(Precedence.Associativity.RIGHT_ASSOC, (byte) 6));
    Concrete.Definition mul = new Concrete.FunctionDefinition(mulRef, Collections.emptyList(), null, null);
    mulRef.setDefinition(mul);

    SimpleNamespace namespace = new SimpleNamespace();
    namespace.addDefinition(plus.getData(), DummyErrorReporter.INSTANCE);
    namespace.addDefinition(mul.getData(), DummyErrorReporter.INSTANCE);

    resolveNamesExpr(new NamespaceScope(namespace), "11 + 2 * 3", 1);
  }

  @Test
  public void whereTest() {
    resolveNamesModule(
        "\\function f (x : \\Type0) => B.b (a x) \\where {\n" +
            "  \\function a (x : \\Type0) => x\n" +
            "  \\data D | D1 | D2\n" +
            "  \\class B \\where { \\data C | cr \\function b (x : \\Type0) => D1 }\n" +
            "}");
  }

  @Test
  public void whereTestDefCmd() {
    resolveNamesModule(
        "\\function f (x : \\Type0) => a \\where {\n" +
        "  \\class A \\where { \\function a => 0 }\n" +
        "  \\open A\n" +
        "}");
  }

  @Test
  public void whereOpenFunction() {
    resolveNamesModule(
        "\\function f => x \\where {\n" +
        "  \\function b => 0 \\where\n" +
        "    \\function x => 0\n" +
        "  \\open b(x)\n" +
        "}");
  }

  @Test
  public void whereNested() {
    resolveNamesModule(
        "\\function f => x \\where {\n" +
        "  \\data B | b\n" +
        "  \\function x => a \\where\n" +
        "    \\function a => b\n" +
        "}");
  }

  @Test
  public void whereOuterScope() {
    resolveNamesModule(
        "\\function f => 0 \\where {\n" +
        "  \\function g => 0\n" +
        "  \\function h => g\n" +
        "}");
  }

  @Test
  public void whereInSignature() {
    resolveNamesModule(
        "\\function f : D => d \\where\n" +
        "  \\data D | d");
  }

  @Test
  public void whereAccessOuter() {
    resolveNamesModule(
        "\\function f => 0 \\where\n" +
        "  \\function x => 0\n" +
        "\\function g => f.x");
  }

  @Test
  public void whereNonStaticOpen() {
    resolveNamesModule(
        "\\function f => 0 \\where {\n" +
        "  \\function x => 0\n" +
        "  \\function y => x\n" +
        "}\n" +
        "\\function g => 0 \\where\n" +
        "  \\open f(y)");
  }

  @Test
  public void openTest() {
    resolveNamesModule("\\class A \\where { \\function x => 0 } \\open A \\function y => x");
  }

  @Test
  public void openTest2() {
    resolveNamesModule("\\class A \\where { \\function x => 0 } \\function y => x \\open A");
  }

  @Ignore
  @Test
  public void exportTest() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\function x => 0 } \\export B } \\function y => A.x");
  }

  @Test
  public void staticFieldAccCallTest() {
    resolveNamesModule("\\class A { | x : \\Type0 \\class B \\where { \\function y => x } } \\function f (a : A) => a.B.y");
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
    assertEquals(3, ((Concrete.ClassDefinition) staticNamespace.getMember("A").abstractDefinition).getStatements().size());
    */
    assertTrue(false);
  }

  @Ignore
  @Test
  public void exportTest2() {
    /*
    resolveNamesDef(
        "\\class Test {\n" +
        "  | (+) (x y : \\Type0) : \\Type0\n" +
        "  \\class A {\n" +
        "    | x : \\Type0\n" +
        "    \\class B {\n" +
        "      | y : \\Type0\n" +
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
    Concrete.ClassDefinition classA = (Concrete.ClassDefinition) namespace.getMember("A").abstractDefinition;
    assertEquals(classA.getStatements().toString(), 4, classA.getStatements().size());
    assertEquals(namespace.getMember("A").namespace.getMembers().toString(), 4, namespace.getMember("A").namespace.getMembers().size());
    Concrete.ClassDefinition classB = (Concrete.ClassDefinition) getField(classA, "B");
    assertNotNull(classB);
    assertEquals(classB.getStatements().toString(), 3, classB.getStatements().size());
    Concrete.ClassDefinition classC = (Concrete.ClassDefinition) getField(classB, "C");
    assertNotNull(classC);
    assertEquals(classC.getStatements().toString(), 2, classC.getStatements().size());
    Concrete.ClassDefinition classD = (Concrete.ClassDefinition) getField(classA, "D");
    assertNotNull(classD);
    assertEquals(classD.getStatements().toString(), 1, classD.getStatements().size());
    */
    assertTrue(false);
  }

  private Concrete.Definition getField(Concrete.ClassDefinition classDefinition, String name) {
    /*
    for (Concrete.Statement statement : classDefinition.getStatements()) {
      if (statement instanceof Concrete.DefineStatement && ((Concrete.DefineStatement) statement).getDefinition().textRepresentation().equals(name)) {
        return ((Concrete.DefineStatement) statement).getDefinition();
      }
    }
    */
    return null;
  }

  @Test
  public void useExistingTestError() {
    resolveNamesDef("\\class Test { \\function A => 0 \\function B => A } \\where { \\class A { } }", 1);
  }

  @Test
  public void defineExistingStaticTestError() {
    resolveNamesModule("\\class A { } \\function A => 0", 1);
  }

  @Test
  public void defineExistingDynamicTestError() {
    resolveNamesDef("\\class Test { \\class A \\function A => 0 }", 1);
  }

  @Ignore
  @Test
  public void exportExistingTestError() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\function x => 0 } } \\export A \\class B \\where { \\function y => 0 }", 2);
  }

  @Ignore
  @Test
  public void exportExistingTestError2() {
    resolveNamesModule("\\class B \\where { \\function y => 0 } \\class A \\where { \\class B \\where { \\function x => 0 } } \\export A", 2);
  }

  @Test
  public void openInsideTest() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\function x => 0 } \\open B } \\function y => A.x", 1);
  }

  @Ignore
  @Test
  public void exportInsideTest() {
    resolveNamesModule("\\class A \\where { \\class B \\where { \\function x => 0 } \\export B } \\function y => A.x");
  }

  @Test
  public void classExtensionWhereTestError() {
    resolveNamesModule(
        "\\function f => 0 \\where {\n" +
        "  \\class A {}\n" +
        "  \\class A { \\function x => 0 }\n" +
        "}", 1);
  }

  @Test
  public void multipleDefsWhere() {
    resolveNamesModule(
        "\\function f => 0 \\where {\n" +
        "  \\function d => 0\n" +
        "  \\function d => 1\n" +
        "}", 1);
  }

  @Test
  public void dataConstructor() {
    resolveNamesModule("\\data D | d \\function f => D.d");
  }

  @Test
  public void testPreludeSuc() {
    loadPrelude();
    resolveNamesDef("\\function test' => suc");
  }

  @Test
  public void testPreludeNonExistentMember() {
    loadPrelude();
    resolveNamesDef("\\function test' => foo", 1);
  }

  @Test
  public void testPreludeNotLoaded() {
    resolveNamesDef("\\function test' => suc", 1);
  }

  @Test
  public void openDuplicate() {
    resolveNamesModule(
        "\\function f => \\Type0\n" +
        "\\class X \\where { \\function f => \\Type0 }\n" +
        "\\open X\n" +
        "\\function g => f");
  }

  @Test
  public void openDuplicateModule() {
    resolveNamesModule(
        "\\class X \\where { \\function f => \\Type0 }\n" +
        "\\class Y \\where { \\function f => \\Type0 }\n" +
        "\\open X\n" +
        "\\open Y\n" +
        "\\function g => f", 1);
  }

  @Test
  public void openDuplicateModuleHiding() {
    resolveNamesModule(
        "\\class X \\where { \\function f => \\Type0 }\n" +
        "\\class Y \\where { \\function f => \\Type0 }\n" +
        "\\open X\n" +
        "\\open Y \\hiding (f)\n" +
        "\\function g => f");
  }

  @Test
  public void conditionsTest() {
    resolveNamesModule(
      "\\data I | left | right\n" +
      "\\data D (A : \\Type)\n" +
      "  | con1 A\n" +
      "  | con2 (D A) I {\n" +
      "    | left => d\n" +
      "    | right => d\n" +
      "  }", 2);
  }

  @Test
  public void patternsTest() {
    resolveNamesClass(
      "\\data K\n" +
      "  | k1 \\Prop\n" +
      "  | k2 \\Prop\n" +
      "\\function crash (k : K) : \\Prop => \\elim k\n" +
      "  | k1 a => a\n" +
      "  | k2 b => a", 1);
  }
}
