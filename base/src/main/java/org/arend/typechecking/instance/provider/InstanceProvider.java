package org.arend.typechecking.instance.provider;

import org.arend.term.concrete.Concrete;

import java.util.function.Predicate;

public interface InstanceProvider {
  Concrete.FunctionDefinition findInstance(Predicate<Concrete.FunctionDefinition> pred);
}
