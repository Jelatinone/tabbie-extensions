package cat.tabbie.extensions.distribution.java;

import java.util.Set;

import cat.tabbie.sdk.distribution.Distribution;
import cat.tabbie.sdk.distribution.Java;
import cat.tabbie.sdk.meta.registry.Registered;
import cat.tabbie.sdk.target.Environment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Server-only Java runtimes that load plugins, registered as the group
 * {@code java:manager}.
 */
@Registered("java:manager")
public interface Manager extends Java {

  /**
   * Canonical plugin-server values.
   */
  @Registered
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  enum Of implements Manager {

    /**
     * Craft-Bukkit
     */
    CRAFT_BUKKIT("java:craftbukkit"),

    /**
     * Spigot
     */
    SPIGOT("java:spigot"),

    /**
     * Paper
     */
    PAPER("java:paper"),

    /**
     * Purpur
     */
    PURPUR("java:purpur"),

    /**
     * Folia
     */
    FOLIA("java:folia");

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

  @Override
  default Set<Environment> environments() {
    return Set.of(Environment.SERVER);
  }
}
