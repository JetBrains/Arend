package com.jetbrains.jetpad.vclang.library;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.library.error.LibraryHeaderError;
import com.jetbrains.jetpad.vclang.library.error.LibraryIOError;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.util.FileUtils;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileLoadableHeaderLibrary extends FileSourceLibrary {
  private final Path myBasePath;
  private static final String DEPS = "dependencies", SOURCE = "sourcesDir", BINARY = "binariesDir", MODULES = "modules";
  private static final String[] Properties = {DEPS, SOURCE, BINARY, MODULES};

  public FileLoadableHeaderLibrary(String name, Path basePath, TypecheckerState typecheckerState) {
    super(name, null, basePath, FileUtils.getModules(basePath, FileUtils.SERIALIZED_EXTENSION), true, Collections.emptyList(), typecheckerState);
    myBasePath = basePath;
  }

  private class Token {
    int line, column;
    String value;
    Token(int line, int column, String value) {
      this.line = line; this.column = column; this.value = value;
    }
  }

  private List<Token> tokenize(BufferedReader reader, ErrorReporter errorReporter) throws IOException {
    int lineInd = 0;
    String line;
    List<Token> tokens = new ArrayList<>();
    while ((line = reader.readLine()) != null) {
      ++lineInd;
      if (line.trim().isEmpty()) {
        continue;
      }
      String[] colonGrp = line.split(":", -1);
      //List<String> fixedColonGrp = new ArrayList<>();
      int columnInd = 1;

      /*
      for (int i = 0; i < colonGrp.length; ) {
        if (i < colonGrp.length - 1 && !colonGrp[i].endsWith(" ") && !colonGrp[i + 1].startsWith(" ")) {
          fixedColonGrp.add(colonGrp[i] + ":" + colonGrp[i + 1]);
          i += 2;
          continue;
        }
        fixedColonGrp.add(colonGrp[i]);
        ++ i;
      } /**/

      for (int i = 0; i < colonGrp.length; ++i) {
        String[] stokens = colonGrp[i].split(" ", -1);
        for (String stoken : stokens) {
          if (stoken.isEmpty()) {
            ++columnInd;
            continue;
          }
          tokens.add(new Token(lineInd, columnInd, stoken));
          columnInd += stoken.length();
        }
        if (i != colonGrp.length - 1) {
          tokens.add(new Token(lineInd, columnInd, ":"));
        }
      }
    }
    return tokens;
  }

  private boolean isValidPropertyName(String name) {
    return Arrays.asList(Properties).contains(name);
  }

  private boolean parseProperty(Token property, List<Token> values, ErrorReporter errorReporter) {
    switch (property.value) {
      case DEPS:
        if (myDependencies != null) {
          errorReporter.report(new LibraryHeaderError(getName(), property.line, property.column, "Dependencies were already specified"));
          return false;
        }
        myDependencies = new ArrayList<>();
        for (Token val : values) {
          if (!FileUtils.isLibraryName(val.value)) {
            errorReporter.report(new LibraryHeaderError(getName(), val.line, val.column, "Invalid library name: " + val.value));
            return false;
          }
          myDependencies.add(new LibraryDependency(val.value));
        }
        break;
      case MODULES:
        if (myModules != null) {
          errorReporter.report(new LibraryHeaderError(getName(), property.line, property.column, "Modules were already specified"));
          return false;
        }
        myModules = new HashSet<>();
        for (Token val : values) {
          ModulePath module = FileUtils.modulePath(val.value);
          if (module == null) {
            errorReporter.report(new LibraryHeaderError(getName(), val.line, val.column, "Invalid module path: " + val.value));
            return false;
          }
          myModules.add(module);
        }
        break;
      case BINARY:
      case SOURCE:
        Path definee = property.value.equals(BINARY) ? myBinaryBasePath : mySourceBasePath;
        if (definee != null) {
          errorReporter.report(new LibraryHeaderError(getName(), property.line, property.column, "The property " + property.value + " was already specified"));
          return false;
        }
        if (values.size() > 1) {
          errorReporter.report(new LibraryHeaderError(getName(), property.line, property.column, "The property " + property.value + " must have exactly one value"));
          return false;
        }
        if (property.value.equals(BINARY)) {
          myBinaryBasePath = Paths.get(values.get(0).value);
          if (!Files.exists(myBinaryBasePath)) {
            myBinaryBasePath = myBasePath.resolve(myBinaryBasePath);
          }
          definee = myBinaryBasePath;
        } else {
          mySourceBasePath = Paths.get(values.get(0).value);
          if (!Files.exists(mySourceBasePath)) {
            mySourceBasePath = myBasePath.resolve(mySourceBasePath);
          }
          definee = mySourceBasePath;
        }
        if (!Files.exists(definee)) {
          errorReporter.report(new LibraryHeaderError(getName(), values.get(0).line, values.get(0).column, "Cannot find directory: " + values.get(0).value));
          mySourceBasePath = null;
          return false;
        }
        break;
    }
    return true;
  }

  @Nullable
  @Override
  protected LibraryHeader loadHeader(ErrorReporter errorReporter) {
    Path headerFile = myBinaryBasePath.resolve(getName() + FileUtils.LIBRARY_EXTENSION);
    try (BufferedReader reader = Files.newBufferedReader(headerFile)) {
      List<Token> tokens = tokenize(reader, errorReporter);
      if (tokens == null) return null;

      if (tokens.isEmpty()) {
        return new LibraryHeader(Collections.emptySet(), Collections.emptyList());
      }

      Iterator<Token> tokenIter = tokens.iterator();
      Token property = null;
      List<Token> values = new ArrayList<>();
      Token prevToken = null;
      Token token = new Token(1,1, "");

      while (tokenIter.hasNext()) {
        token = tokenIter.next();
        if (prevToken == null) {
          prevToken = token;
          continue;
        }
        if (token.value.equals(":")) {
          if (prevToken.value.equals(":") && property == null) {
            errorReporter.report(new LibraryHeaderError(getName(), prevToken.line, prevToken.column, "Name of the property is missing"));
            return null;
          }
          if (values.isEmpty() && property != null) {
            errorReporter.report(new LibraryHeaderError(getName(), token.line, token.column, "Values for the property '" + property.value + "' are missing"));
            return null;
          }
          if (!isValidPropertyName(prevToken.value)) {
            errorReporter.report(new LibraryHeaderError(getName(), prevToken.line, prevToken.column, "The property name '" + prevToken.value + "' is invalid"));
            return null;
          }
          if (property != null) {
            if (!parseProperty(property, values, errorReporter)) {
              return null;
            }
          }
          property = prevToken;
          prevToken = token;
          continue;
        }
        if (property == null) {
          if (isValidPropertyName(prevToken.value)) {
            errorReporter.report(new LibraryHeaderError(getName(), token.line, token.column, "Colon is missing"));
            return null;
          }
          errorReporter.report(new LibraryHeaderError(getName(), prevToken.line, prevToken.column, "The property name is missing"));
          return null;
        }
        if (!prevToken.value.equals(":")) {
          values.add(prevToken);
        }
        prevToken = token;
      }

      if (token.value.equals(":")) {
        errorReporter.report(new LibraryHeaderError(getName(), token.line, token.column, "Values for the property '" + property + "' are missing"));
        return null;
      }

      values.add(token);

      if (!parseProperty(property, values, errorReporter)) {
        return null;
      }

      if (myModules.isEmpty() && mySourceBasePath != null) {
        myModules = FileUtils.getModules(mySourceBasePath, FileUtils.EXTENSION);
      }

    } catch(Exception e) {
      errorReporter.report(new LibraryIOError(headerFile.toString(), "Failed to read header file"));
    }
    return new LibraryHeader(myModules, myDependencies);
  }
}
