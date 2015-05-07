package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Prelude {
  public static Map<String, Definition> DEFINITIONS = new HashMap<>();

  static {
    List<Constructor> constructors = new ArrayList<>(2);
    DataDefinition nat = new DataDefinition("Nat", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.SET), new ArrayList<TypeArgument>(), constructors);
    constructors.add(new Constructor(0, "zero", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.PROP), new ArrayList<TypeArgument>(), nat));
    constructors.add(new Constructor(1, "suc", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0, Universe.Type.SET), args(TypeArg(DefCall(nat))), nat));

    DEFINITIONS.put("Nat", nat);
    DEFINITIONS.put(constructors.get(0).getName(), constructors.get(0));
    DEFINITIONS.put(constructors.get(1).getName(), constructors.get(1));
  }
}
