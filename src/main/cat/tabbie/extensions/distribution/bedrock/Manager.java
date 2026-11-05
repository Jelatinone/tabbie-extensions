package cat.tabbie.extensions.distribution.bedrock;

import java.util.Set;

import cat.tabbie.sdk.distribution.Bedrock;
import cat.tabbie.sdk.distribution.Distribution;
import cat.tabbie.sdk.meta.registry.Registered;
import cat.tabbie.sdk.target.Environment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Server-only Bedrock runtimes that load plugins, registered as the group
 * {@code bedrock:manager}. Which of them also load behaviour and resource
 * packs, and how, the pack families' installs declare.
 */
@Registered("bedrock:manager")
public interface Manager extends Bedrock {

  /**
   * Supports dedicated servers only.
   *
   * @return immutable environments
   */
  @Override
  default Set<Environment> environments() {
    return Set.of(Environment.SERVER);
  }

  /**
   * Canonical plugin-server values.
   */
  @Registered
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  enum Of implements Manager {

    /**
     * Endstone
     */
    ENDSTONE("bedrock:endstone"),

    /**
     * Cloudburst-server
     */
    CLOUDBURST("bedrock:cloudburst"),

    /**
     * Cloudburst-nukkit
     */
    CLOUDBURST_NUKKIT("bedrock:cloudburst-nukkit"),

    /**
     * Nukkit-MOT
     */
    MOT_NUKKIT("bedrock:nukkit-mot"),

    /**
     * Power-Nukkit-X
     */
    POWER_NUKKIT("bedrock:power-nukkit");

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

    /**
     * Returns the stable, case-sensitive namespaced identifier.
     *
     * @return identifier such as {@code bedrock:endstone}
     */
    @Override
    public String id() {
      return id;
    }
  }
}
