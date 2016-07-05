package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.module.NameModuleID;
import com.jetbrains.jetpad.vclang.module.Root;
import com.jetbrains.jetpad.vclang.naming.namespace.EmptyNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.SimpleNamespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;
import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesDef;
import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesExpr;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.compare;
import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.cBinOp;
import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.cNum;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class NameResolverTest {
  @Test
  public void parserInfix() {
    DependentLink parameters = param(true, vars("x", "y"), Nat());
    Definition plus = new FunctionDefinition("+", new Abstract.Definition.Precedence(Abstract.Binding.Associativity.LEFT_ASSOC, (byte) 6), EmptyNamespace.INSTANCE, parameters, Nat(), EmptyElimTreeNode.getInstance());
    Definition mul = new FunctionDefinition("*", new Abstract.Definition.Precedence(Abstract.Binding.Associativity.LEFT_ASSOC, (byte) 7), EmptyNamespace.INSTANCE, parameters, Nat(), EmptyElimTreeNode.getInstance());

    SimpleNamespace namespace = new SimpleNamespace();
    namespace.addDefinition(plus);
    namespace.addDefinition(mul);

    Concrete.Expression result = resolveNamesExpr(namespace, "0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)");
    assertNotNull(result);
    assertTrue(compare(cBinOp(cBinOp(cNum(0), plus, cBinOp(cNum(1), mul, cNum(2))), plus, cBinOp(cBinOp(cNum(3), mul, cBinOp(cNum(4), mul, cNum(5))), mul, cBinOp(cNum(6), plus, cNum(7)))), result));
  }

  @Test
  public void parserInfixError() {
    DependentLink parameters = param(true, vars("x", "y"), Nat());
    Definition plus = new FunctionDefinition("+", new Abstract.Definition.Precedence(Abstract.Binding.Associativity.LEFT_ASSOC, (byte) 6), EmptyNamespace.INSTANCE, parameters, Nat(), EmptyElimTreeNode.getInstance());
    Definition mul = new FunctionDefinition("*", new Abstract.Definition.Precedence(Abstract.Binding.Associativity.RIGHT_ASSOC, (byte) 6), EmptyNamespace.INSTANCE, parameters, Nat(), EmptyElimTreeNode.getInstance());

    SimpleNamespace namespace = new SimpleNamespace();
    namespace.addDefinition(plus);
    namespace.addDefinition(mul);

    resolveNamesExpr(namespace, "11 + 2 * 3", 1);
  }

  @Test
  public void whereTest() {
    resolveNamesClass("test",
        "\\static \\function f (x : Nat) => B.b (a x) \\where {\n" +
            "  \\static \\function a (x : Nat) => x\n" +
            "  \\static \\data D | D1 | D2\n" +
            "  \\static \\class B { \\static \\data C | cr \\static \\function b (x : Nat) => D1 }\n" +
            "}");
  }

  @Test
  public void whereTestDefCmd() {
    resolveNamesClass("test",
        "\\static \\function f (x : Nat) => a \\where {\n" +
        "  \\static \\class A { \\static \\function a => 0 }\n" +
        "  \\open A\n" +
        "}");
  }

  @Test
  public void whereOpenFunction() {
    resolveNamesClass("test",
        "\\static \\function f => x \\where {\n" +
        "  \\static \\function b => 0 \\where\n" +
        "    \\static \\function x => 0\n" +
        "  \\open b(x)\n" +
        "}");
  }

  @Test
  public void whereNested() {
    resolveNamesClass("test",
        "\\static \\function f => x \\where {\n" +
        "  \\static \\data B | b\n" +
        "  \\static \\function x => a \\where\n" +
        "    \\static \\function a => b\n" +
        "}");
  }

  @Test
  public void whereOuterScope() {
    resolveNamesClass("test",
        "\\static \\function f => 0 \\where {\n" +
        "  \\static \\function g => 0\n" +
        "  \\static \\function h => g\n" +
        "}");
  }

  @Test
  public void whereInSignature() {
    resolveNamesClass("test",
        "\\static \\function f : D => d \\where\n" +
        "  \\static \\data D | d");
  }

  @Test
  public void whereAccessOuter() {
    resolveNamesClass("test",
        "\\static \\function f => 0 \\where\n" +
        "  \\static \\function x => 0\n" +
        "\\static \\function g => f.x");
  }

  @Test
  public void whereNonStaticOpen() {
    resolveNamesClass("test",
        "\\static \\function f => 0 \\where {\n" +
        "  \\static \\function x => 0\n" +
        "  \\static \\function y => x\n" +
        "}\n" +
        "\\static \\function g => 0 \\where\n" +
        "  \\open f(y)");
  }

  @Test
  public void whereAbstractError() {
    resolveNamesClass("test", "\\static \\function f => 0 \\where \\abstract x : Nat", 1);
  }

  @Test
  public void numberOfFieldsTest() {
    resolveNamesClass("test", "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat } \\static \\function C => Point { x => 0 }");
    assertNotNull(Root.getModule(new NameModuleID("test")));
    Namespace namespace = Root.getModule(new NameModuleID("test")).namespace;

    assertEquals(2, namespace.getMembers().size());
    assertEquals(2, namespace.getChild("Point").getMembers().size());
    assertEquals(2, ((Abstract.ClassDefinition) namespace.getMember("Point").abstractDefinition).getStatements().size());
  }

  @Test
  public void numberOfFieldsTest2() {
    resolveNamesClass("test",
        "\\abstract f : Nat\n" +
        "\\static \\function g => 0\n" +
        "\\class B { \\function h => 0 \\static \\function k => 0 }\n" +
        "\\static \\class C { \\function h => 0 \\static \\function k => 0 }");
    assertNotNull(Root.getModule(new NameModuleID("test")));
    Namespace namespace = Root.getModule(new NameModuleID("test")).namespace;

    assertEquals(4, namespace.getMembers().size());
    assertNotNull(namespace.getMember("f"));
    assertNotNull(namespace.getMember("g"));
    assertTrue(namespace.getMember("B").abstractDefinition instanceof Abstract.ClassDefinition);
    assertEquals(2, namespace.getMember("B").namespace.getMembers().size());
    assertEquals(2, ((Abstract.ClassDefinition) namespace.getMember("B").abstractDefinition).getStatements().size());
    assertTrue(namespace.getMember("C").abstractDefinition instanceof Abstract.ClassDefinition);
    assertEquals(2, namespace.getMember("C").namespace.getMembers().size());
    assertEquals(2, ((Abstract.ClassDefinition) namespace.getMember("C").abstractDefinition).getStatements().size());
  }

  @Test
  public void openTest() {
    resolveNamesClass("test", "\\static \\class A { \\static \\function x => 0 } \\open A \\static \\function y => x");
  }

  @Test
  public void exportTest() {
    resolveNamesClass("test", "\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\export B } \\static \\function y => A.x");
  }

  @Test
  public void staticFieldAccCallTest() {
    resolveNamesClass("test", "\\static \\class A { \\abstract x : Nat \\class B { \\static \\function y => x } } \\static \\function f (a : A) => a.B.y");
  }

  @Test
  public void exportPublicFieldsTest() {
    resolveNamesClass("test", "\\static \\class A { \\static \\function x => 0 \\static \\class B { \\static \\function y => x } \\export B } \\static \\function f => A.y");
    assertNotNull(Root.getModule(new NameModuleID("test")));
    Namespace staticNamespace = Root.getModule(new NameModuleID("test")).namespace;

    assertEquals(2, staticNamespace.getMembers().size());
    assertNotNull(staticNamespace.getMember("A"));
    assertEquals(3, staticNamespace.getMember("A").namespace.getMembers().size());
    assertEquals(3, ((Abstract.ClassDefinition) staticNamespace.getMember("A").abstractDefinition).getStatements().size());
  }

  @Test
  public void exportTest2() {
    resolveNamesClass("test",
        "\\abstract (+) (x y : Nat) : Nat\n" +
        "\\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\class B {\n" +
        "    \\abstract y : Nat\n" +
        "    \\class C {\n" +
        "      \\static \\function z => x + y\n" +
        "      \\static \\function w => x\n" +
        "    }\n" +
        "    \\export C\n" +
        "  }\n" +
        "  \\class D { \\export B }\n" +
        "  \\function f (b : B) : b.C.z = b.z => path (\\lam _ => b.w + b.y)\n" +
        "}");
    assertNotNull(Root.getModule(new NameModuleID("test")));
    Namespace namespace = Root.getModule(new NameModuleID("test")).namespace;

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
  }

  private Abstract.Definition getField(Abstract.ClassDefinition classDefinition, String name) {
    for (Abstract.Statement statement : classDefinition.getStatements()) {
      if (statement instanceof Abstract.DefineStatement && ((Abstract.DefineStatement) statement).getDefinition().getName().equals(name)) {
        return ((Abstract.DefineStatement) statement).getDefinition();
      }
    }
    return null;
  }

  @Test
  public void defineExistingTestError() {
    resolveNamesClass("test", "\\static \\class A { } \\function A => 0", 1);
  }

  @Test
  public void defineExistingStaticTestError() {
    resolveNamesClass("test", "\\static \\class A { } \\static \\function A => 0", 1);
  }

  @Test
  public void defineExistingDynamicTestError() {
    resolveNamesClass("test", "\\class A { } \\function A => 0", 1);
  }

  @Test
  public void neverCloseField() {
    resolveNamesClass("test", "\\static \\class A { \\static \\function x => 0 } \\static \\class B { \\open A \\export A \\close A } \\static \\class C { \\static \\function y => B.x }");
  }

  @Test
  public void exportExistingTestError() {
    resolveNamesClass("test", "\\static \\class A { \\static \\class B { \\static \\function x => 0 } } \\export A \\static \\class B { \\static \\function y => 0 }", 1);
  }

  @Test
  public void exportExistingTestError2() {
    resolveNamesClass("test", "\\static \\class B { \\static \\function y => 0 } \\static \\class A { \\static \\class B { \\static \\function x => 0 } } \\export A", 1);
  }

  @Test
  public void openExportTest() {
    resolveNamesClass("test", "\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\open B } \\static \\function y => A.x");
  }

  @Test
  public void classExtensionWhereTestError() {
    resolveNamesClass("test",
        "\\static \\function f => 0 \\where {\n" +
        "  \\static \\class A {}\n" +
        "  \\static \\class A { \\function x => 0 }\n" +
        "}", 1);
  }

  @Test
  public void multipleDefsWhere() {
    resolveNamesClass("test",
        "\\static \\function f => 0 \\where {\n" +
        "  \\static \\function d => 0\n" +
        "  \\static \\function d => 1\n" +
        "}", 1);
  }

  @Test
  public void dataConstructor() {
    resolveNamesClass("test", "\\data D | d \\function f => D.d");
  }

  @Test
  public void testPrepreludeSuc() {
    resolveNamesDef(
        "\\static \\function test' => ::Preprelude.suc\n"
    );
  }
}
