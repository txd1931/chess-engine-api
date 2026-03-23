package dev.txd.chess.game;

public class Move {
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

  public void validate() {
    if (from == null || to == null)
      throw new IllegalArgumentException("Move tiles cannot be null");
    from.validate();
    to.validate();
  }

  public boolean isValid(ChessMatch match) {
    return match.isMoveLegal(this);
  }
}