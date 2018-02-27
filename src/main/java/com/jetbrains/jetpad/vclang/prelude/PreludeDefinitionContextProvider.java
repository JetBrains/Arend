package com.jetbrains.jetpad.vclang.prelude;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.serialization.DefinitionContextProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.util.LongName;

import javax.annotation.Nonnull;
import java.util.Collections;

public class PreludeDefinitionContextProvider implements DefinitionContextProvider {
  @Nonnull
  @Override
  public ModulePath getDefinitionModule(GlobalReferable referable) {
    return Prelude.MODULE_PATH;
  }

  @Nonnull
  @Override
  public LongName getDefinitionFullName(GlobalReferable referable) {
    return new LongName(Collections.singletonList(referable.textRepresentation()));
  }
}
