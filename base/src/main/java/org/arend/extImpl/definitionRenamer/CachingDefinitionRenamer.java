package org.arend.extImpl.definitionRenamer;

import org.arend.ext.module.LongReference;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CachingDefinitionRenamer implements DefinitionRenamer {
  private final DefinitionRenamer myRenamer;
  private final Map<ArendRef, @NotNull LongReference> myCache = new HashMap<>();

  public CachingDefinitionRenamer(DefinitionRenamer renamer) {
    myRenamer = renamer;
  }

  @Override
  public @Nullable LongReference renameDefinition(ArendRef ref) {
    LongReference result = myCache.computeIfAbsent(ref, ref1 -> {
      LongReference longReference = myRenamer.renameDefinition(ref1);
      return longReference == null ? LongReference.EMPTY : longReference;
    });
    return result.size() == 0 ? null : result;
  }
}
