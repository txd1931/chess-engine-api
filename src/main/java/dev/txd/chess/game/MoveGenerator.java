package dev.txd.chess.game;

import java.util.List;

@FunctionalInterface
public interface MoveGenerator {
  List<Move> generateMoves(Board board, Tile tile);
}
