package cat.tabbie.extensions.content;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import cat.tabbie.sdk.content.Descriptor;
import cat.tabbie.sdk.form.Form;
import cat.tabbie.sdk.plate.Archive;
import cat.tabbie.sdk.platform.Address;
import cat.tabbie.sdk.platform.Describe;
import lombok.NonNull;
import tools.jackson.databind.JsonNode;

/**
 * What a Java pack, a datapack or a resource pack, declares in its
 * {@code pack.mcmeta}: the pack formats it supports, and its description.
 * The formats are read from the newest fields the file declares:
 * {@code min_format} and {@code max_format}, each a major number or a major
 * and minor pair; otherwise {@code supported_formats}, a number, a
 * {@code [min, max]} pair or a {@code min_inclusive} and
 * {@code max_inclusive} object; otherwise the single {@code pack_format}. A
 * minor number left out is zero.
 *
 * @param root        folder the file lies in, where the pack's files are
 * @param oldest      oldest pack format the pack supports
 * @param newest      newest pack format the pack supports, no older than
 *                    the oldest
 * @param description the pack's description as the file writes it, a text
 *                    component, or a missing node when it declares none
 */
public record Packmeta(
    @NonNull Address root,
    @NonNull Format oldest,
    @NonNull Format newest,
    @NonNull JsonNode description) implements Descriptor {

  /**
   * Name of the file a Java pack declares itself in.
   */
  public static final String FILE = "pack.mcmeta";

  /**
   * Copies the description, and checks the formats are in order.
   *
   * @throws IllegalArgumentException when the newest format is older than
   *                                  the oldest
   */
  public Packmeta {
    description = description.deepCopy();
    if (newest.compareTo(oldest) < 0) {
      throw new IllegalArgumentException("A pack's newest supported format is no older than its oldest.");
    }
  }

  /**
   * Reads what a pack's {@code pack.mcmeta} declares.
   *
   * @param meta the pack's {@code pack.mcmeta}, at its path within the
   *             pack's archive or directory
   * @return the declared formats and description
   * @throws Archive.Rejection.Malformed when the file is not JSON, or
   *                                     declares no formats it supports
   * @throws IOException                 when the file cannot be read
   */
  public static Packmeta of(@NonNull Describe meta) throws IOException {
    JsonNode pack;
    try {
      pack = new Form.Json().readTree(meta).path("pack");
    } catch (Form.Unreadable unreadable) {
      throw new Archive.Rejection.Malformed("A pack's pack.mcmeta is not readable JSON.", unreadable);
    }
    try {
      Address root = meta.path().parent().orElse(Address.root());
      if (pack.has("min_format") && pack.has("max_format")) {
        return new Packmeta(root, Format.of(pack.get("min_format")), Format.of(pack.get("max_format")),
            pack.path("description"));
      }
      JsonNode supported = pack.path("supported_formats");
      if (supported.isObject()) {
        return new Packmeta(root, Format.of(supported.path("min_inclusive")),
            Format.of(supported.path("max_inclusive")), pack.path("description"));
      }
      if (supported.isArray()) {
        return new Packmeta(root, Format.of(supported.path(0)), Format.of(supported.path(1)),
            pack.path("description"));
      }
      Format only = Format.of(supported.isMissingNode() ? pack.path("pack_format") : supported);
      return new Packmeta(root, only, only, pack.path("description"));
    } catch (IllegalArgumentException exception) {
      throw new Archive.Rejection.Malformed("A pack's pack.mcmeta declares the formats it supports.", exception);
    }
  }

  /**
   * Finds and reads the one {@code pack.mcmeta} among the entries of a
   * pack's archive, wherever in the archive it lies. The game loads a pack
   * only when the file lies at its archive's root, so a {@link #root() root}
   * other than the root address marks a pack wrapped in a folder of its own.
   *
   * @param entries file entries of the pack's archive
   * @return the declared formats and description, and where the pack lies
   * @throws Archive.Rejection.Malformed when the archive holds no
   *                                     {@code pack.mcmeta} or several, or
   *                                     the file is malformed
   * @throws IOException                 when the file cannot be read
   */
  public static Packmeta in(@NonNull List<Archive.Entry> entries) throws IOException {
    List<Archive.Entry> metas = entries.stream()
        .filter(entry -> entry.path().fileName().equals(Optional.of(FILE)))
        .toList();
    if (metas.size() != 1) {
      throw new Archive.Rejection.Malformed("A pack declares exactly one pack.mcmeta.", null);
    }
    return of(metas.getFirst());
  }

  /**
   * Whether the pack supports a pack format.
   *
   * @param format pack format of a game version
   * @return whether it lies between the oldest and newest formats supported
   */
  public boolean supports(@NonNull Format format) {
    return oldest.compareTo(format) <= 0 && format.compareTo(newest) <= 0;
  }

  /**
   * A pack format: a major number, and a minor number that newer game
   * versions use to tell apart formats within one major.
   *
   * @param major major format number
   * @param minor minor format number, zero when none is given
   */
  public record Format(int major, int minor) implements Comparable<Format> {

    /**
     * Checks the numbers are not negative.
     *
     * @throws IllegalArgumentException when a number is negative
     */
    public Format {
      if (major < 0 || minor < 0) {
        throw new IllegalArgumentException("A pack format's numbers are not negative.");
      }
    }

    /**
     * Reads a pack format as {@code pack.mcmeta} writes it: a major number,
     * or a major and an optional minor number in an array.
     *
     * @param format the format's JSON
     * @return the format, minor zero when none is given
     * @throws IllegalArgumentException when it is neither
     */
    public static Format of(@NonNull JsonNode format) {
      if (format.isIntegralNumber()) {
        return new Format(format.asInt(), 0);
      }
      if (format.isArray() && (format.size() == 1 || format.size() == 2)
          && format.values().stream().allMatch(JsonNode::isIntegralNumber)) {
        return new Format(format.get(0).asInt(), format.size() == 2 ? format.get(1).asInt() : 0);
      }
      throw new IllegalArgumentException("A pack format is a major number, or a major and minor pair.");
    }

    @Override
    public int compareTo(@NonNull Format other) {
      return Comparator.comparingInt(Format::major)
          .thenComparingInt(Format::minor)
          .compare(this, other);
    }
  }
}
