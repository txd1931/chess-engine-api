package dev.txd.chess.game;

record MatchStamp(Move move, Board board, long timestamp) {
  public static MatchStamp fromMove(Move move, Board board, long timestamp) {
    return new MatchStamp(move, null, timestamp);
  }

  public static MatchStamp fromBoard(Board board, long timestamp) {
    return new MatchStamp(null, board, timestamp);
  }

  public boolean hasBoard() {
    return board != null;
  }
}
