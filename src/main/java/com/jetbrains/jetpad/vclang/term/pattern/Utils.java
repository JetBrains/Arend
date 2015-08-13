package com.jetbrains.jetpad.vclang.term.pattern;


import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class Utils {
  public static void collectPatternNames(Abstract.Pattern pattern, List<String> names) {
    if (pattern instanceof Abstract.NamePattern) {
      if (((Abstract.NamePattern) pattern).getName() != null)
        names.add(((Abstract.NamePattern) pattern).getName());
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      for (Abstract.Pattern nestedPattern : ((Abstract.ConstructorPattern) pattern).getArguments()) {
        collectPatternNames(nestedPattern, names);
      }
    }
  }

  public static void prettyPrintPattern(Abstract.Pattern pattern, StringBuilder builder, List<String> names, int indent) {
  if (pattern instanceof Abstract.NamePattern) {
    if (((Abstract.NamePattern) pattern).getName() == null) {
      builder.append('_');
    } else {
      builder.append(((Abstract.NamePattern) pattern).getName());
    }
  } else if (pattern instanceof Abstract.ConstructorPattern) {
    builder.append('(');
    builder.append(((Abstract.ConstructorPattern) pattern).getConstructorName());
    builder.append(' ');
    for (Abstract.Pattern p : ((Abstract.ConstructorPattern) pattern).getArguments()) {
      prettyPrintPattern(p, builder, names, indent);
    }
  }
  }
}
