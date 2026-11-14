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
 * Plugin-server content. Compatibility is declared explicitly per
 * distribution; support for one server fork does not imply support for
 * another.
 */
@Registered
@Applies(to = {"java:manager", "bedrock:manager"})
public sealed interface Plugin extends Artifact {

  /**
   * Places the plugin as one file named after its source, at its head: the
   * directory accepting plugins, on every Java or Bedrock plugin server.
   *
   * @param context the directory accepting plugins
   * @return images placing the plugin
   * @throws IOException when the plugin's source cannot be described
   */
  @Install
  default Set<Image<?>> installRoot(@NonNull Install.Context context) throws IOException {
    return context.head().receive(source());
  }

  /**
   * A plugin as its provider describes it.
   *
   * @param details the artifact's coordinates, name, source, targets, parity
   *                and relations
   */
  record Default(@NonNull Details details) implements Plugin {
  }
}
