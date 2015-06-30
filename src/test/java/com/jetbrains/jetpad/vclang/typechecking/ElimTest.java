package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
    DataDefinition dataType = new DataDefinition("D", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), parameters, constructors);
    constructors.add(new Constructor(0, "con1", dataType, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), arguments1));
    constructors.add(new Constructor(1, "con2", dataType, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), arguments2));

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
    ElimExpression pTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(4), clauses1, null);
    clauses1.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Nat(), pTerm));
    clauses1.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Pi(Nat(), Nat()), pTerm));
    FunctionDefinition pFunction = new FunctionDefinition("P", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, arguments3, Universe(), Abstract.Definition.Arrow.LEFT, false, pTerm);

    List<Argument> arguments = new ArrayList<>(3);
    arguments.add(Tele(vars("q", "w"), Nat()));
    arguments.add(Tele(vars("e"), Apps(DefCall(dataType), Var("w"), Zero(), Var("q"))));
    arguments.add(Tele(vars("r"), Apps(DefCall(dataType), Var("q"), Var("w"), Suc(Zero()))));
    Expression resultType = Apps(DefCall(pFunction), Var("w"), Zero(), Var("q"), Var("e"), Var("q"), Var("w"), Suc(Zero()), Var("r"));
    List<Clause> clauses2 = new ArrayList<>();
    List<Clause> clauses3 = new ArrayList<>();
    List<Clause> clauses4 = new ArrayList<>();
    ElimExpression term2 = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0) /* r */, clauses2, null);
    ElimExpression term3 = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1) /* e */, clauses3, null);
    ElimExpression term4 = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(4) /* e */, clauses4, null);
    clauses2.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.LEFT, term4, term2));
    clauses2.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.LEFT, term3, term2));
    clauses3.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Var("x"), term3));
    clauses3.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Var("s"), term3));
    clauses4.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Apps(Var("x"), Var("z")), term4));
    clauses4.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Index(7), term4));
    FunctionDefinition function = new FunctionDefinition("fun", null, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, arguments, resultType, Abstract.Definition.Arrow.LEFT, false, term2);

    List<TypeCheckingError> errors = new ArrayList<>();
    function.accept(new DefinitionCheckTypeVisitor(function, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertFalse(function.hasErrors());
  }
}
