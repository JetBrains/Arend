package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

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

    List<Constructor> natConstructors = new ArrayList<>(2);
    NAT = new DataDefinition("Nat", PRELUDE, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.SET), new ArrayList<TypeArgument>(), natConstructors);
    ZERO = new Constructor(0, "zero", NAT, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    SUC = new Constructor(1, "suc", NAT, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.SET), args(TypeArg(DefCall(NAT))));
    natConstructors.add(ZERO);
    natConstructors.add(SUC);

    PRELUDE.addStaticField(NAT, null);
    PRELUDE.addStaticField(ZERO, null);
    PRELUDE.addStaticField(SUC, null);

    List<Constructor> intervalConstructors = new ArrayList<>(3);
    INTERVAL = new DataDefinition("I", PRELUDE, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), intervalConstructors);
    LEFT = new Constructor(0, "left", INTERVAL, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    RIGHT = new Constructor(1, "right", INTERVAL, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    intervalConstructors.add(LEFT);
    intervalConstructors.add(RIGHT);
    intervalConstructors.add(new Constructor(2, "<abstract>", INTERVAL, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>()));

    PRELUDE.addStaticField(INTERVAL, null);
    PRELUDE.addStaticField(LEFT, null);
    PRELUDE.addStaticField(RIGHT, null);

    List<Argument> coerceArguments = new ArrayList<>(3);
    coerceArguments.add(Tele(vars("type"), Pi(DefCall(INTERVAL), Universe(Universe.NO_LEVEL))));
    coerceArguments.add(Tele(vars("elem"), Apps(Index(0), DefCall(LEFT))));
    coerceArguments.add(Tele(vars("point"), DefCall(INTERVAL)));
    List<Clause> coerceClauses = new ArrayList<>(1);
    ElimExpression coerceTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), coerceClauses, null);
    coerceClauses.add(new Clause(LEFT, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Index(0), coerceTerm));
    COERCE = new FunctionDefinition("coe", PRELUDE, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, coerceArguments, Apps(Index(2), Index(0)), Abstract.Definition.Arrow.LEFT, coerceTerm);

    PRELUDE.addStaticField(COERCE, null);

    List<TypeArgument> PathParameters = new ArrayList<>(3);
    PathParameters.add(Tele(vars("A"), Pi(DefCall(INTERVAL), Universe(Universe.NO_LEVEL, Universe.Type.NOT_TRUNCATED))));
    PathParameters.add(TypeArg(Apps(Index(0), DefCall(LEFT))));
    PathParameters.add(TypeArg(Apps(Index(1), DefCall(RIGHT))));
    List<Constructor> PathConstructors = new ArrayList<>(1);
    PATH = new DataDefinition("Path", PRELUDE, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), PathParameters, PathConstructors);
    List<TypeArgument> pathArguments = new ArrayList<>(1);
    pathArguments.add(TypeArg(Pi("i", DefCall(INTERVAL), Apps(Index(3), Index(0)))));
    PATH_CON = new Constructor(0, "path", PATH, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), pathArguments);
    PathConstructors.add(PATH_CON);

    PRELUDE.addStaticField(PATH, null);
    PRELUDE.addStaticField(PATH_CON, null);

    List<Argument> pathInfixArguments = new ArrayList<>(3);
    pathInfixArguments.add(Tele(false, vars("A"), Universe(0)));
    pathInfixArguments.add(Tele(vars("a", "a'"), Index(0)));
    Expression pathInfixTerm = Apps(DefCall(PATH), Lam(lamArgs(Tele(vars("_"), DefCall(INTERVAL))), Index(3)), Index(1), Index(0));
    PATH_INFIX = new FunctionDefinition("=", PRELUDE, new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), Abstract.Definition.Fixity.INFIX, pathInfixArguments, Universe(0), Abstract.Definition.Arrow.RIGHT, pathInfixTerm);

    PRELUDE.addStaticField(PATH_INFIX, null);

    List<Argument> atArguments = new ArrayList<>(5);
    atArguments.add(Tele(false, vars("A"), PathParameters.get(0).getType()));
    atArguments.add(Tele(false, vars("a"), PathParameters.get(1).getType()));
    atArguments.add(Tele(false, vars("a'"), PathParameters.get(2).getType()));
    atArguments.add(Tele(vars("p"), Apps(DefCall(PATH), Index(2), Index(1), Index(0))));
    atArguments.add(Tele(vars("i"), DefCall(INTERVAL)));
    Expression atResultType = Apps(Index(4), Index(0));
    List<Clause> atClauses = new ArrayList<>(2);
    List<Clause> atOtherwiseClauses = new ArrayList<>(1);
    ElimExpression atOtherwiseElim = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), atOtherwiseClauses, null);
    atOtherwiseClauses.add(new Clause(PATH_CON, nameArgs(Name("f")), Abstract.Definition.Arrow.RIGHT, Apps(Index(1), Index(0)), atOtherwiseElim));
    Clause atOtherwise = new Clause(null, null, Abstract.Definition.Arrow.LEFT, atOtherwiseElim, null);
    ElimExpression atTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), atClauses, atOtherwise);
    atOtherwise.setElimExpression(atTerm);
    atClauses.add(new Clause(LEFT, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Index(2), atTerm));
    atClauses.add(new Clause(RIGHT, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Index(1), atTerm));
    AT = new FunctionDefinition("@", PRELUDE, new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), Abstract.Definition.Fixity.INFIX, atArguments, atResultType, Abstract.Definition.Arrow.LEFT, atTerm);

    PRELUDE.addStaticField(AT, null);
  }
}
