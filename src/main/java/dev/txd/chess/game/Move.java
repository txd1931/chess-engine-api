package dev.txd.chess.game;

class Move {
  private Tile from, to;

  public Move(Tile from, Tile to) {
    this.from = from;
    this.to = to;
  }

  public Tile from() {
    return from;
  }

  public Tile to() {
    return to;
  }

  @SuppressWarnings("unlikely-arg-type")
  public boolean isValid(ChessMatch match) {
    return match.validMoves().contains(this);
  }
}