package org.arend.frontend.repl.jline;

import org.jetbrains.annotations.NotNull;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportCompleter implements Completer {
  private final Supplier<Stream<String>> moduleSupplier;

  public ImportCompleter(@NotNull Supplier<Stream<String>> moduleSupplier) {
    this.moduleSupplier = moduleSupplier;
  }

  @Override
  public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
    if (line.words().size() < 1) return;
    if (!Objects.equals("\\import", line.words().get(0))) return;
    if (line.wordIndex() == 1)
      candidates.addAll(moduleSupplier.get().map(Candidate::new).collect(Collectors.toList()));
  }
}
