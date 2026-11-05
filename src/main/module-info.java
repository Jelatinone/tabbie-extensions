/**
 * Tabbie's first-party extensions: the content families and distributions
 * Tabbie ships with, written as any extension is. It requires the SDK,
 * compiles with the SDK's processors on its processor path, and provides the
 * registration they generate, so core loads it as it loads any other module.
 *
 * <p>
 * Each package is exported, so other modules can name its families and
 * distributions, and contribute narrower installs for them.
 */
module cat.tabbie.extensions {
  requires cat.tabbie.sdk;
  requires static lombok;

  exports cat.tabbie.extensions.content;
  exports cat.tabbie.extensions.distribution.bedrock;
  exports cat.tabbie.extensions.distribution.java;

  provides cat.tabbie.sdk.meta.registry.Registration with cat.tabbie.extensions.Registrations;
}
