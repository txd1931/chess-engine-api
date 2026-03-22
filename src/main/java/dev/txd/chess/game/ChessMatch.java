package dev.txd.chess.game;

import java.util.ArrayList;

public final class ChessMatch {

  private boolean enforceRules = true;

  private Board board;
  private MatchStatus matchStatus;
  private MatchRecord matchRecord;
  private boolean whiteTurn;

  public ChessMatch(boolean enforceRules, boolean trackTurnDuration) {
    this.enforceRules = enforceRules;
    matchStatus = MatchStatus.ONGOING;
    board = new Board();
    board.setupStartingBoard();
    matchRecord = new MatchRecord(trackTurnDuration);
    whiteTurn = true;
  }

  public void setEnforceRules(boolean enforceRules) {
    this.enforceRules = enforceRules;
  }

  public boolean getEnforceRules() {
    return enforceRules;
  }

  public Board getBoard() {
    return board;
  }

  public MatchRecord getMatchRecord() {
    return matchRecord;
  }

  public void switchTurn() {
    if (enforceRules)
      throw new UnsupportedOperationException("Direct board changes are not allowed when rules enforcement is enabled");
    whiteTurn = !whiteTurn;
  }

  public boolean isWhiteTurn() {
    return whiteTurn;
  }

  public MatchStatus getStatus() {
    return matchStatus;
  }

  public void resign() {
    matchStatus = whiteTurn ? MatchStatus.BLACK_WINS : MatchStatus.WHITE_WINS;
  }

  public void updateBoard(Board newBoard){
    if (enforceRules)
      throw new UnsupportedOperationException("Direct board changes are not allowed when rules enforcement is enabled");
    if (newBoard == null)
      throw new IllegalArgumentException("New board cannot be null");
    board = newBoard;
    matchRecord.addMoveRecord(newBoard);
  }

  public void move(Move move) {
    if (move == null || move.from() == null || move.to() == null)
      throw new IllegalArgumentException("Move and its tiles cannot be null");
    if (enforceRules)
      if (!isMoveLegal(move))
        throw new IllegalArgumentException("Invalid move");
    board.setPieceAt(move.from().row(), move.from().column(), Board.EMPTY);
    board.setPieceAt(move.to().row(), move.to().column(), board.pieceAt(move.from().row(), move.from().column()));
    switchTurn();
    matchRecord.addMoveRecord(move, board);
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
    for (int c = 0; c < Board.SIZE; c++)
      for (int r = 0; r < Board.SIZE; r++) {
        Tile to = new Tile(r, c);
        Move move = new Move(from, to);
        if (isMoveLegal(move))
          validMoves.add(to);
      }
    return validMoves;
  }

  private boolean isInBounds(Tile tile) {
    return tile.column() >= 0 && tile.column() < Board.SIZE && tile.row() >= 0 && tile.row() < Board.SIZE;
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
