package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
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
  public void recordTest() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract f : Nat -> \\Type0\n" +
        "  \\abstract g : f 0\n" +
        "}\n" +
        "\\static \\function f (b : B) : b.f 0 => b.g");
  }

  @Test
  public void innerRecordTest() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract f : Nat -> \\Type0\n" +
        "  \\class A {\n" +
        "    \\abstract g : f 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function f (b : B) (a : b.A) : b.f 0 => a.g");
  }

  @Test
  public void unknownExtTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { \\override x => 0 \\override z => 0 \\override y => 0 }", 1);
  }

  @Test
  public void typeMismatchMoreTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { \\override x (a : Nat) => a }", 1);
  }

  @Test
  public void typeMismatchLessTest() {
    typeCheckClass(
        "\\static \\class C { \\abstract f (x y z : Nat) : Nat }\n" +
        "\\static \\function D => C { \\override f a => \\lam z w => z }");
  }

  @Test
  public void argTypeMismatchTestError() {
    typeCheckClass(
        "\\static \\class C { \\abstract f (a : Nat) : Nat }\n" +
        "\\static \\function D => C { \\override f (a : Nat -> Nat) => 0 }", 1);
  }

  @Test
  public void resultTypeMismatchTestError() {
    typeCheckClass(
        "\\static \\class Point { \\abstract x : Nat \\abstract y : Nat }\n" +
        "\\static \\function C => Point { \\override x => \\lam (t : Nat) => t }", 1);
  }

  @Test
  public void parentCallTest() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract c : Nat -> Nat -> Nat\n" +
        "  \\abstract f : Nat -> Nat\n" +
        "}\n" +
        "\\static \\function B => A {\n" +
        "  \\override f n <= c n n\n" +
        "}");
  }

  @Test
  public void recursiveTestError() {
    typeCheckClass(
        "\\static \\class A { \\abstract f : Nat -> Nat }\n" +
        "\\static \\function B => A { \\override f n <= \\elim n | zero => zero | suc n' => f (suc n') }", 1);
  }

  @Test
  public void duplicateNameTestError() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract f : Nat -> Nat\n" +
        "}\n" +
        "\\static \\function B => A {\n" +
        "  \\function f (n : Nat) <= n\n" +
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
        "  \\override x => d\n" +
        "  \\override y => d\n" +
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
        "\\static \\function diagonal => Point {\n" +
        "  \\override y => x\n" +
        "}\n" +
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
        "  \\override x => d\n" +
        "  \\override y => d\n" +
        "}\n" +
        "\\static \\function diagonal1 => Point {\n" +
        "  \\override x => 0\n" +
        "  \\override y => x\n" +
        "}\n" +
        "\\static \\function test : \\new diagonal1 = \\new diagonal 0 => path (\\lam _ => \\new diagonal 0)");
  }

  @Test
  public void mutualRecursionTestError() {
    typeCheckClass(
        "\\static \\class Point {\n" +
        "  \\abstract x : Nat\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\static \\function test => Point {\n" +
        "  \\override x => y\n" +
        "  \\override y => x\n" +
        "}", 1);
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
        "\\static \\function C => Point { \\override x => 0 }");
    Namespace namespace = result.getParentNamespace().findChild(result.getName().name);
    assertEquals(new Universe.Type(0, Universe.Type.SET), namespace.getDefinition("Point").getUniverse());
    assertEquals(new Universe.Type(0, Universe.Type.SET), namespace.getDefinition("C").getUniverse());
  }

  @Test
  public void recordConstructorsTest() {
    ClassDefinition classDef = typeCheckClass(
        "\\static \\class A { \\abstract x : Nat \\data Foo | foo (x = 0) \\abstract y : foo = foo }\n" +
        "\\static \\function test (p : A) => p.y");
    Namespace namespace = classDef.getParentNamespace().findChild(classDef.getName().name);
    Expression resultType = ((FunctionDefinition) namespace.getDefinition("test")).getResultType();
    List<Expression> arguments = new ArrayList<>(3);
    Expression function = resultType.normalize(NormalizeVisitor.Mode.WHNF).getFunction(arguments);
    assertEquals(3, arguments.size());
    assertEquals(DefCall(Prelude.PATH), function);

    assertTrue(arguments.get(0) instanceof DefCallExpression);
    assertEquals(Index(0), ((DefCallExpression) arguments.get(0)).getExpression());
    assertEquals(namespace.findChild("A").getDefinition("foo"), ((DefCallExpression) arguments.get(0)).getDefinition());

    assertTrue(arguments.get(1) instanceof DefCallExpression);
    assertEquals(Index(0), ((DefCallExpression) arguments.get(1)).getExpression());
    assertEquals(namespace.findChild("A").getDefinition("foo"), ((DefCallExpression) arguments.get(1)).getDefinition());

    assertTrue(arguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) arguments.get(2)).getBody() instanceof PiExpression);
    List<Expression> domArguments = new ArrayList<>(3);
    Expression domFunction = ((PiExpression) ((LamExpression) arguments.get(2)).getBody()).getArguments().get(0).getType().getFunction(domArguments);
    assertEquals(3, domArguments.size());
    assertEquals(DefCall(Prelude.PATH), domFunction);

    assertTrue(domArguments.get(0) instanceof DefCallExpression);
    assertEquals(Prelude.ZERO, ((DefCallExpression) domArguments.get(0)).getDefinition());

    assertTrue(domArguments.get(1) instanceof DefCallExpression);
    assertEquals(Index(1), ((DefCallExpression) domArguments.get(1)).getExpression());
    assertEquals(namespace.findChild("A").getDefinition("x"), ((DefCallExpression) domArguments.get(1)).getDefinition());

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
      "  \\abstract y : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))\n" +
      "}\n" +
      "\\static \\function test (q : A) => q.y");
    Namespace namespace = classDef.getParentNamespace().findChild(classDef.getName().name);
    Expression resultType = ((FunctionDefinition) namespace.getDefinition("test")).getResultType();
    List<Expression> arguments = new ArrayList<>(3);
    Expression function = resultType.normalize(NormalizeVisitor.Mode.WHNF).getFunction(arguments);
    assertEquals(3, arguments.size());
    assertEquals(DataCall(Prelude.PATH), function);

    assertTrue(arguments.get(0) instanceof AppExpression);
    assertTrue(((AppExpression) arguments.get(0)).getFunction() instanceof DefCallExpression);
    assertEquals(Index(0), ((DefCallExpression) ((AppExpression) arguments.get(0)).getFunction()).getExpression());
    assertEquals(namespace.findChild("A").getDefinition("foo"), ((DefCallExpression) ((AppExpression) arguments.get(0)).getFunction()).getDefinition());
    assertTrue(((AppExpression) arguments.get(0)).getArgument().getExpression() instanceof AppExpression);
    AppExpression appPath00 = (AppExpression) ((AppExpression) arguments.get(0)).getArgument().getExpression();
    assertTrue(appPath00.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath00.getArgument().getExpression()).getBody() instanceof AppExpression);
    AppExpression appPath01 = (AppExpression) ((LamExpression) appPath00.getArgument().getExpression()).getBody();
    assertTrue(appPath01.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath01.getArgument().getExpression()).getBody() instanceof DefCallExpression);
    assertEquals(Index(2), ((DefCallExpression) ((LamExpression) appPath01.getArgument().getExpression()).getBody()).getExpression());
    assertEquals(((ClassDefinition) namespace.getMember("A").definition).getField("x"), ((DefCallExpression) ((LamExpression) appPath01.getArgument().getExpression()).getBody()).getDefinition());

    assertTrue(arguments.get(1) instanceof AppExpression);
    assertTrue(((AppExpression) arguments.get(1)).getFunction() instanceof DefCallExpression);
    assertEquals(Index(0), ((DefCallExpression) ((AppExpression) arguments.get(1)).getFunction()).getExpression());
    assertEquals(namespace.findChild("A").getDefinition("foo"), ((DefCallExpression) ((AppExpression) arguments.get(1)).getFunction()).getDefinition());
    assertTrue(((AppExpression) arguments.get(1)).getArgument().getExpression() instanceof AppExpression);
    AppExpression appPath10 = (AppExpression) ((AppExpression) arguments.get(1)).getArgument().getExpression();
    assertTrue(appPath10.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath10.getArgument().getExpression()).getBody() instanceof AppExpression);
    AppExpression appPath11 = (AppExpression) ((LamExpression) appPath10.getArgument().getExpression()).getBody();
    assertTrue(appPath11.getArgument().getExpression() instanceof LamExpression);
    assertTrue(((LamExpression) appPath11.getArgument().getExpression()).getBody() instanceof DefCallExpression);
    assertEquals(Index(2), ((DefCallExpression) ((LamExpression) appPath11.getArgument().getExpression()).getBody()).getExpression());
    assertEquals(((ClassDefinition) namespace.getMember("A").definition).getField("x"), ((DefCallExpression) ((LamExpression) appPath11.getArgument().getExpression()).getBody()).getDefinition());

    assertTrue(arguments.get(2) instanceof LamExpression);
    assertTrue(((LamExpression) arguments.get(2)).getBody() instanceof AppExpression);
    assertEquals(DefCall(namespace.findChild("A").getDefinition("Foo")), ((AppExpression) ((LamExpression) arguments.get(2)).getBody()).getFunction());
    List<Expression> parameterArguments = new ArrayList<>(1);
    Expression parameterFunction = ((AppExpression) ((LamExpression) arguments.get(2)).getBody()).getArgument().getExpression().getFunction(parameterArguments);
    assertEquals(1, parameterArguments.size());
    assertEquals(ConCall(Prelude.PATH_CON), parameterFunction);
    assertTrue(parameterArguments.get(0) instanceof LamExpression);
    assertTrue(((LamExpression) parameterArguments.get(0)).getBody() instanceof DefCallExpression);
    assertEquals(Index(2), ((DefCallExpression) ((LamExpression) parameterArguments.get(0)).getBody()).getExpression());
    assertEquals(((ClassDefinition) namespace.getMember("A").definition).getField("x"), ((DefCallExpression) ((LamExpression) parameterArguments.get(0)).getBody()).getDefinition());

    List<Expression> parameters = ((ConCallExpression) parameterFunction).getParameters();
    assertEquals(3, parameters.size());

    assertTrue(parameters.get(0) instanceof LamExpression);
    assertEquals(Nat(), ((LamExpression) parameters.get(0)).getBody());

    parameters.set(1, parameters.get(1).normalize(NormalizeVisitor.Mode.WHNF));
    assertTrue(parameters.get(1) instanceof DefCallExpression);
    assertEquals(Index(1), ((DefCallExpression) parameters.get(1)).getExpression());
    assertEquals(((ClassDefinition) namespace.getMember("A").definition).getField("x"), ((DefCallExpression) parameters.get(1)).getDefinition());

    parameters.set(2, parameters.get(2).normalize(NormalizeVisitor.Mode.WHNF));
    assertTrue(parameters.get(2) instanceof DefCallExpression);
    assertEquals(Index(1), ((DefCallExpression) parameters.get(2)).getExpression());
    assertEquals(((ClassDefinition) namespace.getMember("A").definition).getField("x"), ((DefCallExpression) parameters.get(2)).getDefinition());
  }
}
