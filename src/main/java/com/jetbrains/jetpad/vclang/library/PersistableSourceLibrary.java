package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.source.PersistableSource;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.LongName;

import javax.annotation.Nullable;

/**
 * Represents a library that can be persisted as well as loaded.
 */
public abstract class PersistableSourceLibrary extends SourceLibrary {
  /**
   * Creates a new {@code PersistableSourceLibrary}
   *
   * @param typecheckerState the underling typechecker state of this library.
   */
  protected PersistableSourceLibrary(TypecheckerState typecheckerState) {
    super(typecheckerState);
  }

  @Nullable
  @Override
  public abstract PersistableSource getBinarySource(ModulePath modulePath);

  /**
   * Gets the module to which a specified definition belongs.
   *
   * @param referable a definition.
   *
   * @return the module of the definition
   *         or null if either the definition does not belong to this library or some error occurred.
   */
  @Nullable
  public abstract ModulePath getDefinitionModule(GlobalReferable referable);

  /**
   * Gets the long name of a definition.
   *
   * @param referable a definition.
   *
   * @return the long name of the definition
   *         or null if either the definition does not belong to this library or some error occurred.
   */
  @Nullable
  public abstract LongName getDefinitionFullName(GlobalReferable referable);
}
