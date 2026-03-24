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
      Board stampBoard = stamps.get(i).board();
      if (stampBoard == null)
        continue;
      byte[][] source = stampBoard.getBoardData();
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

    if (lastRecordedBoardIndex == -1) {
      board.setupStartingBoard();
      return board;
    }

    for (int i = lastRecordedBoardIndex + 1; i <= index; i++) {
      MatchStamp moveStamp = stamps.get(i);
      if (!moveStamp.hasMove())
        continue;

      int fromColumn = PackedTile.column(moveStamp.fromTile());
      int fromRow = PackedTile.row(moveStamp.fromTile());
      int toColumn = PackedTile.column(moveStamp.toTile());
      int toRow = PackedTile.row(moveStamp.toTile());

      int movingPiece = board.pieceAt(fromColumn, fromRow);
      board.setPieceAt(toColumn, toRow, movingPiece);
      board.setPieceAt(fromColumn, fromRow, Board.EMPTY);
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
