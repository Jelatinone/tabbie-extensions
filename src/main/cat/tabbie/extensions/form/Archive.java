package cat.tabbie.extensions.form;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import cat.tabbie.sdk.failure.Reason;
import cat.tabbie.sdk.failure.Refusal;
import cat.tabbie.sdk.form.Form;
import cat.tabbie.sdk.tree.Address;
import cat.tabbie.sdk.tree.Describe;
import cat.tabbie.sdk.tree.Entry;
import cat.tabbie.sdk.tree.Lease;
import cat.tabbie.sdk.tree.Rule;
import cat.tabbie.sdk.tree.Shape;
import cat.tabbie.sdk.tree.State;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

/**
 * A file read as a ZIP archive: itself the root directory of the
 * {@link Entry entries} the archive holds, walked and received like any
 * directory. Making it reads the archive's entries once, to learn each file's
 * path, size and CRC-32 without keeping their bytes; an entry's bytes are
 * read from the archive again only when its lease reads them, and checked
 * against what was learned.
 *
 * <p>
 * Every path must be safe to place: one that is not a chain of portable
 * names, such as one leading outside the archive, makes the archive
 * {@link Reason.Content.Unsafe unsafe}. Directories are those the archive
 * declares and those its files' paths imply. An archive is read-only, and
 * limits on its size are core's.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class Archive extends Form implements Entry.Directory {

  Map<Address, Measure> files = new LinkedHashMap<>();

  Set<Address> directories = new LinkedHashSet<>();

  private Archive(Shape shape) {
    super(shape);
  }

  /**
   * Reads a file as a ZIP archive.
   *
   * @param shape the file to read
   * @return the archive, as the root directory of its entries
   * @throws Refusal     with {@link Reason.Content.Unreadable} when the shape
   *                     is not a ZIP file, {@link Reason.Content.Unsafe} when
   *                     an entry's path is not safe to place, or
   *                     {@link Reason.Content.Malformed} when its entries
   *                     cannot be read or repeat a path
   * @throws IOException when the file cannot be read
   */
  public static Archive of(@NonNull Shape shape) throws IOException {
    if (!(shape instanceof Shape.File file)) {
      throw new Refusal(new Reason.Content.Unreadable(shape, Archive.class));
    }
    Archive archive = new Archive(shape);
    try (Lease.Read lease = file.read()) {
      ByteBuffer magic = ByteBuffer.allocate(2);
      lease.read(0, magic);
      if (magic.position() < 2 || magic.get(0) != 'P' || magic.get(1) != 'K') {
        throw new Refusal(new Reason.Content.Unreadable(shape, Archive.class));
      }
      try (ZipInputStream entries = new ZipInputStream(lease.stream())) {
        for (ZipEntry entry; (entry = entries.getNextEntry()) != null;) {
          archive.learn(entry, entries);
        }
      } catch (ZipException exception) {
        throw new Refusal(new Reason.Content.Malformed(shape, Archive.class, "Its entries cannot be read."),
            exception);
      }
    }
    if (archive.files.keySet().stream().anyMatch(archive.directories::contains)) {
      throw new Refusal(new Reason.Content.Malformed(shape, Archive.class,
          "Each path is a file or a directory, never both."));
    }
    return archive;
  }

  /**
   * Learns one entry's path, and, for a file, its size and CRC-32, reading the
   * rest of the entry from the archive.
   */
  private void learn(ZipEntry entry, ZipInputStream entries) throws IOException {
    Address address = address(entry.getName());
    for (Optional<Address> above = address.parent(); above.isPresent() && !above.get().isRoot();
        above = above.get().parent()) {
      directories.add(above.get());
    }
    if (entry.isDirectory()) {
      directories.add(address);
      return;
    }
    CRC32 checksum = new CRC32();
    long size = 0;
    byte[] buffer = new byte[8192];
    for (int read; (read = entries.read(buffer)) != -1;) {
      checksum.update(buffer, 0, read);
      size += read;
    }
    if (files.put(address, new Measure(size, String.format("%08x", checksum.getValue()))) != null) {
      throw new Refusal(new Reason.Content.Malformed(shape, Archive.class, "It holds each path once."));
    }
  }

  /**
   * Reads an entry's path as an address, refusing one that is not safe.
   */
  private Address address(String name) throws Refusal {
    String path = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    try {
      Address address = Address.parse(path);
      if (!address.isRoot()) {
        return address;
      }
    } catch (IllegalArgumentException exception) {
      throw new Refusal(new Reason.Content.Unsafe(shape, name), exception);
    }
    throw new Refusal(new Reason.Content.Unsafe(shape, name));
  }

  /**
   * What is at an address in the archive.
   */
  private State state(Address address) {
    if (address.isRoot() || directories.contains(address)) {
      return new State.Directory();
    }
    return files.containsKey(address)
        ? new State.File(new Packed(address).describe(files.get(address)))
        : new State.Absent();
  }

  /**
   * The entries directly beneath a directory of the archive.
   */
  private List<Entry> beneath(Address directory) {
    List<Entry> children = new ArrayList<>();
    directories.stream()
        .filter(address -> address.parent().equals(Optional.of(directory)))
        .forEach(address -> children.add(new Folder(address)));
    files.keySet().stream()
        .filter(address -> address.parent().equals(Optional.of(directory)))
        .forEach(address -> children.add(new Packed(address)));
    return List.copyOf(children);
  }

  @Override
  public Address address() {
    return Address.root();
  }

  @Override
  public Optional<Entry.Directory> parent() {
    return Optional.empty();
  }

  @Override
  public State state() {
    return new State.Directory();
  }

  @Override
  public List<Entry> children() {
    return beneath(Address.root());
  }

  @Override
  public Entry.File file(@NonNull Address tail) {
    if (tail.isRoot()) {
      throw new IllegalArgumentException("A file lies beneath a directory, not in its place.");
    }
    return new Packed(tail);
  }

  @Override
  public Entry.Directory directory(@NonNull Address tail) {
    return tail.isRoot() ? this : new Folder(tail);
  }

  /**
   * What the archive declares of a file's bytes: their size and CRC-32.
   *
   * @param size size of the bytes
   * @param crc  lowercase hexadecimal CRC-32
   */
  private record Measure(long size, String crc) {
  }

  /**
   * A file entry, held by the archive or absent.
   */
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  private final class Packed implements Entry.File {

    Address address;

    Packed(Address address) {
      this.address = address;
    }

    private Describe describe(Measure measure) {
      return new Describe(name(), measure.size(), Map.of(Describe.Digest.CRC32, measure.crc()), this);
    }

    @Override
    public Address address() {
      return address;
    }

    @Override
    public Optional<Entry.Directory> parent() {
      return Optional.of(directory(address.parent().orElseThrow()));
    }

    @Override
    public State state() {
      return Archive.this.state(address);
    }

    @Override
    public Describe describe() throws Refusal {
      Measure measure = files.get(address);
      if (measure == null) {
        throw new Refusal(directories.contains(address)
            ? new Reason.Place.Occupied(address, new State.Directory())
            : new Reason.Place.Missing(address));
      }
      return describe(measure);
    }

    @Override
    public Lease.Read read() throws IOException {
      Describe expected = describe();
      try (Lease.Read lease = ((Shape.File) shape).read();
          ZipInputStream archive = new ZipInputStream(lease.stream())) {
        for (ZipEntry entry; (entry = archive.getNextEntry()) != null;) {
          if (!entry.isDirectory() && Archive.this.address(entry.getName()).equals(address)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            archive.transferTo(bytes);
            Describe found = Describe.of(name(), bytes.toByteArray(), this);
            if (!found.agrees(expected)) {
              throw new Refusal(new Reason.Content.Mismatch(this, expected, found));
            }
            return new Opened(address, bytes.toByteArray());
          }
        }
      } catch (ZipException exception) {
        throw new Refusal(new Reason.Content.Malformed(shape, Archive.class, "Its entries cannot be read."), exception);
      }
      throw new Refusal(new Reason.Content.Malformed(shape, Archive.class, "It no longer holds what it held."));
    }

    @Override
    public Lease.Write write() throws Refusal {
      throw new Refusal(new Reason.Place.Denied(address, Rule.Permission.EDITABLE));
    }
  }

  /**
   * A directory entry, held by the archive or absent.
   */
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  private final class Folder implements Entry.Directory {

    Address address;

    Folder(Address address) {
      this.address = address;
    }

    @Override
    public Address address() {
      return address;
    }

    @Override
    public Optional<Entry.Directory> parent() {
      return Optional.of(Archive.this.directory(address.parent().orElseThrow()));
    }

    @Override
    public State state() {
      return Archive.this.state(address);
    }

    @Override
    public List<Entry> children() {
      return beneath(address);
    }

    @Override
    public Entry.File file(@NonNull Address tail) {
      return Archive.this.file(address.resolve(tail));
    }

    @Override
    public Entry.Directory directory(@NonNull Address tail) {
      return Archive.this.directory(address.resolve(tail));
    }
  }

  /**
   * A read lease on an entry's bytes, read once from the archive.
   */
  @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
  private static final class Opened implements Lease.Read {

    Address address;

    byte[] bytes;

    @NonFinal
    volatile boolean closed;

    Opened(Address address, byte[] bytes) {
      this.address = address;
      this.bytes = bytes;
    }

    private byte[] held() {
      if (closed) {
        throw new IllegalStateException("A closed lease cannot be used.");
      }
      return bytes;
    }

    @Override
    public Address address() {
      return address;
    }

    @Override
    public long size() {
      return held().length;
    }

    @Override
    public int read(long position, @NonNull ByteBuffer target) {
      return Lease.Read.copy(held(), position, target);
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
