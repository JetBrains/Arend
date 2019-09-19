package org.arend.library;

import org.arend.error.doc.Doc;
import org.arend.error.doc.DocFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.arend.error.doc.DocFactory.*;

public class LibraryConfig {
  private String myName;
  private String mySourcesDirectory;
  private String myBinariesDirectory;
  private List<String> myModules;
  private List<String> myDependencies;
  private String myLangVersion;

  public String getName() {
    return myName;
  }

  public void setName(String name) {
    myName = name;
  }

  public String getSourcesDir() {
    return mySourcesDirectory;
  }

  public void setSourcesDir(String sourcesDirectory) {
    mySourcesDirectory = sourcesDirectory;
  }

  public String getBinariesDir() {
    return myBinariesDirectory;
  }

  public void setBinariesDir(String binariesDirectory) {
    myBinariesDirectory = binariesDirectory;
  }

  public List<String> getModules() {
    return myModules;
  }

  public void setModules(List<String> modules) {
    myModules = modules;
  }

  public List<String> getDependencies() {
    return myDependencies;
  }

  public void setDependencies(List<String> dependencies) {
    myDependencies = dependencies;
  }

  public String getLangVersion() {
    return myLangVersion;
  }

  public void setLangVersion(String langVersion) {
    myLangVersion = langVersion;
  }

  @Override
  public String toString() {
    List<Doc> docs = new ArrayList<>();
    if (myName != null) {
      docs.add(text("name: " + myName));
    }
    if (mySourcesDirectory != null) {
      docs.add(text("sourcesDir: " + mySourcesDirectory));
    }
    if (myBinariesDirectory != null) {
      docs.add(text("binariesDir: " + myBinariesDirectory));
    }
    if (myModules != null) {
      docs.add(hList(text("modules: ["), hSep(text(", "), myModules.stream().map(DocFactory::text).collect(Collectors.toList())), text("]")));
    }
    if (myDependencies != null) {
      docs.add(hList(text("dependencies: ["), hSep(text(", "), myDependencies.stream().map(DocFactory::text).collect(Collectors.toList())), text("]")));
    }
    if (myLangVersion != null) {
      docs.add(text("langVersion: " + myLangVersion));
    }
    return vList(docs).toString();
  }
}
