package cat.tabbie.extensions.distribution.java;

import cat.tabbie.sdk.distribution.Distribution;
import cat.tabbie.sdk.distribution.Java;
import cat.tabbie.sdk.meta.registry.Registered;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Java loaders supporting mods on clients and dedicated servers, registered
 * as the group {@code java:launcher}.
 */
@Registered("java:launcher")
public interface Launcher extends Java {

  /**
   * Canonical mod-loader values.
   */
  @Registered
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  enum Of implements Launcher {

    /**
     * Fabric
     */
    FABRIC("java:fabric"),

    /**
     * Quilt
     */
    QUILT("java:quilt"),

    /**
     * Forge
     */
    FORGE("java:forge"),

    /**
     * Neo-Forge
     */
    NEO_FORGE("java:neoforge");

    String id;

    /**
     * Of constructor
     *
     * @param id identifier
     */
    Of(String id) {
      Distribution.validate(id);
      this.id = id;
    }

    @Override
    public String id() {
      return id;
    }

  }
}
