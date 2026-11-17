/**
 * Tabbie's first-party extensions: the content families and distributions
 * Tabbie ships with, written as any extension is. It requires the SDK, and
 * compiles with the SDK's processors on its processor path, which write the
 * registration index core reads, so core loads it as it loads any other
 * module.
 *
 * <p>
 * Each package is exported, so other modules can name its families,
 * distributions and forms, and contribute narrower installs for them, and the
 * registry can call its contributions. The
 * JSON form reads documents into Jackson's tree, so modules reading them
 * compile and run against Jackson too.
 */
module cat.tabbie.extensions {
  requires cat.tabbie.sdk;
  requires static lombok;
  requires transitive tools.jackson.databind;

  exports cat.tabbie.extensions.content;
  exports cat.tabbie.extensions.distribution.bedrock;
  exports cat.tabbie.extensions.distribution.java;
  exports cat.tabbie.extensions.form;
}
