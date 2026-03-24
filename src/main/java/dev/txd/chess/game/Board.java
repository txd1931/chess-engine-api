package dev.txd.chess.game;

import java.util.Optional;

public class Board {
  public final static int SIZE = 8;

  public static final byte EMPTY = 0;
  public static final byte PAWN = 1;
  public static final byte KNIGHT = 2;
  public static final byte BISHOP = 3;
  public static final byte ROOK = 4;
  public static final byte QUEEN = 5;
  public static final byte KING = 6;

  private int whiteCount;
  private int blackCount;
  private Tile whiteKing;
  private Tile blackKing;

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

  public Board() {}
  
  Board(Board other) {
    this.boardData = other.boardData;
  }

  public void initBoard() {
    boardData = new byte[8][8];
  }

  public byte[][] getBoardData() {
    return boardData;
  }

  public void setBoardData(byte[][] boardData) {
    for (int c = 0; c < Board.SIZE; c++) {
      for (int r = 0; r < Board.SIZE; r++) {
        if (boardData[c][r] < -6 || boardData[c][r] > 6)
          throw new IllegalArgumentException("Invalid piece value at (" + c + ", " + r + "): " + boardData[c][r]);
      }
    }
    this.boardData = boardData;
    findKings();
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
    validateTile(column, row);
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

  public Tile[] occupiedTiles() {
    Tile[] occupied = new Tile[piecesCount()];
    for (int c = 0; c < SIZE; c++)
      for (int r = 0; r < SIZE; r++)
        if (boardData[c][r] != EMPTY)
          occupied[c * SIZE + r] = new Tile(c, r);
    return occupied;
  }

  public Tile[] emptyTiles() {
    Tile[] empty = new Tile[SIZE * SIZE - piecesCount()];
    for (int c = 0; c < SIZE; c++)
      for (int r = 0; r < SIZE; r++)
        if (boardData[c][r] == EMPTY)
          empty[c * SIZE + r] = new Tile(c, r);
    return empty;
  }

  public int piecesCount(){
    return whiteCount + blackCount;
  }

  public int piecesCount(boolean forWhite){
    return forWhite ? whiteCount : blackCount;
  }

  public Tile[] pieces(){
    Tile[] pieces = new Tile[piecesCount()];
    for (int c = 0; c < SIZE; c++)
      for (int r = 0; r < SIZE; r++)
        if (boardData[c][r] != EMPTY)
          pieces[c * SIZE + r] = new Tile(c, r);
    return pieces;
  }

  public Tile[] pieces(boolean forWhite){
    Tile[] pieces = new Tile[piecesCount(forWhite)];
    for (int c = 0; c < SIZE; c++)
      for (int r = 0; r < SIZE; r++)
        if ((forWhite && boardData[c][r] > 0) || (!forWhite && boardData[c][r] < 0))
          pieces[c * SIZE + r] = new Tile(c, r);
    return pieces;
  }

  public Tile[] tilesWithPieceType(int pieceType) {
    Tile[] tiles = new Tile[SIZE * SIZE];
    for (int c = 0; c < SIZE; c++)
      for (int r = 0; r < SIZE; r++)
        if (Math.abs(boardData[c][r]) == pieceType)
          tiles[c * SIZE + r] = new Tile(c, r);
    return tiles;
  }

  public Tile[] tiles(){
    Tile[] tiles = new Tile[SIZE * SIZE];
    for (int c = 0; c < SIZE; c++)
      for (int r = 0; r < SIZE; r++)
        tiles[c * SIZE + r] = new Tile(c, r);
    return tiles;
  }

  public void setPieceAt(int column, int row, int piece) {
    validateTile(column, row);
    if(pieceAt(column, row) == KING)
      whiteKing = new Tile(column, row);
    else if(pieceAt(column, row) == -KING)
      blackKing = new Tile(column, row);
    if (piece > 0) {
      if (boardData[column][row] <= 0)
        whiteCount++;
    } else if (piece < 0) {
      if (boardData[column][row] >= 0)
        blackCount++;
    }
    boardData[column][row] = (byte) piece;
  }

  public Tile getKing(boolean white) {
    return white ? whiteKing : blackKing;
  }

  public Optional<Tile[]> pawnsToPromote(boolean forWhite) {
    Tile[] pawnsToPromote = new Tile[SIZE];
    for (int c = 0; c < SIZE; c++)
      if ((forWhite && boardData[c][0] == PAWN) || (!forWhite && boardData[c][7] == -PAWN))
        pawnsToPromote[c] = new Tile(c, forWhite ? 0 : 7);
    return Optional.of(pawnsToPromote);
  }

  private void validateTile(int column, int row) {
    if (column < 0 || column >= boardData[0].length || row < 0 || row >= boardData.length)
      throw new IllegalArgumentException("Invalid tile coordinates");
  }

  private void findKings() {
    whiteKing = blackKing = null;
    for (int c = 0; c < SIZE; c++) {
      for (int r = 0; r < SIZE; r++) {
        if (boardData[c][r] == KING) {
          if (whiteKing != null)
            throw new IllegalStateException("Multiple white kings found on the board");
          whiteKing = new Tile(c, r);
        }
        if (boardData[c][r] == -KING) {
          if (blackKing != null)
            throw new IllegalStateException("Multiple black kings found on the board");
          blackKing = new Tile(c, r);
        }
      }
    }
  }
}
