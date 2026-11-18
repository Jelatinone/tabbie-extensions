package cat.tabbie.extensions.content;

import static cat.tabbie.sdk.meta.action.Closure.NAMESPACES;

import java.io.IOException;
import java.util.Set;

import cat.tabbie.extensions.form.Archive;
import cat.tabbie.sdk.content.Artifact;
import cat.tabbie.sdk.failure.Reason;
import cat.tabbie.sdk.failure.Refusal;
import cat.tabbie.sdk.meta.action.Install;
import cat.tabbie.sdk.meta.action.Place;
import cat.tabbie.sdk.meta.attribute.Applies;
import cat.tabbie.sdk.meta.registry.Registered;
import cat.tabbie.sdk.revision.Image;
import cat.tabbie.sdk.tree.Node;
import lombok.NonNull;

/**
 * Bedrock behaviour content, which changes how a game plays. Each
 * distribution loads packs in its own way, so a pack is placed and installed
 * in one of two:
 * <ul>
 * <li>vanilla Bedrock and Endstone load a behaviour pack from a World, so the
 * pack is placed in every World and unpacked into each;</li>
 * <li>Nukkit-MOT loads every behaviour pack it holds for the whole Cat, from
 * one directory at its root, so the pack is unpacked there alone, once its
 * manifest is found to write its version as numbers.</li>
 * </ul>
 * The other Bedrock servers load none, so no install applies to them. Vanilla
 * Bedrock and Endstone load a World's pack only once its pack list names it;
 * that list is kept consistent with the packs present by its own form, not by
 * the install.
 */
@Registered
public sealed interface Behaviourpack extends Artifact {

  /**
   * Places behaviour packs in each World's {@code behavior_packs/}, on
   * vanilla Bedrock and Endstone. Core calls it once for each World.
   *
   * @param context the head of one World
   * @return the directory accepting behaviour packs in that World
   */
  @Place(NAMESPACES)
  @Applies(to = {"bedrock:vanilla", "bedrock:endstone"})
  static Node.Directory placeNamespaces(@NonNull Place.Context context) {
    return context.head().directory("behavior_packs");
  }

  /**
   * Places behaviour packs in {@code behaviour_packs/} at the root, on
   * Nukkit-MOT, which loads them for the whole Cat.
   *
   * @param context the Cat's root
   * @return the directory accepting behaviour packs
   */
  @Place
  @Applies(to = "bedrock:nukkit-mot")
  static Node.Directory placeRoot(@NonNull Place.Context context) {
    return context.head().directory("behaviour_packs");
  }

  /**
   * Unpacks the pack into a World, into a directory of its own, named by its
   * artifact identity, beneath the World's directory accepting behaviour
   * packs, so two packs never collide, on vanilla Bedrock and Endstone. Core
   * calls it once for each World. Only the folder holding the pack's
   * {@link Manifest manifest} is unpacked, so a pack wrapped in a folder of
   * its own unpacks as if it were not.
   *
   * @param context the directory accepting behaviour packs in one World
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
   * identity, beneath the directory accepting behaviour packs, on Nukkit-MOT.
   * It loads every behaviour pack it holds for the whole Cat, so no World is
   * needed. Only the folder holding the pack's manifest is unpacked, so a pack
   * wrapped in folders of its own unpacks as if it were not. The manifest is
   * read first, so a pack the server could not load is refused before
   * anything is placed.
   *
   * @param context the directory accepting behaviour packs in the Cat
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
  @Applies(to = "bedrock:nukkit-mot")
  default Set<Image<?>> installUnpacked(@NonNull Install.Context context) throws IOException {
    Manifest manifest = Manifest.Numbered.in(source().as(Archive::of));
    return context.head().directory(artifactId().id().toString()).receive(manifest.folder());
  }

  /**
   * A behaviour pack as its provider describes it.
   *
   * @param details the artifact's coordinates, name, source, targets, parity
   *                and relations
   */
  record Default(@NonNull Details details) implements Behaviourpack {
  }
}
