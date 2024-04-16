package org.arend.ext;

import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.dependency.ArendDependencyProvider;
import org.arend.ext.prettifier.ExpressionPrettifier;
import org.arend.ext.serialization.SerializableKeyRegistry;
import org.arend.ext.typechecking.GoalSolver;
import org.arend.ext.typechecking.LevelProver;
import org.arend.ext.typechecking.DefinitionListener;
import org.arend.ext.ui.ArendUI;
import org.arend.ext.variable.VariableRenamerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The main class of the extension should implement this interface.
 */
public interface ArendExtension {
  /**
   * All serializable keys must be registered in this method.
   */
  default void registerKeys(@NotNull SerializableKeyRegistry keyRegistry) {
    keyRegistry.registerAllKeys(this);
  }

  /**
   * Can be used to get access to extensions of libraries on which this library depends.
   *
   * @param dependencies    a map from names of libraries to corresponding extensions
   */
  default void setDependencies(@NotNull Map<String, ArendExtension> dependencies) {}

  /**
   * Can be used to get access to the prelude.
   */
  default void setPrelude(@NotNull ArendPrelude prelude) {}

  /**
   * Can be used to get access to a {@link ConcreteFactory}.
   */
  default void setConcreteFactory(@NotNull ConcreteFactory factory) {}

  /**
   * Can be used to get access to a {@link org.arend.ext.variable.VariableRenamer}.
   */
  default void setVariableRenamerFactory(@NotNull VariableRenamerFactory factory) {}

  /**
   * Can be used to get access to a {@link ArendUI}.
   */
  default void setUI(@NotNull ArendUI ui) {}

  /**
   * Declares meta definitions defined in this extension.
   * This method is invoked first, so it does not have access to the library itself.
   * All definitions must be declared in this method, that is {@code contributor} cannot be stored and invoked later.
   */
  default void declareDefinitions(@NotNull DefinitionContributor contributor) {}

  /**
   * Can be used to get access to definitions.
   */
  default void setDefinitionProvider(@NotNull DefinitionProvider definitionProvider) {}

  /**
   * This method is invoked last and can be used to initialize the extension.
   * It should store all the definition that will be used in the extension.
   * This method can be invoked multiple times if one of the definitions is updated.
   *
   * @param dependencyProvider  provides the access to definitions defined in the library; can be used only inside this method.
   */
  default void load(@NotNull ArendDependencyProvider dependencyProvider) {
    dependencyProvider.load(this);
  }


  /**
   * @return a listener which is invoked after typechecking of a definition is finished.
   */
  default @Nullable DefinitionListener getDefinitionListener() {
    return null;
  }

  /**
   * @return a goal solver that will be used for ordinary goals.
   */
  default @Nullable GoalSolver getGoalSolver() {
    return null;
  }

  /**
   * @return a level prover that will be used to check that types of lemmas and properties are propositions.
   */
  default @Nullable LevelProver getLevelProver() {
    return null;
  }

  /**
   * A literal checker is used to interpret string and numerical literals.
   */
  default @Nullable LiteralTypechecker getLiteralTypechecker() {
    return null;
  }

  default @Nullable ExpressionPrettifier getExpressionPrettifier() {
    return null;
  }
}
