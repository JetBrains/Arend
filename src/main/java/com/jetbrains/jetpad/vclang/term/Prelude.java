package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude {
  public static Map<String, Definition> DEFINITIONS = new HashMap<>();

  static {
    List<Constructor> natConstructors = new ArrayList<>(2);
    DataDefinition nat = new DataDefinition("Nat", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.SET), new ArrayList<TypeArgument>(), natConstructors);
    natConstructors.add(new Constructor(0, "zero", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), nat));
    natConstructors.add(new Constructor(1, "suc", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.SET), args(TypeArg(DefCall(nat))), nat));

    DEFINITIONS.put(nat.getName(), nat);
    DEFINITIONS.put(natConstructors.get(0).getName(), natConstructors.get(0));
    DEFINITIONS.put(natConstructors.get(1).getName(), natConstructors.get(1));

    List<Constructor> intervalConstructors = new ArrayList<>(3);
    DataDefinition interval = new DataDefinition("I", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), intervalConstructors);
    intervalConstructors.add(new Constructor(0, "left", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), interval));
    intervalConstructors.add(new Constructor(1, "right", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), interval));
    intervalConstructors.add(new Constructor(2, "<abstract>", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), interval));

    DEFINITIONS.put(interval.getName(), interval);
    DEFINITIONS.put(intervalConstructors.get(0).getName(), intervalConstructors.get(0));
    DEFINITIONS.put(intervalConstructors.get(1).getName(), intervalConstructors.get(1));

    List<TelescopeArgument> coerceArguments = new ArrayList<>(3);
    coerceArguments.add(Tele(vars("type"), Pi(DefCall(interval), Universe(Universe.NO_LEVEL))));
    coerceArguments.add(Tele(vars("elem"), Apps(Index(0), DefCall(intervalConstructors.get(0)))));
    coerceArguments.add(Tele(vars("point"), DefCall(interval)));
    List<Clause> coerceClauses = new ArrayList<>(1);
    coerceClauses.add(new Clause(intervalConstructors.get(0), new ArrayList<Argument>(), Abstract.Definition.Arrow.RIGHT, Index(1)));
    FunctionDefinition coerce = new FunctionDefinition("coe", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, coerceArguments, Apps(Index(2), Index(0)), Abstract.Definition.Arrow.LEFT, Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), coerceClauses));

    DEFINITIONS.put(coerce.getName(), coerce);
  }
}
