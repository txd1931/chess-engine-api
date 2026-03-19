package dev.txd.chess.game;

public class Tile {
  private int column, row;
  public Tile(int column, int row) {
    this.column = column;
    this.row = row;
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
    return t.column() == column && t.row() == row;
  }

  @Override
  public int hashCode() {
    int result = column;
    result = 31 * result + row;
    return result;
  }
}
