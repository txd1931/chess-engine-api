package dev.txd.chess.game;

public class Tile {
  private int row, column;
  public Tile(int column , int row) {
    this.column = column;
    this.row = row;
  }
  
  public int column() {
    return column;
  }

  public int row() {
    return row;
  }

  public void validate() {
    if (row < 0 || row >= Board.SIZE || column < 0 || column >= Board.SIZE)
      throw new IllegalArgumentException("Tile coordinates must be between 0 and " + (Board.SIZE - 1));
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof Tile))
      return false;
    Tile t = (Tile) o;
    return t.row() == row && t.column() == column;
  }

  @Override
  public int hashCode() {
    int result = column;
    result = 31 * result + row;
    return result;
  }
}
