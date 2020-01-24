package org.arend.util;

import java.math.BigInteger;
import java.util.Objects;

public class Version implements Comparable<Version> {
  public final BigInteger major;
  public final BigInteger minor;
  public final BigInteger patch;

  public Version(String version) {
    String[] split = version.trim().split("\\.");
    switch (split.length) {
      case 0:
        throw new IllegalArgumentException("Invalid version: " + version);
      case 1:
        major = new BigInteger(split[0]);
        minor = BigInteger.ZERO;
        patch = BigInteger.ZERO;
        break;
      case 2:
        major = new BigInteger(split[0]);
        minor = new BigInteger(split[1]);
        patch = BigInteger.ZERO;
        break;
      default:
        major = new BigInteger(split[0]);
        minor = new BigInteger(split[1]);
        patch = new BigInteger(split[2]);
        break;
    }
  }

  public Version(String major, String minor, String patch) {
    this(new BigInteger(major), new BigInteger(minor), new BigInteger(patch));
  }

  public Version(long major, long minor, long patch) {
    this(BigInteger.valueOf(major), BigInteger.valueOf(minor), BigInteger.valueOf(patch));
  }

  public Version(BigInteger major, BigInteger minor, BigInteger patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  @Override
  public String toString() {
    return BigInteger.ZERO.equals(patch)
        ? BigInteger.ZERO.equals(minor)
        ? major.toString()
        : major + "." + minor
        : major + "." + minor + "." + patch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Version version = (Version) o;
    return Objects.equals(major, version.major) &&
        Objects.equals(minor, version.minor) &&
        Objects.equals(patch, version.patch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch);
  }

  @Override
  public int compareTo(Version o) {
    int i = major.compareTo(o.major);
    if (i != 0) return i;
    int j = minor.compareTo(o.minor);
    return j != 0 ? j : patch.compareTo(o.patch);
  }
}
