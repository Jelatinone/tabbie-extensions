package cat.tabbie.extensions.distribution.java;

import cat.tabbie.sdk.distribution.Distribution;
import cat.tabbie.sdk.distribution.Java;
import cat.tabbie.sdk.meta.registry.Registered;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Unmodified Java Edition runtimes.
 */
@Registered
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum Vanilla implements Java {

  /**
   * Vanilla
   */
  VANILLA("java:vanilla");

  String id;

  /**
   * Vanilla constructor
   *
   * @param id identifier
   */
  Vanilla(String id) {
    Distribution.validate(id);
    this.id = id;
  }

  @Override
  public String id() {
    return id;
  }
}
