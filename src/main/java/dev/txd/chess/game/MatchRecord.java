package dev.txd.chess.game;

import java.util.ArrayList;

class MatchRecord {

  private ArrayList<MatchStamp> stamps;
  private boolean trackTurnDuration;
  private boolean closed;

  MatchRecord(boolean trackTurnDuration) {
    stamps = new ArrayList<>();
    this.trackTurnDuration = trackTurnDuration; 
    closed = false;
  }

  void addMoveRecord(Move move, Board board) {
    if(closed)
      throw new IllegalStateException("Cannot add move record to a closed MatchRecord");
    if(move == null && board == null)
      throw new IllegalArgumentException("Move and Board cannot be null simultaneously");
    long turnDuration = stamps.isEmpty() ? (trackTurnDuration ? System.currentTimeMillis() : 0) : 0;
    stamps.add(new MatchStamp(move, board, turnDuration));
  }

  void closeRecordings() {
    stamps.trimToSize();
    closed = true;
  }

  public void addMoveRecord(Board board) {
    addMoveRecord(null, board);
  }

  public void addMoveRecord(MatchStamp stamp) {
    stamps.add(stamp);
  }

  public byte[][][] generateMatchTensor() {
    byte[][][] matchTensor = new byte[Board.SIZE][Board.SIZE][stamps.size()];
    for (int i = 0; i < stamps.size(); i++)
      matchTensor[i] = stamps.get(i).board().getBoardData();
    return matchTensor;
  }

  public long[] timestamps(){
    long[] timestamps = new long[stamps.size()];
    for (int i = 0; i < stamps.size(); i++)
      timestamps[i] = stamps.get(i).timestamp();
    return timestamps;
  }

  public long matchDuration(){
    long duration = 0;
    for (MatchStamp matchStamp : stamps) 
      duration += matchStamp.timestamp();
    return duration;
  }

  public Board getBoardAt(int index) {
    Board board = new Board();
    if (index < 0 || index >= stamps.size())
      throw new IndexOutOfBoundsException("Index out of bounds: " + index);
    MatchStamp stamp = stamps.get(index);
    if (stamp.board() != null)
      return stamp.board();

    int lastRecordedBoardIndex = -1;
    for (int i = index; i >= 0; i--) {
      if (stamps.get(i).board() != null) {
        board = new Board(stamps.get(i).board());
        lastRecordedBoardIndex = i;
        break;
      }
    }
    if (lastRecordedBoardIndex == -1) {
      board.setupStartingBoard();
      return board;
    }
    for (int i = lastRecordedBoardIndex + 1; i <= index; i++) {
      board.setPieceAt(stamps.get(i).move().from().row(), stamps.get(i).move().from().column(),
          stamps.get(i).board().pieceAt(stamps.get(i).move().from().row(), stamps.get(i).move().from().column()));
    }
    return board;
  }

  public boolean validateMoves() {
    for (MatchStamp stamp : stamps) {
      if (stamp.board() != null)
        return false;
    }
    return true;
  }

  public int getMoveCount() {
    return stamps.size();
  }

  public int getValidMoveCount() {
    int count = 0;
    for (MatchStamp stamp : stamps) {
      if (stamp.board() == null)
        count++;
    }
    return count;
  }
}
