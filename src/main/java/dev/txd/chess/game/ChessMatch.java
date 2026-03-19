package dev.txd.chess.game;

import java.util.ArrayList;

public final class ChessMatch {

  private boolean enforceRules = true;

  private Board board;
  private MatchResult matchResult;
  private MatchRecord matchRecord;
  private boolean whiteTurn;

  public ChessMatch() {
    this.enforceRules = true;
    matchResult = MatchResult.ONGOING;
    board.resetBoard();
    matchRecord = new MatchRecord();
    whiteTurn = true;
  }

  public void setCheats(boolean cheatPermision) {
    enforceRules = cheatPermision;
  }

  public void move(int fromColumn, int fromRow, int toColumn, int toRow) {
    if (!isMoveLegal(fromColumn, fromRow, toColumn, toRow))
      throw new IllegalArgumentException("Invalid move");
  }

  public boolean isMoveLegal(int fromColumn, int fromRow, int toColumn, int toRow) {
    return true;
  }

  public ArrayList<Tile> validMoves() {
    return new ArrayList<>();
  }

  private void initialize(boolean cheatPermission) {

  }
}
