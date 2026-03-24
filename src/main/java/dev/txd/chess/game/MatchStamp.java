package dev.txd.chess.game;

record MatchStamp(byte fromTile, byte toTile, Board board, long timestamp) {

  MatchStamp {
    if (fromTile != PackedTile.NO_TILE)
      PackedTile.validate(fromTile);
    if (toTile != PackedTile.NO_TILE)
      PackedTile.validate(toTile);
  }

  public static MatchStamp fromPackedMove(byte fromTile, byte toTile, Board board, long timestamp) {
    return new MatchStamp(fromTile, toTile, board, timestamp);
  }

  public static MatchStamp fromMove(Move move, Board board, long timestamp) {
    if (move == null)
      throw new IllegalArgumentException("Move cannot be null");
    move.validate();
    return fromPackedMove(PackedTile.fromTile(move.from()), PackedTile.fromTile(move.to()), board, timestamp);
  }

  public static MatchStamp fromBoard(Board board, long timestamp) {
    return new MatchStamp(PackedTile.NO_TILE, PackedTile.NO_TILE, board, timestamp);
  }

  public boolean hasBoard() {
    return board != null;
  }

  public boolean hasMove() {
    return fromTile != PackedTile.NO_TILE && toTile != PackedTile.NO_TILE;
  }

  public Move move() {
    if (!hasMove())
      throw new IllegalStateException("MatchStamp does not contain a move");
    return new Move(PackedTile.toTile(fromTile), PackedTile.toTile(toTile));
  }
}
