package cat.tabbie.extensions.form;

import java.io.IOException;

import cat.tabbie.sdk.form.Form;
import cat.tabbie.sdk.tree.Reason;
import cat.tabbie.sdk.tree.Refusal;
import cat.tabbie.sdk.tree.Shape;
import cat.tabbie.sdk.tree.Source;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A file read as a JSON document, leniently, allowing the comments and
 * trailing commas game files carry. The document is read into Jackson's tree
 * when the form is made, and bound to values on request. Typed documents
 * extend it, as a pack's {@link cat.tabbie.extensions.content.Manifest
 * manifest} does.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Json extends Form {

  private static final JsonMapper MAPPER = JsonMapper.builder()
      .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS, JsonReadFeature.ALLOW_TRAILING_COMMA)
      .build();

  JsonNode tree;

  /**
   * Binds a document to the file it was read from.
   *
   * @param shape the file read
   * @param tree  the document it holds
   */
  protected Json(@NonNull Shape shape, @NonNull JsonNode tree) {
    super(shape);
    this.tree = tree.deepCopy();
  }

  /**
   * Reads a file as a JSON document.
   *
   * @param shape the file to read
   * @return the document
   * @throws Refusal     with {@link Reason.Content.Unreadable} when the shape
   *                     is not a file of JSON
   * @throws IOException when the file cannot be read
   */
  public static Json of(@NonNull Shape shape) throws IOException {
    return new Json(shape, read(shape, Json.class));
  }

  /**
   * Reads a file's document into Jackson's tree, refusing in the name of the
   * form reading it.
   *
   * @param shape the file to read
   * @param form  the form reading it
   * @return the document
   * @throws Refusal     with {@link Reason.Content.Unreadable} when the shape
   *                     is not a file of JSON
   * @throws IOException when the file cannot be read
   */
  protected static JsonNode read(@NonNull Shape shape, @NonNull Class<? extends Json> form) throws IOException {
    if (!(shape instanceof Shape.File file)) {
      throw new Refusal(new Reason.Content.Unreadable(shape, form));
    }
    try {
      return MAPPER.readTree(file.bytes());
    } catch (JacksonException exception) {
      throw new Refusal(new Reason.Content.Unreadable(shape, form), exception);
    }
  }

  /**
   * Writes a value as a JSON file, in one fixed indented layout.
   *
   * @param name  portable filename the file takes
   * @param value the value Jackson binds
   * @return the written file
   * @throws IllegalArgumentException when Jackson cannot bind the value, or
   *                                  the name is not a portable single
   *                                  filename
   */
  public static Source.File write(@NonNull String name, @NonNull Object value) {
    try {
      return Source.of(name, String.format("%s\n", MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value)));
    } catch (JacksonException exception) {
      throw new IllegalArgumentException("A document is written from a value Jackson binds.", exception);
    }
  }

  /**
   * Returns the document.
   *
   * @return a copy of the document's tree
   */
  @NonNull
  public JsonNode tree() {
    return tree.deepCopy();
  }

  /**
   * Binds the document to a value.
   *
   * @param <Value> the value's type
   * @param type    the value's class
   * @return the bound value
   * @throws Refusal with {@link Reason.Content.Malformed} when the document
   *                 does not bind to the type
   */
  @NonNull
  public <Value> Value value(@NonNull Class<Value> type) throws Refusal {
    try {
      return MAPPER.treeToValue(tree, type);
    } catch (JacksonException exception) {
      throw malformed(type.getSimpleName(), exception);
    }
  }

  /**
   * Binds the document to a generic value.
   *
   * @param <Value> the value's type
   * @param type    the value's type, with its parameters
   * @return the bound value
   * @throws Refusal with {@link Reason.Content.Malformed} when the document
   *                 does not bind to the type
   */
  @NonNull
  public <Value> Value value(@NonNull TypeReference<Value> type) throws Refusal {
    try {
      return MAPPER.readerFor(type).readValue(tree);
    } catch (JacksonException exception) {
      throw malformed(type.getType().getTypeName(), exception);
    }
  }

  /**
   * Refuses a document that does not bind to a type.
   */
  private Refusal malformed(String type, JacksonException exception) {
    return new Refusal(new Reason.Content.Malformed(shape, getClass(),
        String.format("The document does not describe a %s.", type)), exception);
  }
}
