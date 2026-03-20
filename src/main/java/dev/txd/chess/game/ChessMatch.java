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
    board = new Board();
    board.resetBoard();
    matchRecord = new MatchRecord();
    whiteTurn = true;
  }

  public void setCheats(boolean cheatPermision) {
    enforceRules = cheatPermision;
  }

  public void move(Move move) {
    boolean isMoveLegal = move.isValid(this); 
    if (enforceRules)
      if (!isMoveLegal(move))
        throw new IllegalArgumentException("Invalid move");
    board.setPieceAt(move.from().row(), move.from().column(), Board.EMPTY);
    board.setPieceAt(move.to().row(), move.to().column(), board.pieceAt(move.from().row(), move.from().column()));
    matchRecord.addMoveRecord(move, isMoveLegal, board, System.currentTimeMillis());
  }

  public boolean isMoveLegal(Move move) {
    if (move == null || move.from() == null || move.to() == null)
      throw new IllegalArgumentException("Move and its tiles cannot be null");
    Tile from = move.from();
    Tile to = move.to();

    if (!isInBounds(from) || !isInBounds(to))
      return false;

    int movedPiece = board.pieceAt(from.row(), from.column()); 
    if (movedPiece == Board.EMPTY)
      return false;

    boolean isWhite = movedPiece > 0;
    if (isWhite != whiteTurn)
      return false;

    int targetPiece = board.pieceAt(to.row(), to.column());
    if (targetPiece != Board.EMPTY && (targetPiece > 0) == isWhite)
      return false;

    int distX = to.column() - from.column();
    int distY = to.row() - from.row();

    PieceMoveRules.MoveContext context = new PieceMoveRules.MoveContext(move, distX, distY, isWhite, targetPiece, board,
        this::isPathClear);

    return PieceMoveRules.ruleForPiece(Math.abs(movedPiece)).test(context);
  }

  public ArrayList<Tile> validMoves(Tile from) {
    ArrayList<Tile> validMoves = new ArrayList<>();

    for (int c = 0; c < Board.BOARD_SIZE; c++)
      for (int r = 0; r < Board.BOARD_SIZE; r++) {
        Tile to = new Tile(r, c);
        Move move = new Move(from, to);
        if (isMoveLegal(move))
          validMoves.add(to);
      }
    return validMoves;

  }

  private boolean isInBounds(Tile tile) {
    return tile.column() >= 0 && tile.column() < Board.BOARD_SIZE && tile.row() >= 0 && tile.row() < Board.BOARD_SIZE;
  }

  private boolean isPathClear(Move move) {
    Tile from = move.from();
    Tile to = move.to();
    int colStep = Integer.compare(to.column(), from.column());
    int rowStep = Integer.compare(to.row(), from.row());

    int currentCol = from.column() + colStep;
    int currentRow = from.row() + rowStep;

    while (currentCol != to.column() || currentRow != to.row()) {
      if (board.pieceAt(currentRow, currentCol) != Board.EMPTY)
        return false;
      currentCol += colStep;
      currentRow += rowStep;
    }
    return true;
  }
}
