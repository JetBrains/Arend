package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ListErrorReporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ElimTest {

  @Test
  public void elim() {
    List<TypeArgument> parameters = new ArrayList<>(2);
    parameters.add(TypeArg(Nat()));
    parameters.add(Tele(vars("x", "y"), Nat()));
    List<TypeArgument> arguments1 = new ArrayList<>(1);
    List<TypeArgument> arguments2 = new ArrayList<>(2);
    arguments1.add(TypeArg(Nat()));
    arguments2.add(TypeArg(Pi(Nat(), Nat())));
    arguments2.add(Tele(vars("a", "b", "c"), Nat()));
    DataDefinition dataType = new DataDefinition(new Namespace(new Utils.Name("D"), null), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), parameters);
    dataType.addConstructor(new Constructor(0, dataType.getNamespace().getChild(new Utils.Name("con1")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), arguments1, dataType));
    dataType.addConstructor(new Constructor(1, dataType.getNamespace().getChild(new Utils.Name("con2")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), arguments2, dataType));

    List<Argument> arguments3 = new ArrayList<>(4);
    arguments3.add(Tele(vars("a1", "b1", "c1"), Nat()));
    arguments3.add(Tele(vars("d1"), Apps(DefCall(dataType), Index(2), Index(1), Index(0))));
    arguments3.add(Tele(vars("a2", "b2", "c2"), Nat()));
    arguments3.add(Tele(vars("d2"), Apps(DefCall(dataType), Index(2), Index(1), Index(0))));
    List<Clause> clauses1 = new ArrayList<>(2);
    ElimExpression pTerm = Elim(Index(4), clauses1);
    clauses1.add(new Clause(match(dataType.getConstructor("con1"), match("s")), Abstract.Definition.Arrow.RIGHT, Nat(), pTerm));
    clauses1.add(new Clause(match(dataType.getConstructor("con2"), match("x"), match("y"), match("z"), match("t")), Abstract.Definition.Arrow.RIGHT, Pi(Nat(), Nat()), pTerm));
    FunctionDefinition pFunction = new FunctionDefinition(new Namespace(new Utils.Name("P"), null), Abstract.Definition.DEFAULT_PRECEDENCE, arguments3, Universe(), Abstract.Definition.Arrow.LEFT, pTerm);

    List<Argument> arguments = new ArrayList<>(3);
    arguments.add(Tele(vars("q", "w"), Nat()));
    arguments.add(Tele(vars("e"), Apps(DefCall(dataType), Index(0), Zero(), Index(1))));
    arguments.add(Tele(vars("r"), Apps(DefCall(dataType), Index(2), Index(1), Suc(Zero()))));
    Expression resultType = Apps(DefCall(pFunction), Index(2), Zero(), Index(3), Index(1), Index(3), Index(2), Suc(Zero()), Index(0));
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

    RootModule.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    FunctionDefinition function = new FunctionDefinition(RootModule.ROOT.getChild(new Utils.Name("test")).getChild(new Utils.Name("fun")), Abstract.Definition.DEFAULT_PRECEDENCE, arguments, resultType, Abstract.Definition.Arrow.LEFT, term2);
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedFun = TypeChecking.typeCheckFunctionBegin(errorReporter, function.getNamespace().getParent(), null, function, localContext, null);
    assertNotNull(typedFun);
    TypeChecking.typeCheckFunctionEnd(errorReporter, function.getNamespace().getParent(), function.getTerm(), typedFun, localContext, null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertFalse(typedFun.hasErrors());
  }

  @Test
  public void elim2() {
    parseDefs(
        "\\static \\data D Nat (x y : Nat) | con1 Nat | con2 (Nat -> Nat) (a b c : Nat)\n" +
        "\\static \\function P (a1 b1 c1 : Nat) (d1 : D a1 b1 c1) (a2 b2 c2 : Nat) (d2 : D a2 b2 c2) : \\Type0 <= \\elim d1\n" +
            "| con2 _ _ _ _ => Nat -> Nat\n" +
            "| con1 _ => Nat\n" +
        "\\static \\function test (q w : Nat) (e : D w 0 q) (r : D q w 1) : P w 0 q e q w 1 r <= \\elim r\n" +
            "| con1 s <= \\elim e\n" +
              "| con2 x y z t => x\n" +
              "| con1 _ => s\n" +
              ";\n" +
            "| con2 x y z t <= \\elim e\n" +
              "| con1 s => x q\n" +
              "| con2 _ y z t => x");
  }

  @Test
  public void elim3() {
    parseDefs(
        "\\static \\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
            "\\static \\function test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat <= \\elim r\n" +
            "| con1 s <= \\elim e\n" +
            "| con2 _ {y} {z} {t} => q t\n" +
            "| con1 {z} _ => z\n" +
            ";\n" +
            "| con2 y <= \\elim e\n" +
            "| con1 s => y s\n" +
            "| con2 _ {a} {b} => y (q b)");
  }

  @Test
  public void elim4() {
    parseDefs(
        "\\static \\function test (x : Nat) : Nat <= \\elim x | zero => 0 | _ => 1\n" +
        "\\static \\function test2 (x : Nat) : 1 = 1 => path (\\lam _ => test x)", 1);
  }

  @Test
  public void elim5() {
    parseDefs(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc n) => d1\n" +
        "\\static \\function test (x : D 0) : Nat => \\elim x | d0 => 0");
  }

  @Test
  public void elimUnknownIndex1() {
    parseDefs(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex2() {
    parseDefs(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | d0 => 0 | _ => 1", 1);
  }

  @Test
  public void elimUnknownIndex3() {
    parseDefs(
        "\\static \\data D (x : Nat) | D zero => d0 | D (suc _) => d1\n" +
        "\\static \\function test (x : Nat) (y : D x) : Nat <= \\elim y | _ => 0", 0);
  }

  @Test
  public void elimUnknownIndex4() {
    parseDefs(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1", 2);
  }

  @Test
  public void elimUnknownIndex5() {
    parseDefs(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1 | d2 => 2", 2);
  }

  @Test
  public void elimUnknownIndex6() {
    parseDefs(
        "\\static \\data E | A | B | C\n" +
            "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
            "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | d0 => 0 | d1 => 1 | _ => 2", 2);
  }

  @Test
  public void elimUnknownIndex7() {
    parseDefs(
        "\\static \\data E | A | B | C\n" +
        "\\static \\data D (x : E) | D A => d0 | D B => d1 | D _ => d2\n" +
        "\\static \\function test (x : E) (y : D x) : Nat <= \\elim y | _ => 0", 0);
  }

  @Test
  public void elimTooManyArgs() {
    parseDefs("\\static \\data A | a Nat Nat \\static \\function test (a : A) : Nat <= \\elim a | a _ _ _ =>0", 1);
  }

  @Test
  public void elim6() {
    parseDefs(
        "\\static \\data D | d Nat Nat" +
        "\\static \\function test (x : D) : Nat => \\elim x | d zero zero => 0 | d (suc _) _ => 1 | d _ (suc _) => 2");
  }

  @Test
  public void elim7() {
    parseDefs(
        "\\static \\data D | d Nat Nat"+
        "\\static \\function test (x : D) : Nat => \\elim x | d zero zero => 0 | d (suc (suc _)) zero => 0", 1);
  }

  @Test
  public void elim8() {
    ClassDefinition defs = parseDefs(
        "\\static \\data D | d Nat Nat" +
        "\\static \\function test (x : D) : Nat => \\elim x | d zero zero => 0 | d _ _ => 1");
    FunctionDefinition test = (FunctionDefinition) defs.getNamespace().getMember("test");
    Constructor d = (Constructor) defs.getNamespace().getMember("d");
    Expression call1 = Apps(DefCall(d), Zero(), Index(0));
    Expression call2 = Apps(DefCall(d), Suc(Zero()), Index(0));
    assertEquals(Apps(DefCall(test), call1), Apps(DefCall(test), call1).normalize(NormalizeVisitor.Mode.NF));
    assertEquals(Suc(Zero()), Apps(DefCall(test), call2).normalize(NormalizeVisitor.Mode.NF));
  }
}
