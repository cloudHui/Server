package web.minigame;

/**
 * 五子棋 15x15，1=黑 2=白，黑先。
 */
public class GomokuBoard {
	public static final int SIZE = 15;
	public static final int EMPTY = 0;
	public static final int BLACK = 1;
	public static final int WHITE = 2;

	private final int[][] cells = new int[SIZE][SIZE];
	private int turn = BLACK;
	private int winner = EMPTY;
	private boolean finished;
	private int moveCount;

	public int getTurn() {
		return turn;
	}

	public int getWinner() {
		return winner;
	}

	public boolean isFinished() {
		return finished;
	}

	public int[][] snapshot() {
		int[][] copy = new int[SIZE][SIZE];
		for (int i = 0; i < SIZE; i++) {
			System.arraycopy(cells[i], 0, copy[i], 0, SIZE);
		}
		return copy;
	}

	public boolean place(int x, int y, int color) {
		if (finished || color != turn) {
			return false;
		}
		if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || cells[y][x] != EMPTY) {
			return false;
		}
		cells[y][x] = color;
		moveCount++;
		if (checkWin(x, y, color)) {
			winner = color;
			finished = true;
		} else if (moveCount >= SIZE * SIZE) {
			finished = true;
			winner = EMPTY;
		} else {
			turn = color == BLACK ? WHITE : BLACK;
		}
		return true;
	}

	private boolean checkWin(int x, int y, int color) {
		return count(x, y, 1, 0, color) + count(x, y, -1, 0, color) >= 4
				|| count(x, y, 0, 1, color) + count(x, y, 0, -1, color) >= 4
				|| count(x, y, 1, 1, color) + count(x, y, -1, -1, color) >= 4
				|| count(x, y, 1, -1, color) + count(x, y, -1, 1, color) >= 4;
	}

	private int count(int x, int y, int dx, int dy, int color) {
		int n = 0;
		int cx = x + dx;
		int cy = y + dy;
		while (cx >= 0 && cx < SIZE && cy >= 0 && cy < SIZE && cells[cy][cx] == color) {
			n++;
			cx += dx;
			cy += dy;
		}
		return n;
	}
}
