package dev.txd.chess.game;

final class PackedTile {
  private static final int COLUMN_MASK = 0b0000_0111;
  private static final int ROW_MASK = 0b0011_1000;
  static final byte NO_TILE = (byte) 0xFF;

  private PackedTile() {
  }

  static byte encode(int column, int row) {
    validate(column, row);
    return (byte) ((row << 3) | column);
  }

  static byte fromTile(Tile tile) {
    if (tile == null)
      throw new IllegalArgumentException("Tile cannot be null");
    tile.validate();
    return encode(tile.column(), tile.row());
  }

  static Tile toTile(byte packedTile) {
    validate(packedTile);
    return new Tile(column(packedTile), row(packedTile));
  }

  static int column(byte packedTile) {
    validate(packedTile);
    return packedTile & COLUMN_MASK;
  }

  static int row(byte packedTile) {
    validate(packedTile);
    return (packedTile & ROW_MASK) >>> 3;
  }

  static void validate(byte packedTile) {
    int unsigned = packedTile & 0xFF;
    if (unsigned > 0b0011_1111)
      throw new IllegalArgumentException("Invalid packed tile value: " + unsigned);
  }

  private static void validate(int column, int row) {
    if (column < 0 || column >= Board.SIZE || row < 0 || row >= Board.SIZE)
      throw new IllegalArgumentException("Tile coordinates must be between 0 and " + (Board.SIZE - 1));
  }
}