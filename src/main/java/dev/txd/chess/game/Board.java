package dev.txd.chess.game;

public class Board {
  public final static int SIZE = 8;

  public static final byte EMPTY = 0;
  public static final byte PAWN = 1;
  public static final byte KNIGHT = 2;
  public static final byte BISHOP = 3;
  public static final byte ROOK = 4;
  public static final byte QUEEN = 5;
  public static final byte KING = 6;

  public static final byte[][] STARTING_BOARD = new byte[][] {
      { -ROOK, -KNIGHT, -BISHOP, -QUEEN, -KING, -BISHOP, -KNIGHT, -ROOK },
      { -PAWN, -PAWN,   -PAWN,   -PAWN,  -PAWN, -PAWN,   -PAWN,   -PAWN },
      { EMPTY, EMPTY,   EMPTY,   EMPTY,  EMPTY, EMPTY,   EMPTY,   EMPTY },
      { EMPTY, EMPTY,   EMPTY,   EMPTY,  EMPTY, EMPTY,   EMPTY,   EMPTY },
      { EMPTY, EMPTY,   EMPTY,   EMPTY,  EMPTY, EMPTY,   EMPTY,   EMPTY },
      { EMPTY, EMPTY,   EMPTY,   EMPTY,  EMPTY, EMPTY,   EMPTY,   EMPTY }, 
      { PAWN,  PAWN,    PAWN,    PAWN,   PAWN,  PAWN,    PAWN,    PAWN },
      { ROOK,  KNIGHT,  BISHOP,  QUEEN,  KING,  BISHOP,  KNIGHT,  ROOK } };

  private byte[][] boardData;

  public Board() {
    boardData = new byte[SIZE][SIZE];
  }
  
  Board(Board other) {
    this.boardData = other.boardData;
  }

  public void fillBoard(byte[][] boardData) {
    this.boardData = boardData;
  }

  public void initBoard() {
    boardData = new byte[8][8];
  }

  public byte[][] getBoardData() {
    return boardData;
  }

  public void setBoardData(byte[][] boardData) {
    this.boardData = boardData;
  }

  public void setupStartingBoard() {
    boardData = STARTING_BOARD;
  }

  public int[][] colorData() {
    int[][] colorData = new int[SIZE][SIZE];
    for (int c = 0; c < SIZE; c++)
      for (int r = 0; r < SIZE; r++)
        colorData[c][r] = boardData[c][r] == 0 ? 0 : boardData[c][r] > 0 ? 1 : -1;
    return colorData;
  }

  public int[][] pieceTypeData() {
    int[][] pieceTypeData = new int[SIZE][SIZE];
    for (int c = 0; c < SIZE; c++)
      for (int r = 0; r < SIZE; r++)
        pieceTypeData[c][r] = Math.abs(boardData[c][r]);
    return pieceTypeData;
  }

  public boolean isWhiteAt(int column, int row) {
    return boardData[column][row] > 0;
  }

  public int pieceTypeAt(int column, int row) {
    validateTile(column, row);
    return Math.abs(boardData[column][row]);
  }

  public int pieceAt(int column, int row) {
    validateTile(column, row);
    return (int) boardData[column][row];
  }

  public void setPieceAt(int column, int row, int piece) {
    validateTile(column, row);
    boardData[column][row] = (byte) piece;
  }

  private void validateTile(int column, int row) {
    if (column < 0 || column >= boardData[0].length || row < 0 || row >= boardData.length)
      throw new IllegalArgumentException("Invalid tile coordinates");
  }
}
