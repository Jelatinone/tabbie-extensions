package cat.tabbie.extensions.content;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import cat.tabbie.extensions.form.Json;
import cat.tabbie.sdk.failure.Reason;
import cat.tabbie.sdk.failure.Refusal;
import cat.tabbie.sdk.tree.Shape;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import tools.jackson.databind.JsonNode;

/**
 * The header a Bedrock pack declares in its {@code manifest.json}: the
 * identity and version a World lists the pack by. Behaviour packs, resource
 * packs and world templates all carry one, and the folder it lies in holds the
 * pack's files. Its version is three numbers, or a version string in newer
 * manifest formats; a distribution that reads only numbers reads its packs'
 * manifests as {@link Numbered}.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public sealed class Manifest extends Json permits Manifest.Numbered {

  /**
   * The file a pack's manifest is named.
   */
  public static final String FILE = "manifest.json";

  UUID uuid;

  JsonNode version;

  private Manifest(Shape shape, JsonNode tree, UUID uuid, JsonNode version) {
    super(shape, tree);
    this.uuid = uuid;
    this.version = version.deepCopy();
  }

  /**
   * Reads a pack's manifest.
   *
   * @param shape the manifest file
   * @return the manifest
   * @throws Refusal     with {@link Reason.Content.Unreadable} when the file
   *                     is not JSON, or {@link Reason.Content.Malformed} when
   *                     its header does not declare a uuid and a version
   * @throws IOException when the file cannot be read
   */
  public static Manifest of(@NonNull Shape shape) throws IOException {
    JsonNode tree = read(shape, Manifest.class);
    JsonNode header = tree.path("header");
    UUID uuid;
    try {
      uuid = UUID.fromString(header.path("uuid").asString());
    } catch (IllegalArgumentException exception) {
      throw new Refusal(new Reason.Content.Malformed(shape, Manifest.class, "Its header declares the pack's uuid."),
          exception);
    }
    JsonNode version = header.path("version");
    boolean numbers = version.isArray() && version.size() == 3
        && version.values().stream().allMatch(JsonNode::isIntegralNumber);
    if (!numbers && !version.isString()) {
      throw new Refusal(new Reason.Content.Malformed(shape, Manifest.class,
          "Its header declares the pack's version as three numbers or a version string."));
    }
    return new Manifest(shape, tree, uuid, version);
  }

  /**
   * Reads the one manifest a pack holds anywhere beneath a directory, such as
   * an archive.
   *
   * @param pack the directory holding the pack
   * @return the manifest
   * @throws Refusal     with {@link Reason.Content.Malformed} when the pack
   *                     holds no manifest or several, or as {@link #of(Shape)}
   *                     refuses
   * @throws IOException when the pack cannot be read
   */
  public static Manifest in(@NonNull Shape.Directory pack) throws IOException {
    return find(pack, Integer.MAX_VALUE).as(Manifest::of);
  }

  /**
   * Reads the one manifest a pack holds, when it lies at most some folders
   * deep, as distributions that look for it only there need.
   *
   * @param pack  the directory holding the pack
   * @param depth most folders the manifest may lie beneath
   * @return the manifest
   * @throws Refusal     with {@link Reason.Content.Unsupported} when the
   *                     manifest lies deeper, or as {@link #in(Shape.Directory)}
   *                     refuses
   * @throws IOException when the pack cannot be read
   */
  public static Manifest in(@NonNull Shape.Directory pack, int depth) throws IOException {
    return find(pack, depth).as(Manifest::of);
  }

  /**
   * Returns the pack's identity.
   *
   * @return the uuid
   */
  @NonNull
  public UUID uuid() {
    return uuid;
  }

  /**
   * Returns the pack's version as the manifest writes it.
   *
   * @return three numbers, or a version string
   */
  @NonNull
  public JsonNode version() {
    return version.deepCopy();
  }

  /**
   * Returns the folder the manifest lies in, which holds the pack's files.
   *
   * @return the pack's folder
   */
  @NonNull
  public Shape.Directory folder() {
    return shape.parent().orElseThrow();
  }

  /**
   * Finds the one manifest a pack holds, at most some folders deep.
   */
  private static Shape.File find(Shape.Directory pack, int depth) throws IOException {
    List<Shape.File> manifests = pack.find(FILE);
    if (manifests.size() != 1) {
      throw new Refusal(new Reason.Content.Malformed(pack, Manifest.class, "A pack declares exactly one manifest."));
    }
    Shape.File manifest = manifests.getFirst();
    if (manifest.address().components().size() - 1 > depth) {
      throw new Refusal(new Reason.Content.Unsupported(manifest, Manifest.class,
          String.format("The manifest lies more than %d folders deep, where the distribution does not find it.",
              depth)));
    }
    return manifest;
  }

  /**
   * A manifest whose version is three numbers, as distributions that cannot
   * read a version string, such as the Nukkit family's, need. It is only ever
   * made from a manifest that writes its version so.
   */
  public static final class Numbered extends Manifest {

    private Numbered(Manifest manifest) {
      super(manifest.shape(), manifest.tree(), manifest.uuid, manifest.version);
    }

    /**
     * Reads a pack's manifest, when it writes the version as three numbers.
     *
     * @param shape the manifest file
     * @return the manifest
     * @throws Refusal     with {@link Reason.Content.Unsupported} when the
     *                     version is a string, or as {@link Manifest#of(Shape)}
     *                     refuses
     * @throws IOException when the file cannot be read
     */
    public static Numbered of(@NonNull Shape shape) throws IOException {
      Manifest manifest = Manifest.of(shape);
      if (manifest.version.isString()) {
        throw new Refusal(new Reason.Content.Unsupported(shape, Numbered.class,
            "The pack writes its version as a string, which the distribution cannot read."));
      }
      return new Numbered(manifest);
    }

    /**
     * Reads the one manifest a pack holds anywhere beneath a directory, when it
     * writes the version as three numbers.
     *
     * @param pack the directory holding the pack
     * @return the manifest
     * @throws Refusal     as {@link Manifest#in(Shape.Directory)} and
     *                     {@link #of(Shape)} refuse
     * @throws IOException when the pack cannot be read
     */
    public static Numbered in(@NonNull Shape.Directory pack) throws IOException {
      return find(pack, Integer.MAX_VALUE).as(Numbered::of);
    }

    /**
     * Reads the one manifest a pack holds, when it lies at most some folders
     * deep and writes the version as three numbers.
     *
     * @param pack  the directory holding the pack
     * @param depth most folders the manifest may lie beneath
     * @return the manifest
     * @throws Refusal     as {@link Manifest#in(Shape.Directory, int)} and
     *                     {@link #of(Shape)} refuse
     * @throws IOException when the pack cannot be read
     */
    public static Numbered in(@NonNull Shape.Directory pack, int depth) throws IOException {
      return find(pack, depth).as(Numbered::of);
    }
  }
}
