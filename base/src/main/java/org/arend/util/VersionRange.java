package org.arend.util;

public class VersionRange extends Range<Version> {
  public VersionRange(Version lowerBound, Version upperBound) {
    super(lowerBound, upperBound);
  }

  public static VersionRange parseVersionRange(String text) {
    Range<String> stringRange = parseRange(text);
    if (stringRange == null) return null;
    Version from = Version.fromString(stringRange.proj1);
    Version to = Version.fromString(stringRange.proj2);
    return from == null && stringRange.proj1 != null || to == null && stringRange.proj2 != null ? null : new VersionRange(from, to);
  }

  @Override
  public boolean inRange(Version version) {
    Version v = new Version(version.major, version.minor, version.patch, "");
    return (proj1 == null || proj1.compareTo(proj1.rest.isEmpty() ? v : version) <= 0) && (proj2 == null || proj2.compareTo(proj2.rest.isEmpty() ? v : version) >= 0);
  }
}
