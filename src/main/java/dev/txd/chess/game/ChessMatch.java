package dev.txd.chess.game;

import java.util.ArrayList;
import java.util.Optional;

public final class ChessMatch {
  private boolean enforceRules = true;

  private Board board;
  private ArrayList<Integer> capturedPieces;
  private MatchStatus matchStatus;
  private MatchRecord matchRecord;
  private boolean whiteTurn;
  
  public ChessMatch() {
    this(true, true);
  }

  public ChessMatch(boolean enforceRules, boolean trackTurnDuration) {
    this.enforceRules = enforceRules;
    capturedPieces = new ArrayList<>();
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
    move.validate();
    if (enforceRules)
      if (!isMoveLegal(move))
        throw new IllegalArgumentException("Invalid move");
    board.setPieceAt(move.from().row(), move.from().column(), Board.EMPTY);
    if (board.pieceAt(move.to().row(), move.to().column()) != Board.EMPTY)
      capturedPieces.add(board.pieceAt(move.to().row(), move.to().column()));
    board.setPieceAt(move.to().row(), move.to().column(), board.pieceAt(move.from().row(), move.from().column()));
    switchTurn();
    matchRecord.addMoveRecord(move, board);
  }

  public boolean isMoveLegal(Move move) {
    move.validate();
    Tile from = move.from();
    Tile to = move.to();

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
    from.validate();
    ArrayList<Tile> validMoves = new ArrayList<>();
    for (int c = 0; c < Board.SIZE; c++){
      for (int r = 0; r < Board.SIZE; r++) {
        Tile to = new Tile(c, r);
        Move move = new Move(from, to);
        if (isMoveLegal(move))
          validMoves.add(to);
      }
    }
    return validMoves;
  }

  public int pieceCapturedFromMove(Move move) {
    move.validate();
    if (isMoveLegal(move)){
      return board.pieceAt(move.to().row(), move.to().column());
    } else {
      return -1;
    }
  }

  public ArrayList<Tile> validMoves(int row, int column) {
    return validMoves(new Tile(column, row));
  }

  public Optional<Tile[]> check(boolean forWhite) {
    return check(forWhite, board.getKing(forWhite));
  }

  private Optional<Tile[]> check(boolean forWhite, Tile kingTile){
    ArrayList<Tile> possibleThreats = new ArrayList<>();
    for (int c = 0; c < Board.SIZE; c++){
      for (int r = 0; r < Board.SIZE; r++) {
        Tile from = new Tile(c, r);
        Move move = new Move(from, kingTile);
        if (isMoveLegal(move))
          possibleThreats.add(from);
      }
    }
    if (possibleThreats.size() > 0)
      return Optional.of(possibleThreats.toArray(new Tile[possibleThreats.size()]));
    return Optional.empty();
  }

  public Optional<Tile[]> checkmate(boolean forWhite) {
    if(forWhite != whiteTurn)
      throw new IllegalStateException("Cannot check for checkmate of the player whose turn it is not");
    Tile kingTile = board.getKing(forWhite);
    ArrayList<Tile> possibleKingPositions = validMoves(kingTile);
    boolean kingIsObligatedToMove = false;
    if (board.piecesCount(forWhite) != 1 && board.pawnsToPromote(forWhite).orElse(new Tile[0]).length == 0) {
      Tile[] pieces = board.pieces(forWhite);
      int validMovesCount = 0;
      for (Tile piece : pieces) {
        validMovesCount += validMoves(piece).size();
        if (validMovesCount - validMoves(kingTile).size() > 0) {
          kingIsObligatedToMove = true;
        }
      }
    }
    if (kingIsObligatedToMove)
      possibleKingPositions.add(kingTile);

    ArrayList<Tile> possibleThreats = new ArrayList<>();
    for(Tile possibleKingPosition : possibleKingPositions){
      Tile[] specificPossibleThreats  = check(forWhite, possibleKingPosition).orElse(new Tile[0]);
      for(Tile possibleThreat : specificPossibleThreats){
        possibleThreats.add(possibleThreat);
      }
    }
    if (possibleThreats.size() > 0)
      return Optional.of(possibleThreats.toArray(new Tile[possibleThreats.size()]));
    return Optional.empty();
  }

  public ArrayList<Integer> getCapturedPieces() {
    return capturedPieces;
  }

  private boolean isPathClear(Move move) {
    move.validate();
    Tile from = move.from();
    Tile to = move.to();

    if(Math.abs(to.column() - from.column()) <= 1 && Math.abs(to.row() - from.row()) <= 1)
      return true; 

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
