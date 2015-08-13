package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude {
  public static Namespace PRELUDE;

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
    PRELUDE = new Namespace(new Utils.Name("Prelude"), null);

    NAT = new DataDefinition(PRELUDE.getChild(new Utils.Name("Nat")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), new ArrayList<TypeArgument>());
    ZERO = new Constructor(0, NAT.getNamespace().getChild(new Utils.Name("zero")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), NAT);
    SUC = new Constructor(1, NAT.getNamespace().getChild(new Utils.Name("suc")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.SET), args(TypeArg(DefCall(NAT))), NAT);
    NAT.addConstructor(ZERO);
    NAT.addConstructor(SUC);

    PRELUDE.addMember(NAT);
    PRELUDE.addMember(ZERO);
    PRELUDE.addMember(SUC);

    INTERVAL = new DataDefinition(PRELUDE.getChild(new Utils.Name("I")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>());
    LEFT = new Constructor(0, INTERVAL.getNamespace().getChild(new Utils.Name("left")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL);
    RIGHT = new Constructor(1, INTERVAL.getNamespace().getChild(new Utils.Name("right")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL);
    INTERVAL.addConstructor(LEFT);
    INTERVAL.addConstructor(RIGHT);
    INTERVAL.addConstructor(new Constructor(2, INTERVAL.getNamespace().getChild(new Utils.Name("<abstract>")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), INTERVAL));

    PRELUDE.addMember(INTERVAL);
    PRELUDE.addMember(LEFT);
    PRELUDE.addMember(RIGHT);

    List<Argument> coerceArguments = new ArrayList<>(3);
    coerceArguments.add(Tele(vars("type"), Pi(DefCall(INTERVAL), Universe(Universe.NO_LEVEL))));
    coerceArguments.add(Tele(vars("elem"), Apps(Index(0), DefCall(LEFT))));
    coerceArguments.add(Tele(vars("point"), DefCall(INTERVAL)));
    List<Clause> coerceClauses = new ArrayList<>(1);
    ElimExpression coerceTerm = Elim(Index(0), coerceClauses, null);
    coerceClauses.add(new Clause(LEFT, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Index(0), coerceTerm));
    COERCE = new FunctionDefinition(PRELUDE.getChild(new Utils.Name("coe")), Abstract.Definition.DEFAULT_PRECEDENCE, coerceArguments, Apps(Index(2), Index(0)), Abstract.Definition.Arrow.LEFT, coerceTerm);

    PRELUDE.addMember(COERCE);

    List<TypeArgument> PathParameters = new ArrayList<>(3);
    PathParameters.add(Tele(vars("A"), Pi(DefCall(INTERVAL), Universe(Universe.NO_LEVEL, Universe.Type.NOT_TRUNCATED))));
    PathParameters.add(TypeArg(Apps(Index(0), DefCall(LEFT))));
    PathParameters.add(TypeArg(Apps(Index(1), DefCall(RIGHT))));
    PATH = new DataDefinition(PRELUDE.getChild(new Utils.Name("Path")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), PathParameters);
    List<TypeArgument> pathArguments = new ArrayList<>(1);
    pathArguments.add(TypeArg(Pi("i", DefCall(INTERVAL), Apps(Index(3), Index(0)))));
    PATH_CON = new Constructor(0, PATH.getNamespace().getChild(new Utils.Name("path")), Abstract.Definition.DEFAULT_PRECEDENCE, new Universe.Type(0, Universe.Type.NOT_TRUNCATED), pathArguments, PATH);
    PATH.addConstructor(PATH_CON);

    PRELUDE.addMember(PATH);
    PRELUDE.addMember(PATH_CON);

    List<Argument> pathInfixArguments = new ArrayList<>(3);
    pathInfixArguments.add(Tele(false, vars("A"), Universe(0)));
    pathInfixArguments.add(Tele(vars("a", "a'"), Index(0)));
    Expression pathInfixTerm = Apps(DefCall(PATH), Lam(lamArgs(Tele(vars("_"), DefCall(INTERVAL))), Index(3)), Index(1), Index(0));
    PATH_INFIX = new FunctionDefinition(PRELUDE.getChild(new Utils.Name("=", Abstract.Definition.Fixity.INFIX)), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.NON_ASSOC, (byte) 0), pathInfixArguments, Universe(0), Abstract.Definition.Arrow.RIGHT, pathInfixTerm);

    PRELUDE.addMember(PATH_INFIX);

    List<Argument> atArguments = new ArrayList<>(5);
    atArguments.add(Tele(false, vars("A"), PathParameters.get(0).getType()));
    atArguments.add(Tele(false, vars("a"), PathParameters.get(1).getType()));
    atArguments.add(Tele(false, vars("a'"), PathParameters.get(2).getType()));
    atArguments.add(Tele(vars("p"), Apps(DefCall(PATH), Index(2), Index(1), Index(0))));
    atArguments.add(Tele(vars("i"), DefCall(INTERVAL)));
    Expression atResultType = Apps(Index(4), Index(0));
    List<Clause> atClauses = new ArrayList<>(2);
    List<Clause> atOtherwiseClauses = new ArrayList<>(1);
    ElimExpression atOtherwiseElim = Elim(Index(1), atOtherwiseClauses, null);
    atOtherwiseClauses.add(new Clause(PATH_CON, nameArgs(Name("f")), Abstract.Definition.Arrow.RIGHT, Apps(Index(1), Index(0)), atOtherwiseElim));
    Clause atOtherwise = new Clause(null, null, Abstract.Definition.Arrow.LEFT, atOtherwiseElim, null);
    ElimExpression atTerm = Elim(Index(0), atClauses, atOtherwise);
    atOtherwise.setElimExpression(atTerm);
    atClauses.add(new Clause(LEFT, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Index(2), atTerm));
    atClauses.add(new Clause(RIGHT, new ArrayList<NameArgument>(), Abstract.Definition.Arrow.RIGHT, Index(1), atTerm));
    AT = new FunctionDefinition(PRELUDE.getChild(new Utils.Name("@", Abstract.Definition.Fixity.INFIX)), new Abstract.Definition.Precedence(Abstract.Definition.Associativity.LEFT_ASSOC, (byte) 9), atArguments, atResultType, Abstract.Definition.Arrow.LEFT, atTerm);

    PRELUDE.addMember(AT);
  }
}
