package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude {
  public final static Map<String, Definition> DEFINITIONS = new HashMap<>();

  public final static DataDefinition NAT;
  public final static Constructor ZERO, SUC;

  public final static DataDefinition INTERVAL;
  public final static Constructor LEFT, RIGHT;

  public final static FunctionDefinition COERCE;

  public final static DataDefinition PATH;
  public final static Constructor PATH_CON;

  public final static FunctionDefinition AT;

  static {
    List<Constructor> natConstructors = new ArrayList<>(2);
    NAT = new DataDefinition("Nat", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.SET), new ArrayList<TypeArgument>(), natConstructors);
    ZERO = new Constructor(0, "zero", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), NAT);
    SUC = new Constructor(1, "suc", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.SET), args(TypeArg(DefCall(NAT))), NAT);
    natConstructors.add(ZERO);
    natConstructors.add(SUC);

    DEFINITIONS.put(NAT.getName(), NAT);
    DEFINITIONS.put(ZERO.getName(), ZERO);
    DEFINITIONS.put(SUC.getName(), SUC);

    List<Constructor> intervalConstructors = new ArrayList<>(3);
    INTERVAL = new DataDefinition("I", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), intervalConstructors);
    LEFT = new Constructor(0, "left", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL);
    RIGHT = new Constructor(1, "right", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL);
    intervalConstructors.add(LEFT);
    intervalConstructors.add(RIGHT);
    intervalConstructors.add(new Constructor(2, "<abstract>", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL));

    DEFINITIONS.put(INTERVAL.getName(), INTERVAL);
    DEFINITIONS.put(LEFT.getName(), LEFT);
    DEFINITIONS.put(RIGHT.getName(), RIGHT);

    List<TelescopeArgument> coerceArguments = new ArrayList<>(3);
    coerceArguments.add(Tele(vars("type"), Pi(DefCall(INTERVAL), Universe(Universe.NO_LEVEL))));
    coerceArguments.add(Tele(vars("elem"), Apps(Index(0), DefCall(LEFT))));
    coerceArguments.add(Tele(vars("point"), DefCall(INTERVAL)));
    List<Clause> coerceClauses = new ArrayList<>(1);
    ElimExpression coerceTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), coerceClauses, null);
    coerceClauses.add(new Clause(LEFT, new ArrayList<Argument>(), Abstract.Definition.Arrow.RIGHT, Index(1), coerceTerm));
    COERCE = new FunctionDefinition("coe", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, coerceArguments, Apps(Index(2), Index(0)), Abstract.Definition.Arrow.LEFT, coerceTerm);

    DEFINITIONS.put(COERCE.getName(), COERCE);

    List<TypeArgument> PathParameters = new ArrayList<>(3);
    PathParameters.add(Tele(vars("A"), Pi(DefCall(INTERVAL), Universe(Universe.NO_LEVEL, Universe.Type.NOT_TRUNCATED))));
    PathParameters.add(TypeArg(Apps(Index(0), DefCall(LEFT))));
    PathParameters.add(TypeArg(Apps(Index(1), DefCall(RIGHT))));
    List<Constructor> PathConstructors = new ArrayList<>(1);
    PATH = new DataDefinition("Path", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), PathParameters, PathConstructors);
    List<TypeArgument> pathArguments = new ArrayList<>(1);
    pathArguments.add(TypeArg(Pi("i", DefCall(INTERVAL), Apps(Index(3), Index(0)))));
    PATH_CON = new Constructor(0, "path", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), pathArguments, PATH);
    PathConstructors.add(PATH_CON);

    DEFINITIONS.put(PATH.getName(), PATH);
    DEFINITIONS.put(PATH_CON.getName(), PATH_CON);

    List<TelescopeArgument> atArguments = new ArrayList<>(5);
    atArguments.add(Tele(false, vars("A"), PathParameters.get(0).getType()));
    atArguments.add(Tele(false, vars("a"), PathParameters.get(1).getType()));
    atArguments.add(Tele(false, vars("a'"), PathParameters.get(2).getType()));
    atArguments.add(Tele(vars("p"), Apps(DefCall(PATH), Index(2), Index(1), Index(0))));
    atArguments.add(Tele(vars("i"), DefCall(INTERVAL)));
    Expression atResultType = Apps(Index(4), Index(0));
    List<Clause> atClauses = new ArrayList<>(2);
    List<Clause> atOtherwiseClauses = new ArrayList<>(1);
    ElimExpression atOtherwiseElim = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), atOtherwiseClauses, null);
    atOtherwiseClauses.add(new Clause(PATH_CON, lamArgs(Name("f")), Abstract.Definition.Arrow.RIGHT, Apps(Index(0), Index(1)), atOtherwiseElim));
    Clause atOtherwise = new Clause(null, null, Abstract.Definition.Arrow.LEFT, atOtherwiseElim, null);
    ElimExpression atTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), atClauses, atOtherwise);
    atOtherwise.setElimExpression(atTerm);
    atClauses.add(new Clause(LEFT, new ArrayList<Argument>(), Abstract.Definition.Arrow.RIGHT, Index(2), atTerm));
    atClauses.add(new Clause(RIGHT, new ArrayList<Argument>(), Abstract.Definition.Arrow.RIGHT, Index(1), atTerm));
    AT = new FunctionDefinition("@", new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), Abstract.Definition.Fixity.INFIX, atArguments, atResultType, Abstract.Definition.Arrow.LEFT, atTerm);

    DEFINITIONS.put(AT.getName(), AT);
  }
}
