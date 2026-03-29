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
  private boolean whiteCastleKingSide;
  private boolean whiteCastleQueenSide;
  private boolean blackCastleKingSide;
  private boolean blackCastleQueenSide;
  private byte enPassantTarget = PackedTile.NO_TILE;
  private int halfMoveClock;
  private Map<String, Integer> positionCounts = new HashMap<>();

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
    halfMoveClock = 0;
    resetSpecialMoveStateFromBoard();
    rebuildValidMovesCache();
    resetPositionTracking();
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
    rebuildValidMovesCache();
    recordCurrentPosition();
    refreshMatchStatus();
  }

  private void switchTurnInternal() {
    whiteTurn = !whiteTurn;
  }

  private void rebuildValidMovesCache() {
    validMovesCache.clear();
    for (int fc = 0; fc < Board.SIZE; fc++) {
      for (int fr = 0; fr < Board.SIZE; fr++) {
        byte from = PackedTile.encode(fc, fr);
        PackedTile.validate(from);
        int movedPiece = board.pieceAt(PackedTile.column(from), PackedTile.row(from));
        if (movedPiece == Board.EMPTY)
          continue;
        boolean isMovingWhite = movedPiece > 0;
        if (isMovingWhite != whiteTurn)
          continue;
        for (int tc = 0; tc < Board.SIZE; tc++) {
          for (int tr = 0; tr < Board.SIZE; tr++) {
            byte to = PackedTile.encode(tc, tr);
            PackedTile.validate(to);

            if(matchesPieceRule(from, to, false) && resolvesCheck(isMovingWhite, from, to)){
              validMovesCache.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
            }
          }
        }
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
    matchStatus = MatchStatus.ONGOING;
    halfMoveClock = 0;
    resetSpecialMoveStateFromBoard();
    rebuildValidMovesCache();
    resetPositionTracking();
    refreshMatchStatus();
    matchRecord.addMoveRecord(newBoard);
  }

  public void move(Move move) {
    move.validate();
    byte from = PackedTile.fromTile(move.from());
    byte to = PackedTile.fromTile(move.to());
    move(from, to);
  }

  private void move(byte from, byte to) {
    if (matchStatus != MatchStatus.ONGOING)
      throw new IllegalStateException("Cannot move after the match is finished");

    if (enforceRules && !isMoveLegal(from, to))
      throw new IllegalArgumentException("Invalid move");

    MoveStateTransition.Outcome transition = MoveStateTransition.apply(board, from, to, toTransitionState());
    int capturedPiece = transition.capturedPiece();
    applyTransitionState(transition.state());

    if (capturedPiece != Board.EMPTY)
      capturedPieces.add(capturedPiece);

    switchTurnInternal();
    rebuildValidMovesCache();
    recordCurrentPosition();
    refreshMatchStatus();
    matchRecord.addMoveRecord(from, to, null);
  }

  public boolean isMoveLegal(Move move) {
    move.validate();
    return isMoveLegal(PackedTile.fromTile(move.from()), PackedTile.fromTile(move.to()));
  }

  private boolean isMoveLegal(byte from, byte to) {
    ArrayList<Byte> moves = validMovesCache.get(from);
    return !enforceRules || (moves != null && moves.contains(to));
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
    byte[] opponentPieces = board.piecesPacked(!forWhite);
    for (byte from : opponentPieces) {
      if (matchesPieceRule(from, kingTile, true))
        possibleThreats.add(PackedTile.toTile(from));
    }
    if (!possibleThreats.isEmpty())
      return Optional.of(possibleThreats.toArray(new Tile[possibleThreats.size()]));
    return Optional.empty();
  }

  public Optional<Tile[]> checkmate(boolean forWhite) {
    if(forWhite != whiteTurn)
      throw new IllegalStateException("Cannot check for checkmate of the player whose turn it is not");

    byte kingTile = board.getKingPacked(forWhite);
    Optional<Tile[]> currentThreats = check(forWhite, kingTile);
    if (currentThreats.isEmpty())
      return Optional.empty();

    if (hasAnyLegalResponse(forWhite))
      return Optional.empty();

    return currentThreats;
  }

  public boolean stalemate(boolean forWhite) {
    if(forWhite != whiteTurn)
      throw new IllegalStateException("Cannot check for stalemate of the player whose turn it is not");

    if (check(forWhite).isPresent())
      return false;

    return !hasAnyLegalResponse(forWhite);
  }

  public boolean drawByFiftyMoveRule() {
    return halfMoveClock >= 100;
  }

  public boolean drawByThreefoldRepetition() {
    String key = buildPositionKey();
    return positionCounts.getOrDefault(key, 0) >= 3;
  }

  public boolean drawByInsufficientMaterial() {
    int whiteKnights = 0;
    int whiteBishops = 0;
    int blackKnights = 0;
    int blackBishops = 0;
    int whiteBishopColorParity = -1;
    int blackBishopColorParity = -1;

    for (int c = 0; c < Board.SIZE; c++) {
      for (int r = 0; r < Board.SIZE; r++) {
        int piece = board.pieceAt(c, r);
        if (piece == Board.EMPTY || Math.abs(piece) == Board.KING)
          continue;

        int absPiece = Math.abs(piece);
        boolean isWhite = piece > 0;
        if (absPiece == Board.PAWN || absPiece == Board.ROOK || absPiece == Board.QUEEN)
          return false;

        if (absPiece == Board.KNIGHT) {
          if (isWhite)
            whiteKnights++;
          else
            blackKnights++;
          continue;
        }

        if (absPiece == Board.BISHOP) {
          int parity = (c + r) % 2;
          if (isWhite) {
            whiteBishops++;
            whiteBishopColorParity = parity;
          } else {
            blackBishops++;
            blackBishopColorParity = parity;
          }
        }
      }
    }

    int whiteMinor = whiteKnights + whiteBishops;
    int blackMinor = blackKnights + blackBishops;
    int totalMinor = whiteMinor + blackMinor;

    if (totalMinor <= 1)
      return true;

    if (whiteMinor == 2 && whiteKnights == 2 && blackMinor == 0)
      return true;
    if (blackMinor == 2 && blackKnights == 2 && whiteMinor == 0)
      return true;

    if (totalMinor == 2) {
      boolean oneKnightEach = whiteKnights == 1 && blackKnights == 1 && whiteBishops == 0 && blackBishops == 0;
      if (oneKnightEach)
        return true;

      boolean oneBishopEachSameColor = whiteBishops == 1 && blackBishops == 1 && whiteKnights == 0 && blackKnights == 0
          && whiteBishopColorParity == blackBishopColorParity;
      if (oneBishopEachSameColor)
        return true;
    }

    return false;
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

  private boolean matchesPieceRule(byte from, byte to, boolean attackOnly) {
    if (from == to)
      return false;

    int movedPiece = board.pieceAt(PackedTile.column(from), PackedTile.row(from));
    if (movedPiece == Board.EMPTY)
      return false;

    boolean isWhite = movedPiece > 0;
    int targetPiece = board.pieceAt(PackedTile.column(to), PackedTile.row(to));
    if (targetPiece != Board.EMPTY && (targetPiece > 0) == isWhite)
      return false;

    int distX = PackedTile.column(to) - PackedTile.column(from);
    int distY = PackedTile.row(to) - PackedTile.row(from);

    PieceMoveRules.MoveContext context = new PieceMoveRules.MoveContext(from, to, distX, distY, isWhite, targetPiece, board,
        this::isPathClear);

    if (PieceMoveRules.ruleForPiece(Math.abs(movedPiece)).test(context))
      return true;

    if (attackOnly)
      return false;

    if (isCastlingMove(from, to, movedPiece))
      return isCastlingLegal(from, to, isWhite);

    if (isEnPassantMove(from, to, movedPiece, targetPiece))
      return true;

    return false;
  }

  private boolean resolvesCheck(boolean forWhite, byte from, byte to) {
    boolean previousWhiteCastleKingSide = whiteCastleKingSide;
    boolean previousWhiteCastleQueenSide = whiteCastleQueenSide;
    boolean previousBlackCastleKingSide = blackCastleKingSide;
    boolean previousBlackCastleQueenSide = blackCastleQueenSide;
    byte previousEnPassantTarget = enPassantTarget;

    int fromColumn = PackedTile.column(from);
    int fromRow = PackedTile.row(from);
    int toColumn = PackedTile.column(to);
    int toRow = PackedTile.row(to);

    int movingPiece = board.pieceAt(fromColumn, fromRow);
    int targetPiece = board.pieceAt(toColumn, toRow);
    boolean isEnPassant = isEnPassantMove(from, to, movingPiece, targetPiece);
    int capturedPiece = isEnPassant ? board.pieceAt(toColumn, fromRow) : targetPiece;

    if (isEnPassant)
      board.setPieceAt(toColumn, fromRow, Board.EMPTY);

    board.setPieceAt(toColumn, toRow, movingPiece);
    board.setPieceAt(fromColumn, fromRow, Board.EMPTY);

    boolean castling = isCastlingMove(from, to, movingPiece);
    if (castling)
      moveCastlingRook(from, to);

    boolean movingWhite = movingPiece > 0;
    if (Math.abs(movingPiece) == Board.PAWN && reachesPromotionRank(movingWhite, toRow))
      board.setPieceAt(toColumn, toRow, movingWhite ? Board.QUEEN : -Board.QUEEN);

    boolean stillInCheck = check(forWhite).isPresent();

    if (castling)
      undoCastlingRook(from, to);

    board.setPieceAt(fromColumn, fromRow, movingPiece);
    if (isEnPassant) {
      board.setPieceAt(toColumn, toRow, Board.EMPTY);
      board.setPieceAt(toColumn, fromRow, capturedPiece);
    } else {
      board.setPieceAt(toColumn, toRow, capturedPiece);
    }

    whiteCastleKingSide = previousWhiteCastleKingSide;
    whiteCastleQueenSide = previousWhiteCastleQueenSide;
    blackCastleKingSide = previousBlackCastleKingSide;
    blackCastleQueenSide = previousBlackCastleQueenSide;
    enPassantTarget = previousEnPassantTarget;

    return !stillInCheck;
  }

  private boolean hasAnyLegalResponse(boolean forWhite) {
    byte[] defenderPieces = board.piecesPacked(forWhite);
    for (byte from : defenderPieces) {
      ArrayList<Byte> candidateMoves = validMoves(from);
      for (byte to : candidateMoves) {
        if (resolvesCheck(forWhite, from, to))
          return true;
      }
    }
    return false;
  }

  private void resetSpecialMoveStateFromBoard() {
    applyTransitionState(MoveStateTransition.initialState(board, halfMoveClock));
  }

  private void resetPositionTracking() {
    positionCounts.clear();
    recordCurrentPosition();
  }

  private void recordCurrentPosition() {
    String key = buildPositionKey();
    positionCounts.merge(key, 1, Integer::sum);
  }

  private String buildPositionKey() {
    StringBuilder sb = new StringBuilder(128);
    byte[][] data = board.getBoardData();
    for (int c = 0; c < Board.SIZE; c++) {
      for (int r = 0; r < Board.SIZE; r++) {
        sb.append(data[c][r]).append(',');
      }
    }
    sb.append('|').append(whiteTurn ? 'w' : 'b');
    sb.append('|').append(whiteCastleKingSide ? 'K' : '-');
    sb.append(whiteCastleQueenSide ? 'Q' : '-');
    sb.append(blackCastleKingSide ? 'k' : '-');
    sb.append(blackCastleQueenSide ? 'q' : '-');
    sb.append('|').append(enPassantTarget & 0xFF);
    return sb.toString();
  }

  private void refreshMatchStatus() {
    if (checkmate(whiteTurn).isPresent()) {
      matchStatus = whiteTurn ? MatchStatus.BLACK_WINS : MatchStatus.WHITE_WINS;
      return;
    }

    if (stalemate(whiteTurn)
        || drawByInsufficientMaterial()
        || drawByFiftyMoveRule()
        || drawByThreefoldRepetition()) {
      matchStatus = MatchStatus.DRAW;
      return;
    }

    matchStatus = MatchStatus.ONGOING;
  }

  private boolean isCastlingMove(byte from, byte to, int movedPiece) {
    return Math.abs(movedPiece) == Board.KING
        && PackedTile.row(from) == PackedTile.row(to)
        && Math.abs(PackedTile.column(to) - PackedTile.column(from)) == 2;
  }

  private boolean isCastlingLegal(byte from, byte to, boolean isWhite) {
    int fromColumn = PackedTile.column(from);
    int fromRow = PackedTile.row(from);
    int toColumn = PackedTile.column(to);

    int expectedRow = isWhite ? 7 : 0;
    if (fromColumn != 4 || fromRow != expectedRow)
      return false;

    if (check(isWhite).isPresent())
      return false;

    boolean kingSide = toColumn == 6;
    boolean queenSide = toColumn == 2;
    if (!kingSide && !queenSide)
      return false;

    if (kingSide) {
      if (isWhite ? !whiteCastleKingSide : !blackCastleKingSide)
        return false;
      if (board.pieceAt(5, expectedRow) != Board.EMPTY || board.pieceAt(6, expectedRow) != Board.EMPTY)
        return false;
      if (board.pieceAt(7, expectedRow) != (isWhite ? Board.ROOK : -Board.ROOK))
        return false;
      if (isSquareAttacked(PackedTile.encode(5, expectedRow), !isWhite)
          || isSquareAttacked(PackedTile.encode(6, expectedRow), !isWhite))
        return false;
      return true;
    }

    if (isWhite ? !whiteCastleQueenSide : !blackCastleQueenSide)
      return false;
    if (board.pieceAt(1, expectedRow) != Board.EMPTY || board.pieceAt(2, expectedRow) != Board.EMPTY || board.pieceAt(3, expectedRow) != Board.EMPTY)
      return false;
    if (board.pieceAt(0, expectedRow) != (isWhite ? Board.ROOK : -Board.ROOK))
      return false;
    if (isSquareAttacked(PackedTile.encode(3, expectedRow), !isWhite)
        || isSquareAttacked(PackedTile.encode(2, expectedRow), !isWhite))
      return false;
    return true;
  }

  private boolean isSquareAttacked(byte tile, boolean byWhite) {
    byte[] pieces = board.piecesPacked(byWhite);
    for (byte from : pieces) {
      if (matchesPieceRule(from, tile, true))
        return true;
    }
    return false;
  }

  private boolean isEnPassantMove(byte from, byte to, int movedPiece, int targetPiece) {
    if (Math.abs(movedPiece) != Board.PAWN)
      return false;
    if (targetPiece != Board.EMPTY)
      return false;
    if (enPassantTarget == PackedTile.NO_TILE || to != enPassantTarget)
      return false;

    int distX = PackedTile.column(to) - PackedTile.column(from);
    int distY = PackedTile.row(to) - PackedTile.row(from);
    int direction = movedPiece > 0 ? -1 : 1;
    if (Math.abs(distX) != 1 || distY != direction)
      return false;

    int capturedPawn = board.pieceAt(PackedTile.column(to), PackedTile.row(from));
    return capturedPawn == (movedPiece > 0 ? -Board.PAWN : Board.PAWN);
  }

  private void moveCastlingRook(byte from, byte to) {
    int row = PackedTile.row(from);
    int toColumn = PackedTile.column(to);

    if (toColumn == 6) {
      int rookPiece = board.pieceAt(7, row);
      board.setPieceAt(5, row, rookPiece);
      board.setPieceAt(7, row, Board.EMPTY);
    } else if (toColumn == 2) {
      int rookPiece = board.pieceAt(0, row);
      board.setPieceAt(3, row, rookPiece);
      board.setPieceAt(0, row, Board.EMPTY);
    }
  }

  private void undoCastlingRook(byte from, byte to) {
    int row = PackedTile.row(from);
    int toColumn = PackedTile.column(to);

    if (toColumn == 6) {
      int rookPiece = board.pieceAt(5, row);
      board.setPieceAt(7, row, rookPiece);
      board.setPieceAt(5, row, Board.EMPTY);
    } else if (toColumn == 2) {
      int rookPiece = board.pieceAt(3, row);
      board.setPieceAt(0, row, rookPiece);
      board.setPieceAt(3, row, Board.EMPTY);
    }
  }

  private boolean reachesPromotionRank(boolean isWhite, int row) {
    return isWhite ? row == 0 : row == 7;
  }

  private MoveStateTransition.State toTransitionState() {
    return new MoveStateTransition.State(
        whiteCastleKingSide,
        whiteCastleQueenSide,
        blackCastleKingSide,
        blackCastleQueenSide,
        enPassantTarget,
        halfMoveClock);
  }

  private void applyTransitionState(MoveStateTransition.State state) {
    whiteCastleKingSide = state.whiteCastleKingSide();
    whiteCastleQueenSide = state.whiteCastleQueenSide();
    blackCastleKingSide = state.blackCastleKingSide();
    blackCastleQueenSide = state.blackCastleQueenSide();
    enPassantTarget = state.enPassantTarget();
    halfMoveClock = state.halfMoveClock();
  }
}
