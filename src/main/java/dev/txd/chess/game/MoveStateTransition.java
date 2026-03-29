package dev.txd.chess.game;

final class MoveStateTransition {

  record State(boolean whiteCastleKingSide, boolean whiteCastleQueenSide, boolean blackCastleKingSide,
      boolean blackCastleQueenSide, byte enPassantTarget, int halfMoveClock) {
  }

  record Outcome(int movingPiece, int capturedPiece, State state) {
  }

  private MoveStateTransition() {
  }

  static State initialState(Board board, int halfMoveClock) {
    boolean whiteCastleKingSide = board.pieceAt(4, 7) == Board.KING && board.pieceAt(7, 7) == Board.ROOK;
    boolean whiteCastleQueenSide = board.pieceAt(4, 7) == Board.KING && board.pieceAt(0, 7) == Board.ROOK;
    boolean blackCastleKingSide = board.pieceAt(4, 0) == -Board.KING && board.pieceAt(7, 0) == -Board.ROOK;
    boolean blackCastleQueenSide = board.pieceAt(4, 0) == -Board.KING && board.pieceAt(0, 0) == -Board.ROOK;
    return new State(whiteCastleKingSide, whiteCastleQueenSide, blackCastleKingSide, blackCastleQueenSide,
        PackedTile.NO_TILE, halfMoveClock);
  }

  static Outcome apply(Board board, byte from, byte to, State state) {
    int fromColumn = PackedTile.column(from);
    int fromRow = PackedTile.row(from);
    int toColumn = PackedTile.column(to);
    int toRow = PackedTile.row(to);

    int movingPiece = board.pieceAt(fromColumn, fromRow);
    int targetPiece = board.pieceAt(toColumn, toRow);
    boolean movingWhite = movingPiece > 0;

    boolean isEnPassant = isEnPassantMove(board, from, to, movingPiece, targetPiece, state.enPassantTarget());
    int capturedPiece = targetPiece;
    if (isEnPassant) {
      int capturedPawnRow = fromRow;
      capturedPiece = board.pieceAt(toColumn, capturedPawnRow);
      board.setPieceAt(toColumn, capturedPawnRow, Board.EMPTY);
    }

    board.setPieceAt(toColumn, toRow, movingPiece);
    board.setPieceAt(fromColumn, fromRow, Board.EMPTY);

    if (isCastlingMove(from, to, movingPiece))
      moveCastlingRook(board, from, to);

    int pieceAfterMove = board.pieceAt(toColumn, toRow);
    if (Math.abs(pieceAfterMove) == Board.PAWN && reachesPromotionRank(movingWhite, toRow))
      board.setPieceAt(toColumn, toRow, movingWhite ? Board.QUEEN : -Board.QUEEN);

    int halfMoveClock = (capturedPiece != Board.EMPTY || Math.abs(movingPiece) == Board.PAWN)
        ? 0
        : state.halfMoveClock() + 1;

    State updatedState = new State(
        state.whiteCastleKingSide(),
        state.whiteCastleQueenSide(),
        state.blackCastleKingSide(),
        state.blackCastleQueenSide(),
        PackedTile.NO_TILE,
        halfMoveClock);

    updatedState = updateCastlingRights(updatedState, fromColumn, fromRow, toColumn, toRow, movingPiece, capturedPiece);
    updatedState = updateEnPassantTarget(updatedState, fromColumn, fromRow, toColumn, toRow, movingPiece);

    return new Outcome(movingPiece, capturedPiece, updatedState);
  }

  private static boolean isCastlingMove(byte from, byte to, int movedPiece) {
    return Math.abs(movedPiece) == Board.KING
        && PackedTile.row(from) == PackedTile.row(to)
        && Math.abs(PackedTile.column(to) - PackedTile.column(from)) == 2;
  }

  private static void moveCastlingRook(Board board, byte from, byte to) {
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

  private static boolean isEnPassantMove(Board board, byte from, byte to, int movedPiece, int targetPiece,
      byte enPassantTarget) {
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

  private static State updateEnPassantTarget(State state, int fromColumn, int fromRow, int toColumn, int toRow,
      int movingPiece) {
    byte enPassantTarget = PackedTile.NO_TILE;
    if (Math.abs(movingPiece) == Board.PAWN && Math.abs(toRow - fromRow) == 2 && fromColumn == toColumn) {
      int passedRow = (fromRow + toRow) / 2;
      enPassantTarget = PackedTile.encode(fromColumn, passedRow);
    }

    return new State(
        state.whiteCastleKingSide(),
        state.whiteCastleQueenSide(),
        state.blackCastleKingSide(),
        state.blackCastleQueenSide(),
        enPassantTarget,
        state.halfMoveClock());
  }

  private static State updateCastlingRights(State state, int fromColumn, int fromRow, int toColumn, int toRow,
      int movingPiece, int capturedPiece) {
    boolean whiteCastleKingSide = state.whiteCastleKingSide();
    boolean whiteCastleQueenSide = state.whiteCastleQueenSide();
    boolean blackCastleKingSide = state.blackCastleKingSide();
    boolean blackCastleQueenSide = state.blackCastleQueenSide();

    if (movingPiece == Board.KING) {
      whiteCastleKingSide = false;
      whiteCastleQueenSide = false;
    } else if (movingPiece == -Board.KING) {
      blackCastleKingSide = false;
      blackCastleQueenSide = false;
    }

    if (movingPiece == Board.ROOK) {
      if (fromColumn == 0 && fromRow == 7)
        whiteCastleQueenSide = false;
      if (fromColumn == 7 && fromRow == 7)
        whiteCastleKingSide = false;
    } else if (movingPiece == -Board.ROOK) {
      if (fromColumn == 0 && fromRow == 0)
        blackCastleQueenSide = false;
      if (fromColumn == 7 && fromRow == 0)
        blackCastleKingSide = false;
    }

    if (capturedPiece == Board.ROOK) {
      if (toColumn == 0 && toRow == 7)
        whiteCastleQueenSide = false;
      if (toColumn == 7 && toRow == 7)
        whiteCastleKingSide = false;
    } else if (capturedPiece == -Board.ROOK) {
      if (toColumn == 0 && toRow == 0)
        blackCastleQueenSide = false;
      if (toColumn == 7 && toRow == 0)
        blackCastleKingSide = false;
    }

    return new State(
        whiteCastleKingSide,
        whiteCastleQueenSide,
        blackCastleKingSide,
        blackCastleQueenSide,
        state.enPassantTarget(),
        state.halfMoveClock());
  }

  private static boolean reachesPromotionRank(boolean isWhite, int row) {
    return isWhite ? row == 0 : row == 7;
  }
}
