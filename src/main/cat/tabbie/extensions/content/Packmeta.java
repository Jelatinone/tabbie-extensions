package cat.tabbie.extensions.content;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import cat.tabbie.extensions.form.Json;
import cat.tabbie.sdk.failure.Reason;
import cat.tabbie.sdk.failure.Refusal;
import cat.tabbie.sdk.tree.Shape;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import tools.jackson.databind.JsonNode;

/**
 * What a Java pack, a datapack or a resource pack, declares in its
 * {@code pack.mcmeta}: the pack formats it supports, and its description. The
 * formats are read from the newest fields the file declares:
 * {@code min_format} and {@code max_format}, each a major number or a major
 * and minor pair; otherwise {@code supported_formats}, a number, a
 * {@code [min, max]} pair or a {@code min_inclusive} and {@code max_inclusive}
 * object; otherwise the single {@code pack_format}. A minor number left out is
 * zero. The folder the file lies in holds the pack's files.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Packmeta extends Json {

  /**
   * The file a pack's metadata is named.
   */
  public static final String FILE = "pack.mcmeta";

  Format oldest;

  Format newest;

  JsonNode description;

  private Packmeta(Shape shape, JsonNode tree, Format oldest, Format newest, JsonNode description) {
    super(shape, tree);
    this.oldest = oldest;
    this.newest = newest;
    this.description = description.deepCopy();
  }

  /**
   * Reads a pack's {@code pack.mcmeta}.
   *
   * @param shape the file
   * @return the pack's metadata
   * @throws Refusal     with {@link Reason.Content.Unreadable} when the file
   *                     is not JSON, or {@link Reason.Content.Malformed} when
   *                     it does not declare the formats the pack supports, or
   *                     its newest is older than its oldest
   * @throws IOException when the file cannot be read
   */
  public static Packmeta of(@NonNull Shape shape) throws IOException {
    JsonNode tree = read(shape, Packmeta.class);
    JsonNode pack = tree.path("pack");
    JsonNode description = pack.path("description");
    JsonNode supported = pack.path("supported_formats");
    try {
      Format oldest;
      Format newest;
      if (pack.has("min_format") && pack.has("max_format")) {
        oldest = Format.of(pack.get("min_format"));
        newest = Format.of(pack.get("max_format"));
      } else if (supported.isObject()) {
        oldest = Format.of(supported.path("min_inclusive"));
        newest = Format.of(supported.path("max_inclusive"));
      } else if (supported.isArray()) {
        oldest = Format.of(supported.path(0));
        newest = Format.of(supported.path(1));
      } else {
        oldest = Format.of(supported.isMissingNode() ? pack.path("pack_format") : supported);
        newest = oldest;
      }
      if (newest.compareTo(oldest) < 0) {
        throw new IllegalArgumentException("A pack's newest supported format is no older than its oldest.");
      }
      return new Packmeta(shape, tree, oldest, newest, description);
    } catch (IllegalArgumentException exception) {
      throw new Refusal(new Reason.Content.Malformed(shape, Packmeta.class,
          "It declares the pack formats it supports, the newest no older than the oldest."), exception);
    }
  }

  /**
   * Reads the one {@code pack.mcmeta} a pack holds anywhere beneath a
   * directory, such as an archive.
   *
   * @param pack the directory holding the pack
   * @return the pack's metadata
   * @throws Refusal     with {@link Reason.Content.Malformed} when the pack
   *                     holds none or several, or as {@link #of(Shape)}
   *                     refuses
   * @throws IOException when the pack cannot be read
   */
  public static Packmeta in(@NonNull Shape.Directory pack) throws IOException {
    List<Shape.File> metas = pack.find(FILE);
    if (metas.size() != 1) {
      throw new Refusal(new Reason.Content.Malformed(pack, Packmeta.class,
          "A pack declares exactly one pack.mcmeta."));
    }
    return metas.getFirst().as(Packmeta::of);
  }

  /**
   * Returns the oldest pack format the pack supports.
   *
   * @return the oldest format
   */
  @NonNull
  public Format oldest() {
    return oldest;
  }

  /**
   * Returns the newest pack format the pack supports.
   *
   * @return the newest format, no older than the oldest
   */
  @NonNull
  public Format newest() {
    return newest;
  }

  /**
   * Returns the pack's description as the file writes it.
   *
   * @return a text component, or a missing node when it declares none
   */
  @NonNull
  public JsonNode description() {
    return description.deepCopy();
  }

  /**
   * Returns the folder the file lies in, which holds the pack's files.
   *
   * @return the pack's folder
   */
  @NonNull
  public Shape.Directory folder() {
    return shape.parent().orElseThrow();
  }

  /**
   * Whether the pack supports a pack format.
   *
   * @param format a pack format
   * @return true when it lies between the oldest and newest, inclusive
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
