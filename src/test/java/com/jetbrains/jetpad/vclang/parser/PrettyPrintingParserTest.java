package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class PrettyPrintingParserTest {
  private void testExpr(Expression expected, Expression expr) throws UnsupportedEncodingException {
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    StringBuilder builder = new StringBuilder();
    expr.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
    Concrete.Expression result = parseExpr(moduleLoader, builder.toString());
    assertEquals(0, moduleLoader.getErrors().size());
    assertTrue(compare(expected, result));
  }

  private void testDef(ModuleLoader moduleLoader, FunctionDefinition expected, FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    builder.append("\\static ");
    def.accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
    FunctionDefinition result = (FunctionDefinition) parseDef(moduleLoader, builder.toString());
    assertEquals(expected.getArguments().size(), result.getArguments().size());
    for (int i = 0; i < expected.getArguments().size(); ++i) {
      assertTrue(compare(((TypeArgument) expected.getArguments().get(i)).getType(), ((TypeArgument) result.getArguments().get(i)).getType()));
    }
    assertTrue(compare(expected.getResultType(), result.getResultType()));
    assertNotNull(result.getTerm());
    assertTrue(compare(expected.getTerm(), result.getTerm()));
    assertEquals(expected.getArrow(), result.getArrow());
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    Expression expected = Apps(Lam(lamArgs(Name("x"), Name("y")), Apps(Var("x"), Apps(Var("x"), Var("y")))), Lam(lamArgs(Name("x"), Name("y")), Var("x")), Apps(Lam("x", Var("x")), Lam("x", Var("x"))));
    Expression expr = Apps(Lam(lamArgs(Name("x"), Name("y")), Apps(Index(1), Apps(Index(1), Index(0)))), Lam(lamArgs(Name("x"), Name("y")), Index(1)), Apps(Lam("x", Index(0)), Lam("x", Index(0))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y : Nat) -> Nat -> Nat -> (x y -> y x) -> Nat x y
    Expression expected = Pi(args(Tele(vars("x", "y"), Nat())), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Var("x"), Var("y")), Apps(Var("y"), Var("x"))), Apps(Nat(), Var("x"), Var("y"))))));
    Expression expr = Pi(args(Tele(vars("x", "y"), Nat())), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Index(1), Index(0)), Apps(Index(0), Index(1))), Apps(Nat(), Index(1), Index(0))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (x : Nat) {y z : Nat} -> Nat -> (t z' : Nat) {x' : Nat -> Nat} -> Nat x' y z' t
    Expression expected = Pi("x", Nat(), Pi(args(Tele(false, vars("y", "z"), Nat())), Pi(Nat(), Pi(args(Tele(vars("t", "z'"), Nat())), Pi(false, "x'", Pi(Nat(), Nat()), Apps(Nat(), Var("x'"), Var("y"), Var("z'"), Var("t")))))));
    Expression expr = Pi("x", Nat(), Pi(args(Tele(false, vars("y", "z"), Nat())), Pi(Nat(), Pi(args(Tele(vars("t", "z'"), Nat())), Pi(false, "x'", Pi(Nat(), Nat()), Apps(Nat(), Index(0), Index(4), Index(1), Index(2)))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
    // f {x : Nat} (A : Nat -> \Type0) : A x -> (Nat -> Nat) -> Nat -> Nat => \t y z. y z;
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);
    FunctionDefinition def = new FunctionDefinition(moduleLoader.getRoot().getChild(new Utils.Name("f")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(Tele(false, vars("x"), Nat()), Tele(vars("A"), Pi(Nat(), Universe(0)))), Pi(Apps(Index(0), Index(1)), Pi(Pi(Nat(), Nat()), Pi(Nat(), Nat()))), Definition.Arrow.RIGHT, Lam(lamArgs(Name("t"), Name("y"), Name("z")), Apps(Index(1), Index(0))));
    testDef(moduleLoader, def, def);
  }

  @Test
  public void prettyPrintingParserElim() throws UnsupportedEncodingException {
    // \function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \elim x | zero => y | suc x' => z (foo z x')
    ModuleLoader moduleLoader = new ModuleLoader();
    moduleLoader.init(DummySourceSupplier.getInstance(), DummyOutputSupplier.getInstance(), false);

    List<Clause> fooClausesActual = new ArrayList<>();
    ElimExpression fooTermActual = Elim(Index(1), fooClausesActual, null);
    FunctionDefinition fooDef = new FunctionDefinition(moduleLoader.getRoot().getChild(new Utils.Name("foo")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(Tele(vars("z"), Pi(Pi(Nat(), Nat()), Nat())), Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, fooTermActual);
    fooClausesActual.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Index(0), fooTermActual));
    fooClausesActual.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Index(2), Apps(DefCall(fooDef), Index(2), Index(1))), fooTermActual));
    moduleLoader.getRoot().addMember(fooDef);

    List<Clause> clausesActual = new ArrayList<>();
    ElimExpression termActual = Elim(Index(1), clausesActual, null);
    FunctionDefinition def = new FunctionDefinition(moduleLoader.getRoot().getChild(new Utils.Name("bar")), Abstract.Definition.DEFAULT_PRECEDENCE, lamArgs(Tele(vars("z"), Pi(Pi(Nat(), Nat()), Nat())), Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, termActual);
    clausesActual.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Index(0), termActual));
    clausesActual.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Index(2), Apps(DefCall(fooDef), Index(2), Index(1))), termActual));

    testDef(moduleLoader, def, def);
  }
}
