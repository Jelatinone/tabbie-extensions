package cat.tabbie.extensions.content;

import java.io.IOException;
import java.util.Set;

import cat.tabbie.sdk.content.Artifact;
import cat.tabbie.sdk.content.Install;
import cat.tabbie.sdk.meta.attribute.Applies;
import cat.tabbie.sdk.meta.registry.Registered;
import cat.tabbie.sdk.revision.Image;
import cat.tabbie.sdk.tree.Reason;
import cat.tabbie.sdk.tree.Violation;
import lombok.NonNull;

/**
 * Self-contained pack content owned by one artifact, including every extracted
 * file. Embedded mods are not separate artifacts or external dependencies. A
 * mod loader's layout accepts modpacks at the root of the Cat, where they
 * unpack.
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
   * Unpacks the modpack over the directory of the Cat's model that accepts
   * modpacks, such as the root of a Cat on a mod loader, on vanilla Java, any
   * Java mod loader, and every Bedrock distribution.
   *
   * @param context the work, and where modpacks go in the Cat
   * @return validated immutable images
   * @throws IOException when the modpack cannot be captured or is refused
   * @throws Violation   when the model does not allow a file where the modpack
   *                     puts it, such as {@link Reason.Placement.Unplaced} when
   *                     no directory accepts modpacks
   */
  @Install
  default Set<Image<?>> installRoot(@NonNull Install.Context context) throws IOException {
    return context.place().unpack(source());
  }

  /**
   * A modpack as its provider describes it.
   *
   * @param details the artifact's coordinates, name, source, targets, parity
   *                and relations
   */
  record Default(@NonNull Details details) implements Modpack {

    /**
     * Checks the details are given.
     */
    public Default {
    }
  }
}
