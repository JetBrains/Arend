package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolverTestCase.resolveNamesClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecordsTest {
  @Test
  public void unknownExtTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => 0 | z => 0 | y => 0 }", 1);
  }

  /*
  @Test
  public void typeMismatchMoreTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x (a : Nat) => a }", 1);
  }

  @Test
  public void typeMismatchLessTest() {
    typeCheckClass(
        "\\static \\class C { \\abstract f (x y z : Nat) : Nat }\n" +
        "\\static \\function D => C { f a => \\lam z w => z }");
  }

  @Test
  public void argTypeMismatchTestError() {
    typeCheckClass(
        "\\static \\class C { \\abstract f (a : Nat) : Nat }\n" +
        "\\static \\function D => C { f (a : Nat -> Nat) => 0 }", 1);
  }
  */

  @Test
  public void resultTypeMismatchTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => \\lam (t : Nat) => t }", 1);
  }

  @Test
  public void parentCallTest() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract c : Nat -> Nat -> Nat\n" +
        "  \\abstract f : Nat -> Nat\n" +
        "}\n" +
        "\\static \\function B => A {\n" +
        "  f => \\lam n => c n n\n" +
        "}", 1);
  }

  /*
  @Test
  public void recursiveTestError() {
    typeCheckClass(
        "\\static \\class A { \\abstract f : Nat -> Nat }\n" +
        "\\static \\function B => A { \\override f n <= \\elim n | zero => zero | suc n' => f (suc n') }", 1);
  }
  */

  @Test
  public void duplicateNameTestError() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract f : Nat\n" +
        "}\n" +
        "\\static \\function B => A {\n" +
        "  | f => 0\n" +
        "  | f => 1\n" +
        "}", 1);
  }

  @Test
  public void overriddenFieldAccTest() {
    typeCheckClass(
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function diagonal => \\lam (d : Nat) => Point {\n" +
        "  | x => d\n" +
        "  | y => d\n" +
        "}\n" +
        "\\static \\function test (p : diagonal 0) : p.x = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void newAbstractTestError() {
    typeCheckClass(
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function diagonal => Point { y => 0 }\n" +
        "\\static \\function test => \\new diagonal", 1);
  }

  @Test
  public void newTest() {
    typeCheckClass(
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function diagonal => \\lam (d : Nat) => Point {\n" +
        "  | x => d\n" +
        "  | y => d\n" +
        "}\n" +
        "\\static \\function diagonal1 => Point {\n" +
        "  | x => 0\n" +
        "  | y => 0\n" +
        "}\n" +
        "\\static \\function test : \\new diagonal1 = \\new diagonal 0 => path (\\lam _ => \\new Point { x => 0 | y => 0 })");
  }

  @Test
  public void mutualRecursionTestError() {
    typeCheckClass("test",
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function test => Point {\n" +
        "  | x => y\n" +
        "  | y => x\n" +
        "}", 2);
  }

  @Test
  public void splitClassTestError() {
    resolveNamesClass("test",
        "\\static \\class A {\n" +
        "  \\static \\function x => 0\n" +
        "}\n" +
        "\\static \\class A {\n" +
        "  \\static \\function y => 0\n" +
        "}", 1);
  }

  @Test
  public void recordUniverseTest() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => 0 }");
    assertEquals(new Universe.Type(0, Universe.Type.SET), member.namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(0, Universe.Type.SET), member.namespace.getDefinition("C").getType());
  }

  @Test
  public void recordUniverseTest2() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => 0 | y => 1 }");
    assertEquals(new Universe.Type(0, Universe.Type.SET), member.namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(0, Universe.Type.PROP), member.namespace.getDefinition("C").getType());
  }

  @Test
  public void recordUniverseTest3() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class Point { \\abstract x : \\Type3 \\abstract y : \\Type1 }\n" +
        "\\static \\function C => Point { x => Nat }");
    assertEquals(new Universe.Type(4, Universe.Type.NOT_TRUNCATED), member.namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(2, Universe.Type.NOT_TRUNCATED), member.namespace.getDefinition("C").getType());
  }

  @Test
  public void recordConstructorsTest() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data Foo | foo (x = 0)\n" +
        "  \\function y : foo = foo => path (\\lam _ => foo)\n" +
        "}\n" +
        "\\static \\function test (p : A) => p.y");
    FunctionDefinition testFun = (FunctionDefinition) member.namespace.getDefinition("test");
    Expression resultType = testFun.getResultType();
    Expression function = resultType.normalize(NormalizeVisitor.Mode.WHNF);
    List<? extends Expression> arguments = function.getArguments();
    function = function.getFunction();
    assertEquals(3, arguments.size());
    assertEquals(DataCall(Prelude.PATH), function);

    DataDefinition Foo = (DataDefinition) member.namespace.findChild("A").getDefinition("Foo");
    Constructor foo = Foo.getConstructor("foo");

    assertTrue(arguments.get(2) instanceof ConCallExpression);
    assertEquals(1, ((ConCallExpression) arguments.get(2)).getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), ((ConCallExpression) arguments.get(2)).getDataTypeArguments().get(0));
    assertEquals(foo, ((DefCallExpression) arguments.get(2)).getDefinition());

    assertTrue(arguments.get(1) instanceof ConCallExpression);
    assertEquals(1, ((ConCallExpression) arguments.get(1)).getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), ((ConCallExpression) arguments.get(1)).getDataTypeArguments().get(0));
    assertEquals(foo, ((DefCallExpression) arguments.get(1)).getDefinition());

    assertTrue(arguments.get(0) instanceof LamExpression);
    assertTrue(((LamExpression) arguments.get(0)).getBody() instanceof PiExpression);
    Expression domFunction = ((PiExpression) ((LamExpression) arguments.get(0)).getBody()).getParameters().getType();
    List<? extends Expression> domArguments = domFunction.getArguments();
    domFunction = domFunction.getFunction();
    assertEquals(3, domArguments.size());
    assertEquals(DataCall(Prelude.PATH), domFunction);

    assertTrue(domArguments.get(0) instanceof DefCallExpression);
    assertEquals(Prelude.ZERO, ((DefCallExpression) domArguments.get(0)).getDefinition());

    assertTrue(domArguments.get(1) instanceof AppExpression);
    assertEquals(1, domArguments.get(1).getArguments().size());
    assertEquals(Reference(testFun.getParameters()), domArguments.get(1).getArguments().get(0));
    assertEquals(member.namespace.findChild("A").getDefinition("x").getDefCall(), domArguments.get(1).getFunction());

    assertTrue(domArguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) domArguments.get(2)).getBody() instanceof DefCallExpression);
    assertEquals(Prelude.NAT, ((DefCallExpression) ((LamExpression) domArguments.get(2)).getBody()).getDefinition());
  }

  @Test
  public void recordConstructorsParametersTest() {
    NamespaceMember member = typeCheckClass(
      "\\static \\class A {\n" +
      "  \\abstract x : Nat\n" +
      "  \\data Foo (p : x = x) | foo (p = p)\n" +
      "  \\function y (_ : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))) => 0\n" +
      "}\n" +
      "\\static \\function test (q : A) => q.y");
    FunctionDefinition testFun = (FunctionDefinition) member.namespace.getDefinition("test");
    Expression resultType = testFun.getResultType();
    Expression xCall = member.namespace.findChild("A").getDefinition("x").getDefCall();
    assertTrue(resultType instanceof PiExpression);
    Expression function = ((PiExpression) resultType).getParameters().getType().normalize(NormalizeVisitor.Mode.NF);
    List<? extends Expression> arguments = function.getArguments();
    function = function.getFunction();
    assertEquals(3, arguments.size());
    assertEquals(DataCall(Prelude.PATH), function);

    DataDefinition Foo = (DataDefinition) member.namespace.findChild("A").getDefinition("Foo");
    Constructor foo = Foo.getConstructor("foo");

    assertTrue(arguments.get(2) instanceof AppExpression);
    assertTrue(arguments.get(2).getFunction() instanceof ConCallExpression);
    assertEquals(2, ((ConCallExpression) arguments.get(2).getFunction()).getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), ((ConCallExpression) arguments.get(2).getFunction()).getDataTypeArguments().get(0));
    Expression expr1 = ((ConCallExpression) arguments.get(2).getFunction()).getDataTypeArguments().get(1);
    assertTrue(expr1 instanceof AppExpression);
    assertTrue(expr1.getArguments().get(0) instanceof LamExpression);
    assertTrue(((LamExpression) expr1.getArguments().get(0)).getBody() instanceof AppExpression);
    assertEquals(xCall, ((LamExpression) expr1.getArguments().get(0)).getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), ((LamExpression) expr1.getArguments().get(0)).getBody().getArguments().get(0));

    assertEquals(foo, ((DefCallExpression) arguments.get(2).getFunction()).getDefinition());
    assertTrue(arguments.get(2).getArguments().get(0) instanceof AppExpression);
    AppExpression appPath00 = (AppExpression) arguments.get(2).getArguments().get(0);
    assertTrue(appPath00.getArguments().get(0) instanceof LamExpression);
    assertTrue(((LamExpression) appPath00.getArguments().get(0)).getBody() instanceof AppExpression);
    AppExpression appPath01 = (AppExpression) ((LamExpression) appPath00.getArguments().get(0)).getBody();
    assertTrue(appPath01.getArguments().get(0) instanceof LamExpression);
    assertTrue(((LamExpression) appPath01.getArguments().get(0)).getBody() instanceof AppExpression);
    assertEquals(xCall, ((LamExpression) appPath01.getArguments().get(0)).getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), ((AppExpression) ((LamExpression) appPath01.getArguments().get(0)).getBody()).getArguments().get(0));

    assertTrue(arguments.get(1) instanceof AppExpression);
    assertTrue(arguments.get(1).getFunction() instanceof ConCallExpression);
    assertEquals(2, ((ConCallExpression) arguments.get(1).getFunction()).getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), ((ConCallExpression) arguments.get(1).getFunction()).getDataTypeArguments().get(0));
    assertEquals(expr1, ((ConCallExpression) arguments.get(1).getFunction()).getDataTypeArguments().get(1));
    assertEquals(foo, ((ConCallExpression) arguments.get(1).getFunction()).getDefinition());
    assertTrue(arguments.get(1).getArguments().get(0) instanceof AppExpression);
    AppExpression appPath10 = (AppExpression) arguments.get(1).getArguments().get(0);
    assertTrue(appPath10.getArguments().get(0) instanceof LamExpression);
    assertTrue(((LamExpression) appPath10.getArguments().get(0)).getBody() instanceof AppExpression);
    AppExpression appPath11 = (AppExpression) ((LamExpression) appPath10.getArguments().get(0)).getBody();
    assertTrue(appPath11.getArguments().get(0) instanceof LamExpression);
    assertTrue(((LamExpression) appPath11.getArguments().get(0)).getBody() instanceof AppExpression);
    assertEquals(xCall, ((LamExpression) appPath11.getArguments().get(0)).getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), ((LamExpression) appPath11.getArguments().get(0)).getBody().getArguments().get(0));

    assertTrue(arguments.get(0) instanceof LamExpression);
    assertTrue(((LamExpression) arguments.get(0)).getBody() instanceof AppExpression);
    assertEquals(Apps(Foo.getDefCall(), Reference(testFun.getParameters())), ((LamExpression) arguments.get(0)).getBody().getFunction());
    Expression parameterFunction = ((LamExpression) arguments.get(0)).getBody().getArguments().get(0);
    List<? extends Expression> parameterArguments = parameterFunction.getArguments();
    parameterFunction = parameterFunction.getFunction();
    assertEquals(1, parameterArguments.size());
    assertEquals(ConCall(Prelude.PATH_CON), parameterFunction);
    assertTrue(parameterArguments.get(0) instanceof LamExpression);
    assertTrue(((LamExpression) parameterArguments.get(0)).getBody() instanceof AppExpression);
    assertEquals(xCall, ((LamExpression) parameterArguments.get(0)).getBody().getFunction());
    assertEquals(Reference(testFun.getParameters()), ((LamExpression) parameterArguments.get(0)).getBody().getArguments().get(0));

    List<Expression> parameters = ((ConCallExpression) parameterFunction).getDataTypeArguments();
    assertEquals(3, parameters.size());

    assertTrue(parameters.get(0) instanceof LamExpression);
    assertEquals(Nat(), ((LamExpression) parameters.get(0)).getBody());

    Expression parameter1 = parameters.get(1).normalize(NormalizeVisitor.Mode.WHNF);
    assertTrue(parameter1 instanceof AppExpression);
    assertEquals(xCall, parameter1.getFunction());
    assertEquals(Reference(testFun.getParameters()), parameter1.getArguments().get(0));

    Expression parameter2 = parameters.get(2).normalize(NormalizeVisitor.Mode.WHNF);
    assertTrue(parameter2 instanceof AppExpression);
    assertEquals(xCall, parameter2.getFunction());
    assertEquals(Reference(testFun.getParameters()), parameter2.getArguments().get(0));
  }
}
