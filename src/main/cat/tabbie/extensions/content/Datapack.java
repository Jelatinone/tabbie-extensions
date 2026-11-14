package cat.tabbie.extensions.content;

import static cat.tabbie.sdk.meta.action.Closure.NAMESPACES;

import java.io.IOException;
import java.util.Set;

import cat.tabbie.sdk.content.Artifact;
import cat.tabbie.sdk.meta.action.Install;
import cat.tabbie.sdk.meta.attribute.Applies;
import cat.tabbie.sdk.meta.registry.Registered;
import cat.tabbie.sdk.revision.Image;
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
   * Places the datapack as one file named after its source in a World, at its
   * head: the World's directory accepting datapacks, on every Java
   * distribution. Core calls it once for each World. The game loads a zipped
   * datapack as it is, and unpacking one wrapped in a folder of its own would
   * bury its {@code pack.mcmeta} too deep to load.
   *
   * @param context the directory accepting datapacks in one World
   * @return images placing the datapack
   * @throws IOException when the datapack's source cannot be described
   */
  @Install(NAMESPACES)
  default Set<Image<?>> installNamespaces(@NonNull Install.Context context) throws IOException {
    return context.head().receive(source());
  }

  /**
   * A datapack as its provider describes it.
   *
   * @param details the artifact's coordinates, name, source, targets, parity
   *                and relations
   */
  record Default(@NonNull Details details) implements Datapack {
  }
}
