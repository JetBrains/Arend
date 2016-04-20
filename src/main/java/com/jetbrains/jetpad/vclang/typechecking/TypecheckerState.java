package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Referable;

import java.util.Map;

public class TypecheckerState {
  public static Definition getTypechecked(Map<Abstract.Definition, Definition> myTypecheckMap, Referable ref) {
    final Definition res;
    if (ref instanceof Definition) {
      res = (Definition) ref;
    } else if (ref instanceof Abstract.Definition) {
      res = myTypecheckMap.get(ref);
    } else {
      // FIXME[referable]
      throw new IllegalStateException();
    }
    return res;
  }

  public static Definition getTypecheckedMember(Map<Abstract.Definition, Definition> myTypecheckMap, Definition definition, String name) {
    com.jetbrains.jetpad.vclang.naming.namespace.Namespace ns = definition.getNamespace();
    Referable resolved = ns.resolveName(name);
    return resolved != null ? getTypechecked(myTypecheckMap, resolved) : null;
  }

  public static Definition getDynamicTypecheckedMember(Map<Abstract.Definition, Definition> myTypecheckMap, ClassDefinition classDefinition, String name) {
    com.jetbrains.jetpad.vclang.naming.namespace.Namespace ns = classDefinition.getNamespace();
    Referable resolved = ns.resolveInstanceName(name);
    return resolved != null ? getTypechecked(myTypecheckMap, resolved) : null;
  }
}
