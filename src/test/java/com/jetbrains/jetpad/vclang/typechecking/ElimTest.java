package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ListErrorReporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static org.junit.Assert.*;

public class ElimTest {
  @Test
  public void elim() {
    RootModule.initialize();
    Namespace testNS = RootModule.ROOT.getChild(new Name("test"));
    List<TypeArgument> parameters = new ArrayList<>(2);
    parameters.add(TypeArg(Nat()));
    parameters.add(Tele(vars("x", "y"), Nat()));
    List<TypeArgument> arguments1 = new ArrayList<>(1);
    List<TypeArgument> arguments2 = new ArrayList<>(2);
    arguments1.add(TypeArg(Nat()));
    arguments2.add(TypeArg(Pi(Nat(), Nat())));
    arguments2.add(Tele(vars("a", "b", "c"), Nat()));
    DataDefinition dataType = new DataDefinition(testNS, new Name("D"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), parameters);
    NamespaceMember member = testNS.addDefinition(dataType);
    dataType.addConstructor(new Constructor(member.namespace, new Name("con1"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), arguments1, dataType));
    dataType.addConstructor(new Constructor(member.namespace, new Name("con2"), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), arguments2, dataType));

    List<Argument> arguments3 = new ArrayList<>(4);
    arguments3.add(Tele(vars("a1", "b1", "c1"), Nat()));
    arguments3.add(Tele(vars("d1"), Apps(DataCall(dataType), Index(2), Index(1), Index(0))));
    arguments3.add(Tele(vars("a2", "b2", "c2"), Nat()));
    arguments3.add(Tele(vars("d2"), Apps(DataCall(dataType), Index(2), Index(1), Index(0))));
    List<Clause> clauses1 = new ArrayList<>(2);
    ElimExpression pTerm = Elim(Index(4), clauses1);
    clauses1.add(new Clause(match(dataType.getConstructor("con1"), match("s")), Abstract.Definition.Arrow.RIGHT, Nat(), pTerm));
    clauses1.add(new Clause(match(dataType.getConstructor("con2"), match("x"), match("y"), match("z"), match("t")), Abstract.Definition.Arrow.RIGHT, Pi(Nat(), Nat()), pTerm));
    FunctionDefinition pFunction = new FunctionDefinition(testNS, new Name("P"), Abstract.Definition.DEFAULT_PRECEDENCE, arguments3, Universe(), Abstract.Definition.Arrow.LEFT, pTerm);
    testNS.addDefinition(pFunction);

    List<Argument> arguments = new ArrayList<>(3);
    arguments.add(Tele(vars("q", "w"), Nat()));
    arguments.add(Tele(vars("e"), Apps(DataCall(dataType), Index(0), Zero(), Index(1))));
    arguments.add(Tele(vars("r"), Apps(DataCall(dataType), Index(2), Index(1), Suc(Zero()))));
    Expression resultType = Apps(FunCall(pFunction), Index(2), Zero(), Index(3), Index(1), Index(3), Index(2), Suc(Zero()), Index(0));
    List<Clause> clauses2 = new ArrayList<>();
    List<Clause> clauses3 = new ArrayList<>();
    List<Clause> clauses4 = new ArrayList<>();
    ElimExpression term2 = Elim(Index(0) /* r */, clauses2);
    ElimExpression term3 = Elim(Index(1) /* e */, clauses3);
    ElimExpression term4 = Elim(Index(4) /* e */, clauses4);
    clauses2.add(new Clause(match(dataType.getConstructor("con2"), match("x"), match("y"), match("z"), match("t")), Abstract.Definition.Arrow.LEFT, term4, term2));
    clauses2.add(new Clause(match(dataType.getConstructor("con1"), match("s")), Abstract.Definition.Arrow.LEFT, term3, term2));
    clauses3.add(new Clause(match(dataType.getConstructor("con2"), match("x"), match("y"), match("z"), match("t")), Abstract.Definition.Arrow.RIGHT, Index(4), term3));
    clauses3.add(new Clause(match(dataType.getConstructor("con1"), match("s")), Abstract.Definition.Arrow.RIGHT, Index(0), term3));
    clauses4.add(new Clause(match(dataType.getConstructor("con1"), match("s")), Abstract.Definition.Arrow.RIGHT, Apps(Index(3), Index(2)), term4));
    clauses4.add(new Clause(match(dataType.getConstructor("con2"), match("x"), match("y"), match("z"), match("t")), Abstract.Definition.Arrow.RIGHT, Index(7), term4));

    ListErrorReporter errorReporter = new ListErrorReporter();
    FunctionDefinition function = new FunctionDefinition(RootModule.ROOT.getChild(new Name("test")), new Name("fun"), Abstract.Definition.DEFAULT_PRECEDENCE, arguments, resultType, Abstract.Definition.Arrow.LEFT, term2);
    Namespace functionNamespace = function.getStaticNamespace();
    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(functionNamespace.getParent(), errorReporter);
    visitor.setNamespaceMember(functionNamespace.getParent().getMember(function.getName().name));
    FunctionDefinition typedFun = visitor.visitFunction(function, null);
    assertNotNull(typedFun);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedFun.hasErrors());
  }

  @Test
  public void elim2() {
    typeCheckClass(
        "\\static \\data D Nat (x y : Nat) | con1 Nat | con2 (Nat -> Nat) (a b c : Nat)\n" +
        "\\static \\function P (a1 b1 c1 : Nat) (d1 : D a1 b1 c1) (a2 b2 c2 : Nat) (d2 : D a2 b2 c2) : \\Type0 <= \\elim d1\n" +
        "  | con2 _ _ _ _ => Nat -> Nat\n" +
        "  | con1 _ => Nat\n" +
        "\\static \\function test (q w : Nat) (e : D w 0 q) (r : D q w 1) : P w 0 q e q w 1 r <= \\elim r\n" +
        "  | con1 s <= \\elim e\n" +
        "    | con2 x y z t => x\n" +
        "    | con1 _ => s\n" +
        "  ;\n" +
        "  | con2 x y z t <= \\elim e\n" +
        "    | con1 s => x q\n" +
        "    | con2 _ y z t => x");
  }

  @Test
  public void elim3() {
    typeCheckClass(
        "\\static \\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
        "\\static \\function test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat <= \\elim r\n" +
        "  | con1 s <= \\elim e\n" +
        "    | con2 _ {y} {z} {t} => q t\n" +
        "    | con1 {z} _ => z\n" +
        "    ;\n" +
        "  | con2 y <= \\elim e\n" +
        "    | con1 s => y s\n" +
        "    | con2 _ {a} {b} => y (q b)");
  }

  @Test
  public void elim4() {
    typeCheckClass(
        "\\static \\function test (x : Nat) : Nat <= \\elim x | zero => 0 | _ => 1\n" +
        "\\static \\function test2 (x : Nat) : 1 = 1 => path (\\lam _ => test x)", 1);
  }

  @Test
  public void elim5() {
    typeCheckClass(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc n) => d1\n" +
        "\\static \\function test (x : D 0) : Nat => \\elim x | d0 => 0");
  }

  @Test
  public void elimUnknownIndex1() {
    typeCheckClass(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex2() {
    typeCheckClass(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | d0 => 0 | _ => 1", 1);
  }

  @Test
  public void elimUnknownIndex3() {
    typeCheckClass(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | _ => 0", 0);
  }

  @Test
  public void elimUnknownIndex4() {
    typeCheckClass(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex5() {
    typeCheckClass(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1 | d2 => 2", 2);
  }

  @Test
  public void elimUnknownIndex6() {
    typeCheckClass(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1 | _ => 2", 2);
  }

  @Test
  public void elimUnknownIndex7() {
    typeCheckClass(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | _ => 0", 0);
  }

  @Test
  public void elimTooManyArgs() {
    typeCheckClass("\\static \\data A | a Nat Nat \\static \\function test (a : A) : Nat <= \\elim a | a _ _ _ =>0", 1);
  }

  @Test
  public void elim6() {
    typeCheckClass(
        "\\static \\data D | d Nat Nat\n" +
            "\\static \\function test (x : D) : Nat => \\elim x | d zero zero => 0 | d (suc _) _ => 1 | d _ (suc _) => 2");
  }

  @Test
  public void elim7() {
    typeCheckClass(
        "\\static \\data D | d Nat Nat\n" +
        "\\static \\function test (x : D) : Nat => \\elim x | d zero zero => 0 | d (suc (suc _)) zero => 0", 1);
  }

  @Test
  public void elim8() {
    ClassDefinition defs = typeCheckClass(
        "\\static \\data D | d Nat Nat\n" +
        "\\static \\function test (x : D) : Nat <= \\elim x | d zero zero => 0 | d _ _ => 1");
    Namespace namespace = defs.getParentNamespace().findChild(defs.getName().name);
    FunctionDefinition test = (FunctionDefinition) namespace.getDefinition("test");
    Constructor d = (Constructor) namespace.getDefinition("d");
    Expression call1 = Apps(ConCall(d), Zero(), Index(0));
    Expression call2 = Apps(ConCall(d), Suc(Zero()), Index(0));
    assertEquals(Apps(FunCall(test), call1), Apps(FunCall(test), call1).normalize(NormalizeVisitor.Mode.NF));
    assertEquals(Suc(Zero()), Apps(FunCall(test), call2).normalize(NormalizeVisitor.Mode.NF));
  }

  @Test
  public void elim9() {
    typeCheckClass(
        "\\static \\data D Nat | D (suc n) => d1 | D _ => d | D zero => d0\n" +
        "\\static \\function test (n : Nat) (a : D (suc n)) : Nat <= \\elim a | d => 0", 1);
  }

  @Test
  public void elimEmptyBranch() {
    typeCheckClass(
        "\\static \\data D Nat | D (suc n) => dsuc\n" +
        "\\static \\function test (n : Nat) (d : D n) : Nat <= \\elim n, d | zero, _! | suc n, dsuc => 0");
  }

  @Test
  public void elimEmptyBranchError() {
    typeCheckClass(
        "\\static \\data D Nat | D (suc n) => dsuc\n" +
        "\\static \\function test (n : Nat) (d : D n) : Nat <= \\elim n, d | suc n, _! | zero, _! => 0", 1);
  }

  @Test
  public void elimUnderLetError() {
    typeCheckClass("\\static \\function test (n : Nat) : Nat <= \\let x => 0 \\in \\elim n | _! => 0", 1);
  }

  @Test
  public void elimOutOfDefinitionError() {
    typeCheckClass("\\static \\function test (n : Nat) : Nat <= \\let x : Nat <= \\elim n | _ => 0 \\in 1", 1);
  }

  @Test
  public void elimLetError() {
    typeCheckClass("\\static \\function test => \\let x => 0 \\in \\let y : Nat <= \\elim x | _ => 0 \\in 1", 1);
  }

  @Test
  public void testSide() {
    typeCheckClass("\\static \\function test (n : Nat) <= suc (\\elim n | suc n => n | zero => 0)", 1);
  }

  @Test
  public void testNoPatterns() {
    typeCheckClass("\\static \\function test (n : Nat) : 0 = 1 <= \\elim n", 1);
  }

  @Test
  public void testAuto() {
    typeCheckClass(
        "\\static \\data Empty\n" +
        "\\static \\function test (n : Nat) (e : Empty) : Empty <= \\elim n, e");
  }

  @Test
  public void testAuto1() {
    typeCheckClass(
        "\\static \\data Geq Nat Nat | Geq _ zero => Geq-zero | Geq (suc n) (suc m) => Geq-suc (Geq n m)\n" +
        "\\static \\function test (n m : Nat) (p : Geq n m) : Nat <= \\elim n, m, p\n" +
        "  | _, zero, Geq-zero => 0\n" +
        "  | suc n, suc m, Geq-suc p => 1");
  }

  @Test
  public void testAutoNonData() {
    typeCheckClass(
        "\\static \\data D Nat | D zero => dcons\n" +
        "\\static \\data E (n : Nat) (Nat -> Nat) (D n) | econs\n" +
        "\\static \\function test (n : Nat) (d : D n) (e : E n (\\lam x => x) d) : Nat <= \\elim n, d, e\n" +
        "  | zero, dcons, econs => 1");
  }

  @Test
  public void testSmthing() {
    typeCheckClass(
        "\\static \\data Geq (x y : Nat)\n" +
        "  | Geq m zero => EqBase \n" +
        "  | Geq (suc n) (suc m) => EqSuc (p : Geq n m)\n" +
        "\n" +
        "\\static \\function f (x y : Nat) (p : Geq x y) : Nat <=\n" +
        "  \\case x, y, p\n" +
        "    | m, zero, EqBase <= zero \n" +
        "    | zero, suc _, x <= \\elim x ;\n" +
        "    | suc _, suc _, EqSuc q <= suc zero", 3);
  }
}
