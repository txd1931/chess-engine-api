package dev.txd.chess.game;

public final class ChessMatch {
  private final static int BOARD_SIZE = 8;

  public static final byte EMPTY = 0;

  public static final byte WHITE_PAWN = 1;
  public static final byte WHITE_KNIGHT = 2;
  public static final byte WHITE_BISHOP = 3;
  public static final byte WHITE_ROOK = 4;
  public static final byte WHITE_QUEEN = 5;
  public static final byte WHITE_KING = 6;

  public static final byte BLACK_PAWN = -1;
  public static final byte BLACK_KNIGHT = -2;
  public static final byte BLACK_BISHOP = -3;
  public static final byte BLACK_ROOK = -4;
  public static final byte BLACK_QUEEN = -5;
  public static final byte BLACK_KING = -6;

  private boolean cheatsActivated;

  private byte[][] boardData;
  private MatchResult matchResult;
  private MatchRecord moveRegistry;
  private boolean whiteTurn;

  public ChessMatch() {
    initialize(true);
  }

  public ChessMatch(Boolean cheatPermission) {
    initialize(cheatPermission);
  }
  
  public void setCheats(boolean cheatPermision){
    cheatsActivated = cheatPermision;
  }
  
  public void move(int fromColumn, int fromRow, int toColumn, int toRow) {
    if (!isMoveLegal(fromColumn, fromRow, toColumn, toRow))
      throw new IllegalArgumentException("Invalid move");
  }
  
  public boolean isMoveLegal(int fromColumn, int fromRow, int toColumn, int toRow) {
    return true;
  }
  
  public boolean isWhiteAt(int row, int column) {
    return boardData[row][column] > 0;
  }
  
  public int[][] colorData() {
    int[][] colorData = new int[BOARD_SIZE][BOARD_SIZE];
    for (int c = 0; c < BOARD_SIZE; c++)
      for (int r = 0; r < BOARD_SIZE; r++)
    colorData[c][r] = boardData[c][r] == 0 ? 0 : boardData[c][r] > 0 ? 1 : -1;
    return colorData;
  }

  public ArrayList<Tile> validMoves(){
    
  }

  private void initialize(boolean cheatPermission){
    this.cheatsActivated = cheatPermission;
    matchResult = MatchResult.ONGOING;
    resetBoard();
    moveRegistry = new MatchRecord();
    whiteTurn = true;
  }

  public void resetBoard() {
    boardData = new byte[][] {
      {BLACK_ROOK, BLACK_KNIGHT, BLACK_BISHOP, BLACK_QUEEN, BLACK_KING, BLACK_BISHOP, BLACK_KNIGHT, BLACK_ROOK},
      {BLACK_PAWN, BLACK_PAWN,   BLACK_PAWN,   BLACK_PAWN,  BLACK_PAWN, BLACK_PAWN,   BLACK_PAWN,   BLACK_PAWN},
      {EMPTY,      EMPTY,        EMPTY,        EMPTY,       EMPTY,      EMPTY,        EMPTY,        EMPTY},
      {EMPTY,      EMPTY,        EMPTY,        EMPTY,       EMPTY,      EMPTY,        EMPTY,        EMPTY},
      {EMPTY,      EMPTY,        EMPTY,        EMPTY,       EMPTY,      EMPTY,        EMPTY,        EMPTY},
      {EMPTY,      EMPTY,        EMPTY,        EMPTY,       EMPTY,      EMPTY,        EMPTY,        EMPTY},
      {WHITE_PAWN, WHITE_PAWN,   WHITE_PAWN,   WHITE_PAWN,  WHITE_PAWN, WHITE_PAWN,   WHITE_PAWN,   WHITE_PAWN},
      {WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, WHITE_QUEEN, WHITE_KING ,WHITE_BISHOP ,WHITE_KNIGHT ,WHITE_ROOK}
    };
  }
}
