package dev.txd.chess.game;

public class Board {
  public final static int BOARD_SIZE = 8;

  public static final byte WHITE_KING = 6;
  public static final byte WHITE_QUEEN = 5;
  public static final byte WHITE_ROOK = 4;
  public static final byte WHITE_BISHOP = 3;
  public static final byte WHITE_KNIGHT = 2;
  public static final byte WHITE_PAWN = 1;
  public static final byte EMPTY = 0;
  public static final byte BLACK_PAWN = -1;
  public static final byte BLACK_KNIGHT = -2;
  public static final byte BLACK_BISHOP = -3;
  public static final byte BLACK_ROOK = -4;
  public static final byte BLACK_QUEEN = -5;
  public static final byte BLACK_KING = -6;

  public static final byte[][] STARTING_BOARD = new byte[][]{
      {BLACK_ROOK, BLACK_KNIGHT, BLACK_BISHOP, BLACK_QUEEN, BLACK_KING, BLACK_BISHOP, BLACK_KNIGHT, BLACK_ROOK},
      {BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN, BLACK_PAWN},
      {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
      {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
      {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
      {EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY},
      {WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN, WHITE_PAWN},
      {WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, WHITE_QUEEN, WHITE_KING, WHITE_BISHOP, WHITE_KNIGHT, WHITE_ROOK}};

  private byte[][] boardData;

  public void fillBOard(byte[][] boardData) {
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

  public void resetBoard() {
    boardData = STARTING_BOARD;
  }

  public int[][] colorData() {
    int[][] colorData = new int[BOARD_SIZE][BOARD_SIZE];
    for (int c = 0; c < BOARD_SIZE; c++)
      for (int r = 0; r < BOARD_SIZE; r++)
        colorData[c][r] = boardData[c][r] == 0 ? 0 : boardData[c][r] > 0 ? 1 : -1;
    return colorData;
  }

  public int[][] pieceTypeData() {
    int[][] pieceTypeData = new int[BOARD_SIZE][BOARD_SIZE];
    for (int c = 0; c < BOARD_SIZE; c++)
      for (int r = 0; r < BOARD_SIZE; r++)
        pieceTypeData[c][r] = Math.abs(boardData[c][r]);
    return pieceTypeData;
  }

  public boolean isWhiteAt(int row, int column) {
    return boardData[row][column] > 0;
  }

  public int pieceTypeAt(int row, int column) {
    validateTile(row, column);
    return Math.abs(boardData[row][column]);
  }

  public int pieceAt(int row, int column) {
    validateTile(row, column);
    return (int) boardData[row][column];
  }

  public void setPieceAt(int row, int column, int piece) {
    validateTile(row, column);
    boardData[row][column] = (byte) piece;
  }

  private void validateTile(int row, int column) {
    if (row < 0 || row >= boardData.length || column < 0 || column >= boardData[0].length)
      throw new IllegalArgumentException("Invalid tile coordinates");
  }
}
