package org.arend.util;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public class Version implements Comparable<Version> {
  public final BigInteger major;
  public final BigInteger minor;
  public final BigInteger patch;
  public final String rest;

  public static Version fromString(String version) {
    if (version == null) return null;
    String[] split = version.trim().split("\\.");
    try {
      switch (split.length) {
        case 0: throw new IllegalArgumentException("Invalid version: " + version);
        case 1: return new Version(new BigInteger(split[0]), BigInteger.ZERO, BigInteger.ZERO, "");
        case 2: return new Version(new BigInteger(split[0]), new BigInteger(split[1]), BigInteger.ZERO, "");
        default: return new Version(new BigInteger(split[0]), new BigInteger(split[1]), new BigInteger(split[2]), String.join(".", Arrays.asList(split).subList(3, split.length)));
      }
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  public Version(BigInteger major, BigInteger minor, BigInteger patch, String rest) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.rest = rest;
  }

  public String getLongString() {
    return major + "." + minor + "." + patch + (rest.isEmpty() ? "" : "." + rest);
  }

  @Override
  public String toString() {
    return rest.isEmpty() && BigInteger.ZERO.equals(patch) ? (BigInteger.ZERO.equals(minor) ? major.toString() : major + "." + minor) : getLongString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Version version = (Version) o;
    return Objects.equals(major, version.major) &&
        Objects.equals(minor, version.minor) &&
        Objects.equals(patch, version.patch) &&
        Objects.equals(rest, version.rest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, rest);
  }

  @Override
  public int compareTo(Version o) {
    int i = major.compareTo(o.major);
    if (i != 0) return i;
    int j = minor.compareTo(o.minor);
    if (j != 0) return j;
    int k = patch.compareTo(o.patch);
    return k != 0 ? k : rest.compareTo(o.rest);
  }
}
