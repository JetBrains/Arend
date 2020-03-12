package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.ClassReferable;
import org.arend.term.concrete.Concrete;

import java.util.function.Predicate;

public interface InstanceProvider {
  Concrete.FunctionDefinition findInstance(ClassReferable classRef, Predicate<Concrete.FunctionDefinition> pred);
}
