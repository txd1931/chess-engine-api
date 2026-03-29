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

  void addMoveRecord(byte fromTile, byte toTile, Board board) {
    if(closed)
      throw new IllegalStateException("Cannot add move record to a closed MatchRecord");
    if(fromTile == PackedTile.NO_TILE && toTile == PackedTile.NO_TILE && board == null)
      throw new IllegalArgumentException("Move and Board cannot be null simultaneously");
    long turnDuration = stamps.isEmpty() ? (trackTurnDuration ? System.currentTimeMillis() : 0) : 0;
    Board boardSnapshot = board == null ? null : new Board(board);
    stamps.add(new MatchStamp(fromTile, toTile, boardSnapshot, turnDuration));
  }

  void addMoveRecord(Move move, Board board) {
    if (move == null) {
      addMoveRecord(PackedTile.NO_TILE, PackedTile.NO_TILE, board);
      return;
    }
    move.validate();
    addMoveRecord(PackedTile.fromTile(move.from()), PackedTile.fromTile(move.to()), board);
  }

  void closeRecordings() {
    stamps.trimToSize();
    closed = true;
  }

  void addMoveRecord(Board board) {
    addMoveRecord(PackedTile.NO_TILE, PackedTile.NO_TILE, board);
  }

  void addMoveRecord(MatchStamp stamp) {
    if (closed)
      throw new IllegalStateException("Cannot add move record to a closed MatchRecord");
    if (stamp == null)
      throw new IllegalArgumentException("Stamp cannot be null");
    Board boardSnapshot = stamp.board() == null ? null : new Board(stamp.board());
    stamps.add(new MatchStamp(stamp.fromTile(), stamp.toTile(), boardSnapshot, stamp.timestamp()));
  }

  public byte[][][] generateMatchTensor() {
    byte[][][] matchTensor = new byte[stamps.size()][Board.SIZE][Board.SIZE];
    for (int i = 0; i < stamps.size(); i++) {
      Board boardAt = getBoardAt(i);
      byte[][] source = boardAt.getBoardData();
      for (int c = 0; c < Board.SIZE; c++)
        System.arraycopy(source[c], 0, matchTensor[i][c], 0, Board.SIZE);
    }
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
      return new Board(stamp.board());

    int lastRecordedBoardIndex = -1;
    for (int i = index; i >= 0; i--) {
      if (stamps.get(i).board() != null) {
        board = new Board(stamps.get(i).board());
        lastRecordedBoardIndex = i;
        break;
      }
    }

    int replayStartIndex = 0;
    if (lastRecordedBoardIndex == -1) {
      board.setupStartingBoard();
    } else {
      replayStartIndex = lastRecordedBoardIndex + 1;
    }

    MoveStateTransition.State state = MoveStateTransition.initialState(board, 0);
    for (int i = replayStartIndex; i <= index; i++) {
      MatchStamp replayStamp = stamps.get(i);
      if (replayStamp.board() != null) {
        board = new Board(replayStamp.board());
        state = MoveStateTransition.initialState(board, 0);
        continue;
      }
      if (!replayStamp.hasMove())
        continue;

      state = MoveStateTransition.apply(board, replayStamp.fromTile(), replayStamp.toTile(), state).state();
    }
    return new Board(board);
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
