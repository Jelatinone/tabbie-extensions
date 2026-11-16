package cat.tabbie.extensions.content;

import static cat.tabbie.sdk.target.Environment.CLIENT;

import java.io.IOException;
import java.util.Set;

import cat.tabbie.extensions.form.Archive;
import cat.tabbie.sdk.content.Artifact;
import cat.tabbie.sdk.meta.action.Install;
import cat.tabbie.sdk.meta.action.Place;
import cat.tabbie.sdk.meta.attribute.Applies;
import cat.tabbie.sdk.meta.registry.Registered;
import cat.tabbie.sdk.revision.Image;
import cat.tabbie.sdk.tree.Node;
import cat.tabbie.sdk.tree.Reason;
import cat.tabbie.sdk.tree.Refusal;
import lombok.NonNull;

/**
 * Resource content, which changes how a game looks and sounds. Each edition
 * places and loads its packs in its own way, so Java and Bedrock packs are
 * separate families. An install makes a pack available to the client; turning
 * it on for a whole client is left to the player using it.
 */
public sealed interface Resourcepack extends Artifact {

  /**
   * Java resource content, placed as one file where a client accepts it.
   * Servers deliver resource packs themselves, so it installs on clients
   * alone.
   */
  @Registered
  @Applies(to = "java:base", environments = CLIENT)
  sealed interface Java extends Resourcepack {

    /**
     * Places Java resource packs in {@code resourcepacks/} at the root, on
     * every Java client.
     *
     * @param context the Cat's root
     * @return the directory accepting Java resource packs
     */
    @Place
    static Node.Directory placeRoot(@NonNull Place.Context context) {
      return context.head().directory("resourcepacks");
    }

    /**
     * Places the pack as one file named after its source, at its head: the
     * directory accepting Java resource packs, on every Java client.
     *
     * @param context the directory accepting Java resource packs
     * @return images placing the pack
     * @throws IOException when the pack's source cannot be described
     */
    @Install
    default Set<Image<?>> install(@NonNull Install.Context context) throws IOException {
      return context.head().receive(source());
    }

    /**
     * A Java resource pack as its provider describes it.
     *
     * @param details the artifact's coordinates, name, source, targets, parity
     *                and relations
     */
    record Default(@NonNull Details details) implements Java {
    }
  }

  /**
   * Bedrock resource content. Its files are available to the whole Cat, in the
   * directory accepting them. Each distribution loads packs in its own way, so
   * a pack installs in one of three:
   * <ul>
   * <li>vanilla Bedrock and Endstone load a pack from a folder, so the pack is
   * unpacked;</li>
   * <li>Nukkit-MOT and Cloudburst Server load a pack from a folder too, but
   * read only a version written as numbers;</li>
   * <li>Cloudburst Nukkit and PowerNukkitX load only archives, so the pack is
   * placed as the archive it is.</li>
   * </ul>
   * None of the last four reads a pack whose {@link Manifest manifest} writes
   * its version as a string, so such a pack is refused there before anything
   * is placed. Vanilla Bedrock and Endstone send a World only the packs its
   * pack list names; that list is kept consistent with the packs present by
   * its own form, not by the install.
   */
  @Registered
  sealed interface Bedrock extends Resourcepack {

    /**
     * Places Bedrock resource packs in {@code resource_packs/} at the root, on
     * every Bedrock distribution.
     *
     * @param context the Cat's root
     * @return the directory accepting Bedrock resource packs
     */
    @Place
    @Applies(to = "bedrock:base")
    static Node.Directory placeRoot(@NonNull Place.Context context) {
      return context.head().directory("resource_packs");
    }

    /**
     * Unpacks the pack into a directory of its own, named by its artifact
     * identity, beneath the directory accepting Bedrock resource packs, on
     * vanilla Bedrock and Endstone. Only the folder holding the pack's
     * manifest is unpacked, so a pack wrapped in a folder of its own unpacks
     * as if it were not.
     *
     * @param context the directory accepting Bedrock resource packs
     * @return images placing the pack's files
     * @throws Refusal     with {@link Reason.Content.Malformed} when the pack
     *                     does not hold exactly one manifest, or its manifest
     *                     declares no identity, or as reading the pack as an
     *                     {@link Archive} refuses
     * @throws IOException when the pack cannot be read
     */
    @Install
    @Applies(to = {"bedrock:vanilla", "bedrock:endstone"})
    default Set<Image<?>> install(@NonNull Install.Context context) throws IOException {
      Manifest manifest = Manifest.in(source().as(Archive::of));
      return context.head().directory(artifactId().id().toString()).receive(manifest.folder());
    }

    /**
     * Unpacks the pack into a directory of its own, named by its artifact
     * identity, beneath the directory accepting Bedrock resource packs, on
     * Nukkit-MOT and Cloudburst Server. The manifest is read first, so a pack
     * the server could not load is refused before anything is placed.
     *
     * @param context the directory accepting Bedrock resource packs
     * @return images placing the pack's files
     * @throws Refusal     with {@link Reason.Content.Malformed} when the pack
     *                     does not hold exactly one manifest, or its manifest
     *                     declares no identity,
     *                     {@link Reason.Content.Unsupported} when the manifest
     *                     writes the pack's version as a string, or as reading
     *                     the pack as an {@link Archive} refuses
     * @throws IOException when the pack cannot be read
     */
    @Install
    @Applies(to = {"bedrock:cloudburst", "bedrock:nukkit-mot"})
    default Set<Image<?>> installUnpacked(@NonNull Install.Context context) throws IOException {
      Manifest manifest = Manifest.in(source().as(Archive::of))
          .requireNumbered();
      return context.head().directory(artifactId().id().toString()).receive(manifest.folder());
    }

    /**
     * Places the pack as one archive, named by its artifact identity with the
     * {@code .mcpack} extension, in the directory accepting Bedrock resource
     * packs, on Cloudburst Nukkit and PowerNukkitX. They read the manifest only
     * at the archive's root or one folder down, and only a version written as
     * numbers, so the manifest is read first and a pack the server could not
     * load is refused before anything is placed.
     *
     * @param context the directory accepting Bedrock resource packs
     * @return images placing the archive
     * @throws Refusal     with {@link Reason.Content.Malformed} when the pack
     *                     does not hold exactly one manifest, or its manifest
     *                     declares no identity,
     *                     {@link Reason.Content.Unsupported} when the manifest
     *                     lies deeper or writes the pack's version as a
     *                     string, or as reading the pack as an {@link Archive}
     *                     refuses
     * @throws IOException when the pack cannot be read
     */
    @Install
    @Applies(to = {"bedrock:cloudburst-nukkit", "bedrock:power-nukkit"})
    default Set<Image<?>> installArchived(@NonNull Install.Context context) throws IOException {
      Manifest.in(source().as(Archive::of))
          .requireDepth(1)
          .requireNumbered();
      return context.head().receive(source(), String.format("%s.mcpack", artifactId().id()));
    }

    /**
     * A Bedrock resource pack as its provider describes it.
     *
     * @param details the artifact's coordinates, name, source, targets, parity
     *                and relations
     */
    record Default(@NonNull Details details) implements Bedrock {
    }
  }
}
