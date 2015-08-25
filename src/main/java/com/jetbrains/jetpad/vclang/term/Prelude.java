package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude {
  public static ClassDefinition PRELUDE;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT;

  public static FunctionDefinition COERCE;

  public static DataDefinition PATH;
  public static FunctionDefinition PATH_INFIX;
  public static Constructor PATH_CON;

  public static FunctionDefinition AT;

  static {
    PRELUDE = new ClassDefinition("Prelude", null);

    NAT = new DataDefinition(new Utils.Name("Nat"), PRELUDE, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), new ArrayList<TypeArgument>());
    ZERO = new Constructor(0, new Utils.Name("zero"), NAT, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    SUC = new Constructor(1, new Utils.Name("suc"), NAT, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), args(TypeArg(DefCall(NAT))));
    NAT.addConstructor(ZERO);
    NAT.addConstructor(SUC);

    PRELUDE.addField(NAT, null);
    PRELUDE.addField(ZERO, null);
    PRELUDE.addField(SUC, null);

    INTERVAL = new DataDefinition(new Utils.Name("I"), PRELUDE, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    LEFT = new Constructor(0, new Utils.Name("left"), INTERVAL, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    RIGHT = new Constructor(1, new Utils.Name("right"), INTERVAL, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    INTERVAL.addConstructor(LEFT);
    INTERVAL.addConstructor(RIGHT);
    INTERVAL.addConstructor(new Constructor(2, new Utils.Name("<abstract>"), INTERVAL, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>()));

    PRELUDE.addField(INTERVAL, null);
    PRELUDE.addField(LEFT, null);
    PRELUDE.addField(RIGHT, null);

    List<Argument> coerceArguments = new ArrayList<>(3);
    coerceArguments.add(Tele(vars("type"), Pi(DefCall(INTERVAL), Universe(Universe.NO_LEVEL))));
    coerceArguments.add(Tele(vars("elem"), Apps(Index(0), DefCall(LEFT))));
    coerceArguments.add(Tele(vars("point"), DefCall(INTERVAL)));
    List<Clause> coerceClauses = new ArrayList<>(1);
    ElimExpression coerceTerm = Elim(Index(0), coerceClauses);
    coerceClauses.add(new Clause(match(LEFT), Abstract.Definition.Arrow.RIGHT, Index(0), coerceTerm));
    COERCE = new FunctionDefinition(new Utils.Name("coe"), PRELUDE, Abstract.Definition.DEFAULT_PRECEDENCE, coerceArguments, Apps(Index(2), Index(0)), Abstract.Definition.Arrow.LEFT, coerceTerm);

    PRELUDE.addField(COERCE, null);

    List<TypeArgument> PathParameters = new ArrayList<>(3);
    PathParameters.add(Tele(vars("A"), Pi(DefCall(INTERVAL), Universe(Universe.NO_LEVEL, Universe.Type.NOT_TRUNCATED))));
    PathParameters.add(TypeArg(Apps(Index(0), DefCall(LEFT))));
    PathParameters.add(TypeArg(Apps(Index(1), DefCall(RIGHT))));
    PATH = new DataDefinition(new Utils.Name("Path"), PRELUDE, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), PathParameters);
    List<TypeArgument> pathArguments = new ArrayList<>(1);
    pathArguments.add(TypeArg(Pi("i", DefCall(INTERVAL), Apps(Index(3), Index(0)))));
    PATH_CON = new Constructor(0, new Utils.Name("path"), PATH, Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), pathArguments);
    PATH.addConstructor(PATH_CON);

    PRELUDE.addField(PATH, null);
    PRELUDE.addField(PATH_CON, null);

    List<Argument> pathInfixArguments = new ArrayList<>(3);
    pathInfixArguments.add(Tele(false, vars("A"), Universe(0)));
    pathInfixArguments.add(Tele(vars("a", "a'"), Index(0)));
    Expression pathInfixTerm = Apps(DefCall(PATH), Lam(lamArgs(Tele(vars("_"), DefCall(INTERVAL))), Index(3)), Index(1), Index(0));
    PATH_INFIX = new FunctionDefinition(new Utils.Name("=", Abstract.Definition.Fixity.INFIX), PRELUDE, new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixArguments, Universe(0), Abstract.Definition.Arrow.RIGHT, pathInfixTerm);

    PRELUDE.addField(PATH_INFIX, null);

    List<Argument> atArguments = new ArrayList<>(5);
    atArguments.add(Tele(false, vars("A"), PathParameters.get(0).getType()));
    atArguments.add(Tele(false, vars("a"), PathParameters.get(1).getType()));
    atArguments.add(Tele(false, vars("a'"), PathParameters.get(2).getType()));
    atArguments.add(Tele(vars("p"), Apps(DefCall(PATH), Index(2), Index(1), Index(0))));
    atArguments.add(Tele(vars("i"), DefCall(INTERVAL)));
    Expression atResultType = Apps(Index(4), Index(0));
    List<Clause> atClauses = new ArrayList<>(2);
    List<Clause> atOtherwiseClauses = new ArrayList<>(1);
    ElimExpression atOtherwiseElim = Elim(Index(1), atOtherwiseClauses);
    atOtherwiseClauses.add(new Clause(match(PATH_CON, match("f")), Abstract.Definition.Arrow.RIGHT, Apps(Index(1), Index(0)), atOtherwiseElim));
    ElimExpression atTerm = Elim(Index(0), atClauses);
    atClauses.add(new Clause(match(LEFT), Abstract.Definition.Arrow.RIGHT, Index(2), atTerm));
    atClauses.add(new Clause(match(RIGHT), Abstract.Definition.Arrow.RIGHT, Index(1), atTerm));
    atClauses.add(new Clause(match(null), Abstract.Definition.Arrow.LEFT, atOtherwiseElim, atTerm));
    AT = new FunctionDefinition(new Utils.Name("@", Abstract.Definition.Fixity.INFIX), PRELUDE, new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atArguments, atResultType, Abstract.Definition.Arrow.LEFT, atTerm);

    PRELUDE.addField(AT, null);
  }
}
