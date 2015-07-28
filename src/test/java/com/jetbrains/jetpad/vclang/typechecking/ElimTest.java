package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
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
    List<Constructor> constructors = new ArrayList<>(2);
    List<TypeArgument> arguments1 = new ArrayList<>(1);
    List<TypeArgument> arguments2 = new ArrayList<>(2);
    arguments1.add(TypeArg(Nat()));
    arguments2.add(TypeArg(Pi(Nat(), Nat())));
    arguments2.add(Tele(vars("a", "b", "c"), Nat()));
    DataDefinition dataType = new DataDefinition(new Utils.Name("D"), null, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), parameters, constructors);
    constructors.add(new Constructor(0, new Utils.Name("con1"), dataType, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), arguments1));
    constructors.add(new Constructor(1, new Utils.Name("con2"), dataType, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(Universe.NO_LEVEL), arguments2));

    List<Argument> arguments3 = new ArrayList<>(4);
    arguments3.add(Tele(vars("a1", "b1", "c1"), Nat()));
    arguments3.add(Tele(vars("d1"), Apps(DefCall(dataType), Index(2), Index(1), Index(0))));
    arguments3.add(Tele(vars("a2", "b2", "c2"), Nat()));
    arguments3.add(Tele(vars("d2"), Apps(DefCall(dataType), Index(2), Index(1), Index(0))));
    List<NameArgument> arguments11 = new ArrayList<>(1);
    List<NameArgument> arguments12 = new ArrayList<>(4);
    arguments11.add(Name("s"));
    arguments12.add(Name("x"));
    arguments12.add(Name("y"));
    arguments12.add(Name("z"));
    arguments12.add(Name("t"));
    List<Clause> clauses1 = new ArrayList<>(2);
    ElimExpression pTerm = Elim(Index(4), clauses1, null);
    clauses1.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Nat(), pTerm));
    clauses1.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Pi(Nat(), Nat()), pTerm));
    FunctionDefinition pFunction = new FunctionDefinition(new Utils.Name("P"), null, Abstract.Definition.DEFAULT_PRECEDENCE, arguments3, Universe(), Abstract.Definition.Arrow.LEFT, pTerm);

    List<Argument> arguments = new ArrayList<>(3);
    arguments.add(Tele(vars("q", "w"), Nat()));
    arguments.add(Tele(vars("e"), Apps(DefCall(dataType), Index(0), Zero(), Index(1))));
    arguments.add(Tele(vars("r"), Apps(DefCall(dataType), Index(2), Index(1), Suc(Zero()))));
    Expression resultType = Apps(DefCall(pFunction), Index(2), Zero(), Index(3), Index(1), Index(3), Index(2), Suc(Zero()), Index(0));
    List<Clause> clauses2 = new ArrayList<>();
    List<Clause> clauses3 = new ArrayList<>();
    List<Clause> clauses4 = new ArrayList<>();
    ElimExpression term2 = Elim(Index(0) /* r */, clauses2, null);
    ElimExpression term3 = Elim(Index(1) /* e */, clauses3, null);
    ElimExpression term4 = Elim(Index(4) /* e */, clauses4, null);
    clauses2.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.LEFT, term4, term2));
    clauses2.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.LEFT, term3, term2));
    clauses3.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Index(4), term3));
    clauses3.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Index(0), term3));
    clauses4.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Apps(Index(3), Index(2)), term4));
    clauses4.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Index(7), term4));

    ModuleLoader moduleLoader = new ModuleLoader();
    FunctionDefinition function = new FunctionDefinition(new Utils.Name("fun"), new ClassDefinition("test", moduleLoader.rootModule()), Abstract.Definition.DEFAULT_PRECEDENCE, arguments, resultType, Abstract.Definition.Arrow.LEFT, term2);
    List<Binding> localContext = new ArrayList<>();
    FunctionDefinition typedFun = TypeChecking.typeCheckFunctionBegin(moduleLoader, (ClassDefinition) function.getParent(), function, localContext, null);
    assertNotNull(typedFun);
    TypeChecking.typeCheckFunctionEnd(moduleLoader, (ClassDefinition) function.getParent(), function.getTerm(), typedFun, localContext, null, false);
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(0, moduleLoader.getErrors().size());
    assertFalse(typedFun.hasErrors());
  }

  @Test
  public void elim2() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader,
        "\\data D Nat (x y : Nat) | con1 Nat | con2 (Nat -> Nat) (a b c : Nat)\n" +
        "\\function P (a1 b1 c1 : Nat) (d1 : D a1 b1 c1) (a2 b2 c2 : Nat) (d2 : D a2 b2 c2) : \\Type0 <= \\elim d1\n" +
            "| con2 _ _ _ _ => Nat -> Nat\n" +
            "| con1 _ => Nat\n" +
        "\\function test (q w : Nat) (e : D w 0 q) (r : D q w 1) : P w 0 q e q w 1 r <= \\elim r\n" +
            "| con1 s <= \\elim e\n" +
              "| con2 x y z t => x" +
              "| con1 _ => s" +
              ";\n" +
            "| con2 x y z t <= \\elim e\n" +
              "| con1 s => x q" +
              "| con2 _ y z t => x");
  }

  @Test
  public void elim3() {
    ModuleLoader moduleLoader = new ModuleLoader();
    parseDefs(moduleLoader,
        "\\data D (x : Nat -> Nat) (y : Nat) | con1 {Nat} Nat | con2 (Nat -> Nat) {a b c : Nat}\n" +
        "\\function test (q : Nat -> Nat) (e : D q 0) (r : D (\\lam x => x) (q 1)) : Nat <= \\elim r\n" +
          "| con1 s <= \\elim e\n" +
            "| con2 _ {y} {z} {t} => q t" +
            "| con1 {z} _ => z" +
            ";\n" +
          "| con2 y <= \\elim e\n" +
            "| con1 s => y s" +
            "| con2 _ {a} {b} => y (q b)");
  }
}
