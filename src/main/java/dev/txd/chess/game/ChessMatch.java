package dev.txd.chess.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ChessMatch {
  private boolean enforceRules = true;

  private Board board;
  private ArrayList<Integer> capturedPieces;
  private Map<Byte, ArrayList<Byte>> validMovesCache = new HashMap<>();
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

  private void switchTurnInternal() {
    whiteTurn = !whiteTurn;
  }

  private void rebuildValidMovesCache() {
    validMovesCache.clear();
    for (int c = 0; c < Board.SIZE; c++) {
      for (int r = 0; r < Board.SIZE; r++) {
        byte from = PackedTile.encode(c, r);
        validMovesCache.put(from, validMoves(from));
      }
    }
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
    byte from = PackedTile.fromTile(move.from());
    byte to = PackedTile.fromTile(move.to());
    move(from, to);
  }

  private void move(byte from, byte to) {
    if (enforceRules && !isMoveLegal(from, to))
      throw new IllegalArgumentException("Invalid move");

    int fromColumn = PackedTile.column(from);
    int fromRow = PackedTile.row(from);
    int toColumn = PackedTile.column(to);
    int toRow = PackedTile.row(to);

    if (board.pieceAt(toColumn, toRow) != Board.EMPTY)
      capturedPieces.add(board.pieceAt(toColumn, toRow));

    board.setPieceAt(toColumn, toRow, board.pieceAt(fromColumn, fromRow));
    board.setPieceAt(fromColumn, fromRow, Board.EMPTY);
    switchTurnInternal();
    matchRecord.addMoveRecord(new Move(PackedTile.toTile(from), PackedTile.toTile(to)), board);
  }

  public boolean isMoveLegal(Move move) {
    move.validate();
    return isMoveLegal(PackedTile.fromTile(move.from()), PackedTile.fromTile(move.to()));
  }

  private boolean isMoveLegal(byte from, byte to) {
    int fromColumn = PackedTile.column(from);
    int fromRow = PackedTile.row(from);
    int toColumn = PackedTile.column(to);
    int toRow = PackedTile.row(to);

    int movedPiece = board.pieceAt(fromColumn, fromRow);
    if (movedPiece == Board.EMPTY)
      return false;

    boolean isWhite = movedPiece > 0;
    if (isWhite != whiteTurn)
      return false;

    int targetPiece = board.pieceAt(toColumn, toRow);
    if (targetPiece != Board.EMPTY && (targetPiece > 0) == isWhite)
      return false;

    int distX = toColumn - fromColumn;
    int distY = toRow - fromRow;

    PieceMoveRules.MoveContext context = new PieceMoveRules.MoveContext(from, to, distX, distY, isWhite, targetPiece, board,
        this::isPathClear);

    return PieceMoveRules.ruleForPiece(Math.abs(movedPiece)).test(context);
  }

  public ArrayList<Tile> validMoves(Tile from) {
    ArrayList<Byte> packedMoves = validMoves(PackedTile.fromTile(from));
    ArrayList<Tile> validMoves = new ArrayList<>(packedMoves.size());
    for (byte packedMove : packedMoves)
      validMoves.add(PackedTile.toTile(packedMove));
    return validMoves;
  }

  private ArrayList<Byte> validMoves(byte from) {
    ArrayList<Byte> validMoves = new ArrayList<>();
    for (int c = 0; c < Board.SIZE; c++){
      for (int r = 0; r < Board.SIZE; r++) {
        byte to = PackedTile.encode(c, r);
        if (isMoveLegal(from, to))
          validMoves.add(to);
      }
    }
    return validMoves;
  }

  public int pieceCapturedFromMove(Move move) {
    move.validate();
    byte to = PackedTile.fromTile(move.to());
    if (isMoveLegal(move)){
      return board.pieceAt(PackedTile.column(to), PackedTile.row(to));
    } else {
      return -1;
    }
  }

  public ArrayList<Tile> validMoves(int column, int row) {
    return validMoves(new Tile(column, row));
  }

  public Optional<Tile[]> check(boolean forWhite) {
    return check(forWhite, board.getKingPacked(forWhite));
  }

  private Optional<Tile[]> check(boolean forWhite, byte kingTile){
    ArrayList<Tile> possibleThreats = new ArrayList<>();
    for (int c = 0; c < Board.SIZE; c++){
      for (int r = 0; r < Board.SIZE; r++) {
        byte from = PackedTile.encode(c, r);
        if (isMoveLegal(from, kingTile))
          possibleThreats.add(PackedTile.toTile(from));
      }
    }
    if (possibleThreats.size() > 0)
      return Optional.of(possibleThreats.toArray(new Tile[possibleThreats.size()]));
    return Optional.empty();
  }

  public Optional<Tile[]> checkmate(boolean forWhite) {
    if(forWhite != whiteTurn)
      throw new IllegalStateException("Cannot check for checkmate of the player whose turn it is not");
    byte kingTile = board.getKingPacked(forWhite);
    ArrayList<Byte> possibleKingPositions = validMoves(kingTile);
    boolean kingIsObligatedToMove = false;
    if (board.piecesCount(forWhite) != 1 && board.pawnsToPromote(forWhite).orElse(new Tile[0]).length == 0) {
      Tile[] pieces = board.pieces(forWhite);
      int validMovesCount = 0;
      for (Tile piece : pieces) {
        if (piece == null)
          continue;
        validMovesCount += validMoves(PackedTile.fromTile(piece)).size();
        if (validMovesCount - validMoves(kingTile).size() > 0) {
          kingIsObligatedToMove = true;
        }
      }
    }
    if (kingIsObligatedToMove)
      possibleKingPositions.add(kingTile);

    ArrayList<Tile> possibleThreats = new ArrayList<>();
    for(byte possibleKingPosition : possibleKingPositions){
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

  private boolean isPathClear(byte from, byte to) {
    int fromColumn = PackedTile.column(from);
    int fromRow = PackedTile.row(from);
    int toColumn = PackedTile.column(to);
    int toRow = PackedTile.row(to);

    if(Math.abs(toColumn - fromColumn) <= 1 && Math.abs(toRow - fromRow) <= 1)
      return true; 

    int colStep = Integer.compare(toColumn, fromColumn);
    int rowStep = Integer.compare(toRow, fromRow);

    int currentCol = fromColumn + colStep;
    int currentRow = fromRow + rowStep;

    while (currentCol != toColumn || currentRow != toRow) {
      if (board.pieceAt(currentCol, currentRow) != Board.EMPTY)
        return false;
      currentCol += colStep;
      currentRow += rowStep;
    }
    return true;
  }
}
