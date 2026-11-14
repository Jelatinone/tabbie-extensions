package cat.tabbie.extensions.content;

import java.io.IOException;
import java.util.Set;

import cat.tabbie.sdk.content.Artifact;
import cat.tabbie.sdk.meta.action.Install;
import cat.tabbie.sdk.meta.attribute.Applies;
import cat.tabbie.sdk.meta.registry.Registered;
import cat.tabbie.sdk.revision.Image;
import lombok.NonNull;

/**
 * Java loader mod content. Compatibility is declared explicitly per
 * distribution; support for one loader does not imply support for another.
 */
@Registered
@Applies(to = "java:launcher")
public sealed interface Mod extends Artifact {

  /**
   * Places the mod as one file named after its source, at its head: the
   * directory accepting mods, on every Java mod loader.
   *
   * @param context the directory accepting mods
   * @return images placing the mod
   * @throws IOException when the mod's source cannot be described
   */
  @Install
  default Set<Image<?>> installRoot(@NonNull Install.Context context) throws IOException {
    return context.head().receive(source());
  }

  /**
   * A mod as its provider describes it.
   *
   * @param details the artifact's coordinates, name, source, targets, parity
   *                and relations
   */
  record Default(@NonNull Details details) implements Mod {
  }
}
