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
 * Java loader mod content. Compatibility is declared explicitly per
 * distribution; support for one loader does not imply support for another.
 */
@Registered
@Applies(to = "java:launcher")
public sealed interface Mod extends Artifact {

  /**
   * Places the mod as one file named after its source, in the directory of
   * the Cat's model accepting mods, on every Java mod loader.
   *
   * @param context the work, and where mods go in the Cat
   * @return validated immutable images
   * @throws IOException when the mod cannot be captured
   * @throws Violation   when the model does not allow the file, such as
   *                     {@link Reason.Placement.Unplaced} when no directory
   *                     accepts mods
   */
  @Install
  default Set<Image<?>> installRoot(@NonNull Install.Context context) throws IOException {
    return context.place().file(source());
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
