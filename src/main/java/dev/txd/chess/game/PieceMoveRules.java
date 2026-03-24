package dev.txd.chess.game;

import java.util.function.Predicate;

final class PieceMoveRules {

  @FunctionalInterface
  public interface PathClearChecker {
    boolean test(byte fromTile, byte toTile);
  }

  public record MoveContext(byte fromTile, byte toTile, int distX, int distY, boolean isWhite, int targetPiece, Board board,
      PathClearChecker pathClearChecker) {
  }

  public static final Predicate<MoveContext> PAWN_RULE = ctx -> {
    int direction = ctx.isWhite() ? -1 : 1;
    int startRow = ctx.isWhite() ? 6 : 1;
    int fromRow = PackedTile.row(ctx.fromTile());
    int fromColumn = PackedTile.column(ctx.fromTile());

    boolean oneStepForward = ctx.distX() == 0 && ctx.distY() == direction && ctx.targetPiece() == Board.EMPTY;
    boolean twoStepForward = ctx.distX() == 0 && ctx.distY() == (2 * direction) && fromRow == startRow
        && ctx.targetPiece() == Board.EMPTY
      && ctx.board().pieceAt(fromColumn, fromRow + direction) == Board.EMPTY;
    boolean diagonalCapture = Math.abs(ctx.distX()) == 1 && ctx.distY() == direction && ctx.targetPiece() != Board.EMPTY
        && (ctx.targetPiece() > 0) != ctx.isWhite();

    return oneStepForward || twoStepForward || diagonalCapture;
  };

  public static final Predicate<MoveContext> KNIGHT_RULE = ctx -> (Math.abs(ctx.distX()) == 2
      && Math.abs(ctx.distY()) == 1) || (Math.abs(ctx.distX()) == 1 && Math.abs(ctx.distY()) == 2);

  public static final Predicate<MoveContext> BISHOP_RULE = ctx -> Math.abs(ctx.distX()) == Math.abs(ctx.distY())
      && ctx.pathClearChecker().test(ctx.fromTile(), ctx.toTile());

  public static final Predicate<MoveContext> ROOK_RULE = ctx -> (ctx.distX() == 0 || ctx.distY() == 0)
      && ctx.pathClearChecker().test(ctx.fromTile(), ctx.toTile());

  /*public static final Predicate<MoveContext> QUEEN_RULE = ctx -> ((Math.abs(ctx.distX()) == Math.abs(ctx.distY()))
      || (ctx.distX() == 0 || ctx.distY() == 0)) && ctx.pathClearChecker().test(ctx.move());
  */
  public static final Predicate<MoveContext> QUEEN_RULE = ctx -> ROOK_RULE.test(ctx) || BISHOP_RULE.test(ctx);

  public static final Predicate<MoveContext> KING_RULE = ctx -> Math.abs(ctx.distX()) <= 1 && Math.abs(ctx.distY()) <= 1
      && (Math.abs(ctx.distX()) + Math.abs(ctx.distY()) > 0);


  public static Predicate<MoveContext> ruleForPiece(int absPieceType) {
    return switch (absPieceType) {
      case Board.PAWN -> PAWN_RULE;
      case Board.KNIGHT -> KNIGHT_RULE;
      case Board.BISHOP -> BISHOP_RULE;
      case Board.ROOK -> ROOK_RULE;
      case Board.QUEEN -> QUEEN_RULE;
      case Board.KING -> KING_RULE;
      default -> ctx -> false;
    };
  }
}
