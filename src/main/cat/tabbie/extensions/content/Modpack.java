package cat.tabbie.extensions.content;

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
 * Self-contained pack content owned by one artifact, including every extracted
 * file. Embedded mods are not separate artifacts or external dependencies. A
 * modpack is placed at the root of the Cat, where it unpacks.
 *
 * Client and server variants are published as separate target-specific builds,
 * because every artifact of a build installs on every target it advertises.
 * Manifest formats that download their contents, such as Modrinth's or
 * CurseForge's, need an importer rather than this artifact.
 */
@Registered
@Applies(to = {"java:vanilla", "java:launcher", "bedrock:base"})
public sealed interface Modpack extends Artifact {

  /**
   * Places modpacks at the root, on vanilla Java, any Java mod loader, and
   * every Bedrock distribution.
   *
   * @param context the Cat's root
   * @return the root, accepting modpacks
   */
  @Place
  static Node.Directory placeRoot(@NonNull Place.Context context) {
    return context.head();
  }

  /**
   * Unpacks the modpack over its head: the directory accepting modpacks, such
   * as the root of a Cat on a mod loader, on vanilla Java, any Java mod
   * loader, and every Bedrock distribution.
   *
   * @param context the directory accepting modpacks
   * @return images placing the modpack's files
   * @throws Refusal     as reading the modpack as an {@link Archive} refuses,
   *                     such as with {@link Reason.Content.Unsafe} when it
   *                     holds a path that is not safe to place
   * @throws IOException when the modpack cannot be read
   */
  @Install
  default Set<Image<?>> install(@NonNull Install.Context context) throws IOException {
    return context.head().receive(source().as(Archive::of));
  }

  /**
   * A modpack as its provider describes it.
   *
   * @param details the artifact's coordinates, name, source, targets, parity
   *                and relations
   */
  record Default(@NonNull Details details) implements Modpack {
  }
}
