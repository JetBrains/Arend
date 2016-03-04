package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import org.junit.Test;

import java.util.ArrayList;
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
    ClassDefinition result = typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => 0 }");
    Namespace namespace = result.getResolvedName().toNamespace();
    assertEquals(new UniverseOld.Type(0, UniverseOld.Type.SET), namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(0, UniverseOld.Type.SET), namespace.getDefinition("C").getType());
  }

  @Test
  public void recordUniverseTest2() {
    ClassDefinition result = typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { x => 0 | y => 1 }");
    Namespace namespace = result.getResolvedName().toNamespace();
    assertEquals(new UniverseOld.Type(0, UniverseOld.Type.SET), namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(0, UniverseOld.Type.PROP), namespace.getDefinition("C").getType());
  }

  @Test
  public void recordUniverseTest3() {
    ClassDefinition result = typeCheckClass(
        "\\static \\class Point { \\abstract x : \\Type3 \\abstract y : \\Type1 }\n" +
        "\\static \\function C => Point { x => Nat }");
    Namespace namespace = result.getResolvedName().toNamespace();
    assertEquals(new UniverseOld.Type(4, UniverseOld.Type.NOT_TRUNCATED), namespace.getDefinition("Point").getUniverse());
    assertEquals(Universe(2, UniverseOld.Type.NOT_TRUNCATED), namespace.getDefinition("C").getType());
  }

  @Test
  public void recordConstructorsTest() {
    ClassDefinition classDef = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data Foo | foo (x = 0)\n" +
        "  \\function y : foo = foo => path (\\lam _ => foo)\n" +
        "}\n" +
        "\\static \\function test (p : A) => p.y");
    Namespace namespace = classDef.getResolvedName().toNamespace();
    FunctionDefinition testFun = (FunctionDefinition) namespace.getDefinition("test");
    Expression resultType = testFun.getResultType();
    List<Expression> arguments = new ArrayList<>(3);
    Expression function = resultType.normalize(NormalizeVisitor.Mode.WHNF).getFunction(arguments);
    assertEquals(3, arguments.size());
    assertEquals(DataCall(Prelude.PATH), function);

    DataDefinition Foo = (DataDefinition) namespace.findChild("A").getDefinition("Foo");
    Constructor foo = Foo.getConstructor("foo");

    assertTrue(arguments.get(0) instanceof ConCallExpression);
    assertEquals(1, ((ConCallExpression) arguments.get(0)).getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), ((ConCallExpression) arguments.get(0)).getDataTypeArguments().get(0));
    assertEquals(foo, ((DefCallExpression) arguments.get(0)).getDefinition());

    assertTrue(arguments.get(1) instanceof ConCallExpression);
    assertEquals(1, ((ConCallExpression) arguments.get(1)).getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), ((ConCallExpression) arguments.get(1)).getDataTypeArguments().get(0));
    assertEquals(foo, ((DefCallExpression) arguments.get(1)).getDefinition());

    assertTrue(arguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) arguments.get(2)).getBody() instanceof PiExpression);
    List<Expression> domArguments = new ArrayList<>(3);
    Expression domFunction = ((PiExpression) ((LamExpression) arguments.get(2)).getBody()).getParameters().getType().getFunction(domArguments);
    assertEquals(3, domArguments.size());
    assertEquals(DataCall(Prelude.PATH), domFunction);

    assertTrue(domArguments.get(0) instanceof DefCallExpression);
    assertEquals(Prelude.ZERO, ((DefCallExpression) domArguments.get(0)).getDefinition());

    assertTrue(domArguments.get(1) instanceof AppExpression);
    assertEquals(Reference(testFun.getParameters()), ((AppExpression) domArguments.get(1)).getArgument().getExpression());
    assertEquals(namespace.findChild("A").getDefinition("x").getDefCall(), ((AppExpression) domArguments.get(1)).getFunction());

    assertTrue(domArguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) domArguments.get(2)).getBody() instanceof DefCallExpression);
    assertEquals(Prelude.NAT, ((DefCallExpression) ((LamExpression) domArguments.get(2)).getBody()).getDefinition());
  }

  @Test
  public void recordConstructorsParametersTest() {
    ClassDefinition classDef = typeCheckClass(
      "\\static \\class A {\n" +
      "  \\abstract x : Nat\n" +
      "  \\data Foo (p : x = x) | foo (p = p)\n" +
      "  \\function y (_ : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))) => 0\n" +
      "}\n" +
      "\\static \\function test (q : A) => q.y");
    Namespace namespace = classDef.getResolvedName().toNamespace();
    FunctionDefinition testFun = (FunctionDefinition) namespace.getDefinition("test");
    Expression resultType = testFun.getResultType();
    Expression xCall = namespace.findChild("A").getDefinition("x").getDefCall();
    List<Expression> arguments = new ArrayList<>(3);
    assertTrue(resultType instanceof PiExpression);
    Expression function = ((PiExpression) resultType).getParameters().getType().normalize(NormalizeVisitor.Mode.NF).getFunction(arguments);
    assertEquals(3, arguments.size());
    assertEquals(DataCall(Prelude.PATH), function);

    DataDefinition Foo = (DataDefinition) namespace.findChild("A").getDefinition("Foo");
    Constructor foo = Foo.getConstructor("foo");

    assertTrue(arguments.get(0) instanceof AppExpression);
    assertTrue(((AppExpression) arguments.get(0)).getFunction() instanceof ConCallExpression);
    assertEquals(2, ((ConCallExpression) ((AppExpression) arguments.get(0)).getFunction()).getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), ((ConCallExpression) ((AppExpression) arguments.get(0)).getFunction()).getDataTypeArguments().get(0));
    Expression expr1 = ((ConCallExpression) ((AppExpression) arguments.get(0)).getFunction()).getDataTypeArguments().get(1);
    assertTrue(expr1 instanceof AppExpression);
    assertTrue(((AppExpression) expr1).getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) ((AppExpression) expr1).getArgument().getExpression()).getBody() instanceof AppExpression);
    assertEquals(xCall, ((AppExpression) ((LamExpression) ((AppExpression) expr1).getArgument().getExpression()).getBody()).getFunction());
    assertEquals(Reference(testFun.getParameters()), ((AppExpression) ((LamExpression) ((AppExpression) expr1).getArgument().getExpression()).getBody()).getArgument().getExpression());

    assertEquals(foo, ((DefCallExpression) ((AppExpression) arguments.get(0)).getFunction()).getDefinition());
    assertTrue(((AppExpression) arguments.get(0)).getArgument().getExpression() instanceof AppExpression);
    AppExpression appPath00 = (AppExpression) ((AppExpression) arguments.get(0)).getArgument().getExpression();
    assertTrue(appPath00.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath00.getArgument().getExpression()).getBody() instanceof AppExpression);
    AppExpression appPath01 = (AppExpression) ((LamExpression) appPath00.getArgument().getExpression()).getBody();
    assertTrue(appPath01.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath01.getArgument().getExpression()).getBody() instanceof AppExpression);
    assertEquals(xCall, ((AppExpression) ((LamExpression) appPath01.getArgument().getExpression()).getBody()).getFunction());
    assertEquals(Reference(testFun.getParameters()), ((AppExpression) ((LamExpression) appPath01.getArgument().getExpression()).getBody()).getArgument().getExpression());

    assertTrue(arguments.get(1) instanceof AppExpression);
    assertTrue(((AppExpression) arguments.get(1)).getFunction() instanceof ConCallExpression);
    assertEquals(2, ((ConCallExpression) ((AppExpression) arguments.get(1)).getFunction()).getDataTypeArguments().size());
    assertEquals(Reference(testFun.getParameters()), ((ConCallExpression) ((AppExpression) arguments.get(1)).getFunction()).getDataTypeArguments().get(0));
    assertEquals(expr1, ((ConCallExpression) ((AppExpression) arguments.get(1)).getFunction()).getDataTypeArguments().get(1));
    assertEquals(foo, ((ConCallExpression) ((AppExpression) arguments.get(1)).getFunction()).getDefinition());
    assertTrue(((AppExpression) arguments.get(1)).getArgument().getExpression() instanceof AppExpression);
    AppExpression appPath10 = (AppExpression) ((AppExpression) arguments.get(1)).getArgument().getExpression();
    assertTrue(appPath10.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath10.getArgument().getExpression()).getBody() instanceof AppExpression);
    AppExpression appPath11 = (AppExpression) ((LamExpression) appPath10.getArgument().getExpression()).getBody();
    assertTrue(appPath11.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath11.getArgument().getExpression()).getBody() instanceof AppExpression);
    assertEquals(xCall, ((AppExpression) ((LamExpression) appPath11.getArgument().getExpression()).getBody()).getFunction());
    assertEquals(Reference(testFun.getParameters()), ((AppExpression) ((LamExpression) appPath11.getArgument().getExpression()).getBody()).getArgument().getExpression());

    assertTrue(arguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) arguments.get(2)).getBody() instanceof AppExpression);
    assertEquals(Apps(Foo.getDefCall(), Reference(testFun.getParameters())), ((AppExpression) ((LamExpression) arguments.get(2)).getBody()).getFunction());
    List<Expression> parameterArguments = new ArrayList<>(1);
    Expression parameterFunction = ((AppExpression) ((LamExpression) arguments.get(2)).getBody()).getArgument().getExpression().getFunction(parameterArguments);
    assertEquals(1, parameterArguments.size());
    assertEquals(ConCall(Prelude.PATH_CON), parameterFunction);
    assertTrue(parameterArguments.get(0) instanceof LamExpression);
    assertTrue(((LamExpression) parameterArguments.get(0)).getBody() instanceof AppExpression);
    assertEquals(xCall, ((AppExpression) ((LamExpression) parameterArguments.get(0)).getBody()).getFunction());
    assertEquals(Reference(testFun.getParameters()), ((AppExpression) ((LamExpression) parameterArguments.get(0)).getBody()).getArgument().getExpression());

    List<Expression> parameters = ((ConCallExpression) parameterFunction).getDataTypeArguments();
    assertEquals(3, parameters.size());

    assertTrue(parameters.get(0) instanceof LamExpression);
    assertEquals(Nat(), ((LamExpression) parameters.get(0)).getBody());

    Expression parameter1 = parameters.get(1).normalize(NormalizeVisitor.Mode.WHNF);
    assertTrue(parameter1 instanceof AppExpression);
    assertEquals(xCall, ((AppExpression) parameter1).getFunction());
    assertEquals(Reference(testFun.getParameters()), ((AppExpression) parameter1).getArgument().getExpression());

    Expression parameter2 = parameters.get(2).normalize(NormalizeVisitor.Mode.WHNF);
    assertTrue(parameter2 instanceof AppExpression);
    assertEquals(xCall, ((AppExpression) parameter2).getFunction());
    assertEquals(Reference(testFun.getParameters()), ((AppExpression) parameter2).getArgument().getExpression());
  }
}
