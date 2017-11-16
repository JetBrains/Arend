package com.jetbrains.jetpad.vclang.module.caching;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Precedence;

import java.util.List;

public interface CacheDefinitionRegistry {
  GlobalReferable registerDefinition(ModulePath module,  List<String> path, Precedence precedence, GlobalReferable parent);
}
