package org.arend.frontend.repl.jline;

import org.arend.naming.reference.Referable;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.function.Supplier;

public class ScopeCompleter implements Completer {
  private final @NotNull Supplier<@NotNull List<Referable>> scopeSupplier;

  public ScopeCompleter(@NotNull Supplier<@NotNull List<Referable>> scopeSupplier) {
    this.scopeSupplier = scopeSupplier;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    String word = line.word();
    var firstChar = word.isEmpty() ? '+' : word.charAt(0);
    if ("~!@#$%^&*-+=<>?/|:".indexOf(firstChar) > 0 || Character.isAlphabetic(firstChar)) {
      for (Referable referable : scopeSupplier.get())
        candidates.add(new Candidate(referable.getRefName()));
    }
  }
}
