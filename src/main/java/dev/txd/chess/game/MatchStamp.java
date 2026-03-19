package dev.txd.chess.game;

public record MatchStamp(Move move, boolean validMove, Board board, long timestamp) {
  public static MatchStamp fromMove(Move move, boolean validMove, long timestamp) {
    return new MatchStamp(move, validMove, null, timestamp);
  }

  public static MatchStamp fromBoard(Board board, long timestamp) {
    return new MatchStamp(null, true, board, timestamp);
  }

  public boolean hasBoard() {
    return board != null;
  }
}
