package cat.tabbie.extensions.content;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import cat.tabbie.sdk.content.Descriptor;
import cat.tabbie.sdk.form.Form;
import cat.tabbie.sdk.plate.Archive;
import cat.tabbie.sdk.platform.Address;
import cat.tabbie.sdk.platform.Describe;
import cat.tabbie.sdk.revision.Chunk;
import lombok.NonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The header a Bedrock pack declares in its {@code manifest.json}: the
 * identity and version a World lists the pack by. Behaviour packs, resource
 * packs and world templates all carry one.
 *
 * @param root    folder the manifest lies in, where the pack's files are
 * @param uuid    pack identity
 * @param version pack version as the manifest writes it: three numbers, or
 *                a version string in newer manifest formats
 */
public record Manifest(@NonNull Address root, @NonNull UUID uuid, @NonNull JsonNode version) implements Descriptor {

  /**
   * Name of the file a pack declares its manifest in.
   */
  public static final String FILE = "manifest.json";

  /**
   * Copies the version, and checks it is three numbers or a version string.
   *
   * @throws IllegalArgumentException when the version is neither
   */
  public Manifest {
    version = version.deepCopy();
    boolean numbers = version.isArray() && version.size() == 3
        && version.values().stream().allMatch(JsonNode::isIntegralNumber);
    if (!numbers && !version.isString()) {
      throw new IllegalArgumentException("A pack's version is three numbers or a version string.");
    }
  }

  /**
   * Reads the header a pack's manifest declares.
   *
   * @param manifest the pack's manifest, at its path within the pack's
   *                 archive or directory
   * @return the declared identity and version
   * @throws Archive.Rejection.Malformed when the manifest is not JSON, or
   *                                     declares no uuid and version in its
   *                                     header
   * @throws IOException                 when the manifest cannot be read
   */
  public static Manifest of(@NonNull Describe manifest) throws IOException {
    JsonNode header;
    try {
      header = new Form.Json().readTree(manifest).path("header");
    } catch (Form.Unreadable unreadable) {
      throw new Archive.Rejection.Malformed("A pack's manifest is not readable JSON.", unreadable);
    }
    try {
      return new Manifest(manifest.path().parent().orElse(Address.root()),
          UUID.fromString(header.path("uuid").asString()), header.path("version"));
    } catch (IllegalArgumentException exception) {
      throw new Archive.Rejection.Malformed("A pack's manifest declares its uuid and version.", exception);
    }
  }

  /**
   * Finds and reads the one manifest among the entries of a pack's archive,
   * wherever in the archive it lies.
   *
   * @param entries file entries of the pack's archive
   * @return the declared identity and version, and where the pack lies
   * @throws Archive.Rejection.Malformed when the archive holds no manifest
   *                                     or several, as a bundle of packs
   *                                     does, or the manifest is malformed
   * @throws IOException                 when the manifest cannot be read
   */
  public static Manifest in(@NonNull List<Archive.Entry> entries) throws IOException {
    List<Archive.Entry> manifests = entries.stream()
        .filter(entry -> entry.path().fileName().equals(Optional.of(FILE)))
        .toList();
    if (manifests.size() != 1) {
      throw new Archive.Rejection.Malformed("A pack declares exactly one manifest.", null);
    }
    return of(manifests.getFirst());
  }

  /**
   * Checks the pack writes its version as three numbers, as a distribution
   * that reads no version string needs, such as every server of the Nukkit
   * family. Run before anything is placed, so a pack the distribution could
   * not load is never installed.
   *
   * @return this manifest
   * @throws Archive.Rejection.Unsupported when the version is a string, as
   *                                       manifest format 3 writes it
   */
  public Manifest requireNumbered() throws Archive.Rejection.Unsupported {
    if (version.isString()) {
      throw new Archive.Rejection.Unsupported(String.format("The manifest at %s writes the pack's version as a "
          + "string, which the distribution cannot read.", root.resolve(FILE).canonical()));
    }
    return this;
  }

  /**
   * Checks the manifest lies at most some folders deep in the pack, as a
   * distribution loading the pack's archive as it is needs. Run before
   * anything is placed, so a pack the distribution could not load is never
   * installed.
   *
   * @param depth most folders the manifest may lie in, zero for the pack's
   *              root
   * @return this manifest
   * @throws Archive.Rejection.Unsupported when it lies deeper
   */
  public Manifest requireDepth(int depth) throws Archive.Rejection.Unsupported {
    if (root.components().size() > depth) {
      throw new Archive.Rejection.Unsupported(String.format("The manifest at %s lies more than %d folders deep, "
          + "where the distribution does not find it.", root.resolve(FILE).canonical(), depth));
    }
    return this;
  }

  /**
   * Describes appending this pack's entry to a World's list of the packs it
   * loads, as the list's JSON form reads it. Its inverse removes the entry
   * wherever it is.
   *
   * @return chunk anchored at the end
   */
  @NonNull
  public Chunk listed() {
    Form.Json form = new Form.Json();
    ObjectNode entry = form.mapper().createObjectNode()
        .put("pack_id", uuid.toString())
        .set("version", version);
    try {
      return Chunk.append(Chunk.linesOf(form.decode(
          form.writeValue("packs.json", form.mapper().createArrayNode().add(entry)))));
    } catch (IOException exception) {
      throw new IllegalStateException("A list just written reads back.", exception);
    }
  }
}
