package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    DataDefinition dataType = new DataDefinition("D", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), parameters, constructors);
    constructors.add(new Constructor(0, "con1", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), arguments1, dataType));
    constructors.add(new Constructor(1, "con2", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), arguments2, dataType));

    List<TelescopeArgument> arguments3 = new ArrayList<>(4);
    arguments3.add(Tele(vars("a1", "b1", "c1"), Nat()));
    arguments3.add(Tele(vars("d1"), Apps(DefCall(dataType), Index(2), Index(1), Index(0))));
    arguments3.add(Tele(vars("a2", "b2", "c2"), Nat()));
    arguments3.add(Tele(vars("d2"), Apps(DefCall(dataType), Index(2), Index(1), Index(0))));
    List<Argument> arguments11 = new ArrayList<>(1);
    List<Argument> arguments12 = new ArrayList<>(4);
    arguments11.add(Name("x"));
    arguments12.add(Name("x"));
    arguments12.add(Name("y"));
    arguments12.add(Name("z"));
    arguments12.add(Name("w"));
    List<Clause> clauses1 = new ArrayList<>(2);
    clauses1.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Nat()));
    clauses1.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Pi(Nat(), Nat())));
    Expression pTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(4), clauses1);
    FunctionDefinition pFunction = new FunctionDefinition("P", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, arguments3, Universe(), Abstract.Definition.Arrow.LEFT, pTerm);

    List<TelescopeArgument> arguments = new ArrayList<>(3);
    arguments.add(Tele(vars("q", "w"), Nat()));
    arguments.add(Tele(vars("e"), Apps(DefCall(dataType), Index(0), Zero(), Index(1))));
    arguments.add(Tele(vars("r"), Apps(DefCall(dataType), Index(2), Index(1), Suc(Zero()))));
    Expression resultType = Apps(DefCall(pFunction), Index(2), Zero(), Index(3), Index(1), Index(3), Index(2), Suc(Zero()), Index(0));
    List<Clause> clauses2 = new ArrayList<>();
    List<Clause> clauses3 = new ArrayList<>();
    List<Clause> clauses4 = new ArrayList<>();
    clauses2.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.LEFT, Elim(Abstract.ElimExpression.ElimType.ELIM, Index(5), clauses4)));
    clauses2.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.LEFT, Elim(Abstract.ElimExpression.ElimType.ELIM, Index(2), clauses3)));
    clauses3.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Index(3)));
    clauses3.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Index(0)));
    clauses4.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Apps(Index(4), Index(2))));
    clauses4.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Index(7)));
    Expression term = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), clauses2);
    FunctionDefinition function = new FunctionDefinition("fun", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, arguments, resultType, Abstract.Definition.Arrow.LEFT, term);

    Map<String, Definition> globalContext = new HashMap<>();
    globalContext.put("D", dataType);
    globalContext.put("P", pFunction);
    globalContext.put("con1", constructors.get(0));
    globalContext.put("con2", constructors.get(1));
    List<TypeCheckingError> errors = new ArrayList<>();
    Definition result = function.accept(new DefinitionCheckTypeVisitor(globalContext, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(result);
  }
}
