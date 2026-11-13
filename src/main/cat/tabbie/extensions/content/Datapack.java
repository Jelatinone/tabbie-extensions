package cat.tabbie.extensions.content;

import static cat.tabbie.sdk.meta.action.Closure.NAMESPACES;

import java.io.IOException;
import java.util.Set;

import cat.tabbie.sdk.content.Artifact;
import cat.tabbie.sdk.content.Install;
import cat.tabbie.sdk.meta.attribute.Applies;
import cat.tabbie.sdk.meta.registry.Registered;
import cat.tabbie.sdk.revision.Image;
import cat.tabbie.sdk.tree.Violation;
import lombok.NonNull;

/**
 * Java world data content, which belongs to a World and installs into the
 * Worlds its context names. Constructors do not inspect pack metadata or
 * discover world directories.
 */
@Registered
@Applies(to = "java:base")
public sealed interface Datapack extends Artifact {

  /**
   * Places the datapack as one file named after its source in each World of
   * the context, in the World's directory accepting datapacks, on every Java
   * distribution. The game loads a zipped datapack as it is, and unpacking
   * one wrapped in a folder of its own would bury its {@code pack.mcmeta} too
   * deep to load.
   *
   * @param context the work, and where datapacks go in every World
   * @return validated immutable images
   * @throws IOException when the datapack cannot be captured
   * @throws Violation   when the model does not allow the file in a World
   */
  @Install(NAMESPACES)
  default Set<Image<?>> installNamespaces(@NonNull Install.Context context) throws IOException {
    return context.place().file(source());
  }

  /**
   * A datapack as its provider describes it.
   *
   * @param details the artifact's coordinates, name, source, targets, parity
   *                and relations
   */
  record Default(@NonNull Details details) implements Datapack {

    /**
     * Checks the details are given.
     */
    public Default {
    }
  }
}
