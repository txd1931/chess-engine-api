package dev.txd.chess.game;

public class Tile {
  private int row, column;
  public Tile(int row, int column) {
    this.row = row;
    this.column = column;
  }
  public int column() {
    return column;
  }

  public int row() {
    return row;
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
