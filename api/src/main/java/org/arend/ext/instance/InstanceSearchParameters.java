package org.arend.ext.instance;

import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.core.definition.CoreFunctionDefinition;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.typechecking.TypedExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Specifies parameters for instance search.
 */
public interface InstanceSearchParameters {
  /**
   * @return true if local instances should be searched
   */
  default boolean searchLocal() {
    return true;
  }

  /**
   * @return true if global instances should be searched
   */
  default boolean searchGlobal() {
    return true;
  }

  /**
   * Checks if the class of an instances is accepted.
   */
  default boolean testClass(@NotNull CoreClassDefinition classDefinition) {
    return true;
  }

  /**
   * Checks if a global instance is accepted.
   */
  default boolean testGlobalInstance(@NotNull CoreFunctionDefinition instance) {
    return true;
  }

  /**
   * Checks if a concrete instance is accepted.
   */
  default boolean testGlobalInstance(@NotNull ConcreteExpression instance) {
    return true;
  }

  /**
   * Checks if a local instance is accepted.
   */
  default boolean testLocalInstance(@NotNull CoreExpression instance) {
    return true;
  }
}
