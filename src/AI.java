import java.util.Random;

public class AI {

    // 定义棋盘格子的状态：0为空，正数为白棋，负数为黑棋
    private static final int EMPTY = 0;
    private static final int W_PAWN = 1, W_KNIGHT = 2, W_BISHOP = 3, W_ROOK = 4, W_QUEEN = 5, W_KING = 6;
    private static final int B_PAWN = -1, B_KNIGHT = -2, B_BISHOP = -3, B_ROOK = -4, B_QUEEN = -5, B_KING = -6;

    // 各个棋子的基础评估分值
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000;

    // 特殊情况的分值：杀棋分和将军奖励
    private static final int MATE_VALUE = 20000000;
    private static final int CHECK_BONUS = 150;

    // 置换表（Transposition Table）配置，用于存储已搜索过的盘面
    private static final int TT_SIZE = 1 << 20;
    private static final int TT_MASK = TT_SIZE - 1;

    private static long[] ttHashes = new long[TT_SIZE];
    private static int[] ttScores = new int[TT_SIZE];
    private static byte[] ttDepths = new byte[TT_SIZE];
    private static byte[] ttFlags = new byte[TT_SIZE]; // 标识分值类型：1代表精确值，2代表下界，3代表上界

    // Zobrist Hashing 用于快速生成盘面的唯一哈希值
    private static final long[][] ZOBRIST_KEYS = new long[64][13];
    private static final long SIDE_TO_MOVE_KEY;
    private static long currentBoardHash = 0;

    // 初始化 Zobrist 随机数序列
    static {
        Random rnd = new Random(123456789L);
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 13; j++) {
                ZOBRIST_KEYS[i][j] = rnd.nextLong();
            }
        }
        SIDE_TO_MOVE_KEY = rnd.nextLong();
    }

    // 棋子位置评估表 (Piece-Square Tables)，根据棋子所处位置进行加减分
    // 例如兵靠近对方底线分值更高，马在中心位置分值更高
    private static final int[][] PAWN_PST = {
            {0,  0,  0,  0,  0,  0,  0,  0},
            {50, 50, 50, 50, 50, 50, 50, 50},
            {10, 10, 20, 30, 30, 20, 10, 10},
            {5,  5,  10, 25, 25, 10, 5,  5},
            {0,  0,  5,  20, 20, 5,  0,  0},
            {5, -5, -10, 0,  0, -10, -5, 5},
            {5,  10, 10, -20, -20, 10, 10, 5},
            {0,  0,  0,  0,  0,  0,  0,  0}
    };
    private static final int[][] KNIGHT_PST = {
            {-50, -40, -30, -30, -30, -30, -40, -50},
            {-40, -20, 0,   0,   0,   0,   -20, -40},
            {-30, 0,   10,  15,  15,  10,  0,   -30},
            {-30, 5,   15,  20,  20,  15,  5,   -30},
            {-30, 0,   15,  20,  20,  15,  0,   -30},
            {-30, 5,   10,  15,  15,  10,  5,   -30},
            {-40, -20, 0,   5,   5,   0,   -20, -40},
            {-50, -40, -30, -30, -30, -30, -40, -50}
    };
    private static final int[][] BISHOP_PST = {
            {-20, -10, -10, -10, -10, -10, -10, -20},
            {-10, 0,   0,   0,   0,   0,   0,   -10},
            {-10, 0,   5,   10,  10,  5,   0,   -10},
            {-10, 5,   5,   10,  10,  5,   5,   -10},
            {-10, 0,   10,  10,  10,  10,  0,   -10},
            {-10, 10,  10,  10,  10,  10,  10,  -10},
            {-10, 5,   0,   0,   0,   0,   5,   -10},
            {-20, -10, -10, -10, -10, -10, -10, -20}
    };
    private static final int[][] ROOK_PST = {
            {0,  0,  0,  0,  0,  0,  0,  0},
            {5,  10, 10, 10, 10, 10, 10, 5},
            {-5, 0,  0,  0,  0,  0,  0,  -5},
            {-5, 0,  0,  0,  0,  0,  0,  -5},
            {-5, 0,  0,  0,  0,  0,  0,  -5},
            {-5, 0,  0,  0,  0,  0,  0,  -5},
            {-5, 0,  0,  0,  0,  0,  0,  -5},
            {0,  0,  0,  5,  5,  0,  0,  0}
    };
    private static final int[][] QUEEN_PST = {
            {-20, -10, -10, -5, -5, -10, -10, -20},
            {-10, 0,   0,   0,   0,   0,   0,   -10},
            {-10, 0,   5,   5,   5,   5,   0,   -10},
            {-5,  0,   5,   5,   5,   5,   0,   -5},
            {0,   0,   5,   5,   5,   5,   0,   0},
            {-10, 5,   5,   5,   5,   5,   0,   -10},
            {-10, 0,   5,   0,   0,   0,   0,   -10},
            {-20, -10, -10, -5, -5, -10, -10, -20}
    };
    private static final int[][] KING_PST = {
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-20, -30, -30, -40, -40, -30, -30, -20},
            {-10, -20, -20, -20, -20, -20, -20, -10},
            {20,  30,  10,  0,   0,   10,  30,  20},
            {20,  30,  30,  10,  10,  30,  30,  20}
    };
    //这些数值是网上找的不是自己瞎写的，前辈大师下棋得出的经验
    private static float moveDelay = 0f;
    private static final float DELAY_TIME = 1.5f;
    private static GameBoard.Move cachedMove = null;
    private static boolean isWaiting = false;

    // 外部调用入口：将字符串数组棋盘转为整型棋盘，通过搜索获取最优移动
    public static GameBoard.Move getBestMove(String[][] strBoard) {
        int[] board = convertToIntBoard(strBoard);
        currentBoardHash = computeFullHash(board);

        // 设置搜索深度为5层
        int bestMoveInt = findBestMoveInternal(board, 5);

        // 没找到招法，可能是被将死或者闷宫
        if (bestMoveInt == 0) return null;

        int from = (bestMoveInt >> 6) & 0x3F;
        int to = bestMoveInt & 0x3F;
        return new GameBoard.Move(from / 8, from % 8, to / 8, to % 8);
    }

    // 模拟思考延迟，避免AI瞬间秒走，提升体验
    public static void startMoveDelay(GameBoard.Move move) {
        if (move == null) return; // 防止外部传入null导致赋值异常
        cachedMove = move;
        moveDelay = DELAY_TIME;
        isWaiting = true;
    }

    // 在游戏渲染循环中检查延迟是否结束
    public static boolean checkDelay(float deltaTime) { // 由外部传入deltaTime，避免依赖Gdx崩溃
        if (!isWaiting) return false;
        moveDelay -= deltaTime;
        if (moveDelay <= 0) {
            isWaiting = false;
            return true;
        }
        return false;
    }

    public static GameBoard.Move getCachedMove() { return cachedMove; }
    public static void resetDelay() { moveDelay = 0f; cachedMove = null; isWaiting = false; }

    // AI搜索的第一层逻辑：遍历所有合法移动并调用Minimax
    private static int findBestMoveInternal(int[] board, int maxDepth) {
        int bestVal = Integer.MIN_VALUE + 1;
        int bestMove = 0;

        // 生成黑方AI的所有移动
        IntList moves = generateMoves(board, false);
        orderMoves(board, moves); // 移动排序优化剪枝效率

        for (int i = 0; i < moves.size; i++) {
            int move = moves.get(i);
            long undoInfo = makeMove(board, move);

            // 走完后检查自己的王是否在对方火力下，不能送王
            if (isSquareAttacked(board, findKing(board, false), true)) {
                unmakeMove(board, move, undoInfo);
                continue;
            }

            // 进入递归搜索
            int val = minimax(board, maxDepth - 1, Integer.MIN_VALUE + 100, Integer.MAX_VALUE - 100, true);

            unmakeMove(board, move, undoInfo);

            if (val > bestVal) {
                bestVal = val;
                bestMove = move;
            }
        }
        return bestMove;
    }

    // 带有Alpha-Beta剪枝和置换表的极大极小值搜索
    private static int minimax(int[] board, int depth, int alpha, int beta, boolean isMaximizing) {
        // 首先尝试从置换表中读取结果
        int score = readTT(currentBoardHash, depth, alpha, beta);
        if (score != Integer.MIN_VALUE) {
            return score;
        }

        int alphaOriginal = alpha;

        // 搜索到底部，返回盘面估值
        if (depth <= 0) { // depth<=0 增加稳定性
            int val = -evaluateBoard(board);
            writeTT(currentBoardHash, 0, val, (byte)1);
            return val;
        }

        IntList moves = generateMoves(board, isMaximizing);
        int legalMovesCount = 0;
        orderMoves(board, moves);

        int bestVal = isMaximizing ? Integer.MIN_VALUE + 1000 : Integer.MAX_VALUE - 1000;

        for (int i = 0; i < moves.size; i++) {
            int move = moves.get(i);
            long undoInfo = makeMove(board, move);

            int kingPos = findKing(board, isMaximizing);
            // 排除导致己方王被攻击的非法招法
            if (isSquareAttacked(board, kingPos, !isMaximizing)) {
                unmakeMove(board, move, undoInfo);
                continue;
            }
            legalMovesCount++;

            int val = minimax(board, depth - 1, alpha, beta, !isMaximizing);

            unmakeMove(board, move, undoInfo);

            if (isMaximizing) {
                if (val > bestVal) bestVal = val;
                if (val > alpha) alpha = val;
            } else {
                if (val < bestVal) bestVal = val;
                if (val < beta) beta = val;
            }

            // Alpha-Beta 剪枝
            if (beta <= alpha) break;
        }

        // 处理特殊结尾：将军死或和棋
        if (legalMovesCount == 0) {
            int kingPos = findKing(board, isMaximizing);
            if (isSquareAttacked(board, kingPos, !isMaximizing)) {
                // 将军死的分数要考虑深度，鼓励尽早杀棋
                return isMaximizing ? (-MATE_VALUE - depth) : (MATE_VALUE + depth);
            } else {
                return 0; // 逼和
            }
        }

        // 将搜索结果记录到置换表
        byte flag;
        if (bestVal <= alphaOriginal) {
            flag = 3; // 上界
        } else if (bestVal >= beta) {
            flag = 2; // 下界
        } else {
            flag = 1; // 精确值
        }
        writeTT(currentBoardHash, depth, bestVal, flag);

        return bestVal;
    }

    // 计算哈希表索引
    private static int getTTIndex(long hash) {
        return (int)(hash & TT_MASK);
    }

    // 从置换表中读取存储的状态
    private static int readTT(long hash, int depth, int alpha, int beta) {
        int idx = getTTIndex(hash);
        if (ttHashes[idx] == hash) {
            if (ttDepths[idx] >= depth || Math.abs(ttScores[idx]) > MATE_VALUE - 1000) {
                int score = ttScores[idx];
                byte flag = ttFlags[idx];
                if (flag == 1) return score;
                if (flag == 2 && score >= beta) return score;
                if (flag == 3 && score <= alpha) return score;
            }
        }
        return Integer.MIN_VALUE;
    }

    // 写入搜索结果到置换表
    private static void writeTT(long hash, int depth, int score, byte flag) {
        int idx = getTTIndex(hash);
        ttHashes[idx] = hash;
        ttScores[idx] = score;
        ttDepths[idx] = (byte)depth;
        ttFlags[idx] = flag;
    }

    // 重新计算整个棋盘的 Zobrist 哈希值
    private static long computeFullHash(int[] board) {
        long h = 0;
        for (int i = 0; i < 64; i++) {
            if (board[i] != EMPTY) {
                int piece = board[i];
                h ^= ZOBRIST_KEYS[i][piece + 6];
            }
        }
        return h;
    }

    // 执行移动：更新数组，处理吃子、升变、王车易位、过路兵，并增量更新哈希值
    private static long makeMove(int[] board, int move) {
        int from = (move >> 6) & 0x3F;
        int to = move & 0x3F;
        int piece = board[from];
        int captured = board[to];

        boolean isEnPassant = false;
        boolean isCastling = false;
        boolean isPromotion = false;

        // 哈希更新：移除起点棋子
        currentBoardHash ^= ZOBRIST_KEYS[from][piece + 6];
        // 移除落点原有棋子（若有）
        if (captured != EMPTY) {
            currentBoardHash ^= ZOBRIST_KEYS[to][captured + 6];
        }

        // 处理过路兵逻辑 (提前判断)
        if (Math.abs(piece) == W_PAWN && Math.abs(from % 8 - to % 8) != 0 && captured == EMPTY) {
            isEnPassant = true;
            int capPos = to + (piece > 0 ? -8 : 8);
            captured = board[capPos]; // 存入被吃的兵
            currentBoardHash ^= ZOBRIST_KEYS[capPos][captured + 6]; // 移除被吃的兵哈希
            board[capPos] = EMPTY;
        }

        board[to] = piece;
        board[from] = EMPTY;

        // 处理兵的升变（默认变后）
        if (Math.abs(piece) == W_PAWN) {
            int r = to / 8;
            if ((piece == B_PAWN && r == 0) || (piece == W_PAWN && r == 7)) {
                isPromotion = true;
                int newPiece = (piece > 0) ? W_QUEEN : B_QUEEN;
                board[to] = newPiece;
                currentBoardHash ^= ZOBRIST_KEYS[to][newPiece + 6];
            } else {
                currentBoardHash ^= ZOBRIST_KEYS[to][piece + 6];
            }
        } else {
            currentBoardHash ^= ZOBRIST_KEYS[to][piece + 6];
        }

        // 处理王车易位：同时移动王和车
        if (Math.abs(piece) == W_KING && Math.abs(from - to) == 2) {
            isCastling = true;
            int row = from / 8;
            int rookFrom, rookTo;
            int rookPiece = (piece > 0) ? W_ROOK : B_ROOK;

            if (to % 8 == 6) { // 短易位
                rookFrom = row * 8 + 7;
                rookTo = row * 8 + 5;
            } else { // 长易位
                rookFrom = row * 8 + 0;
                rookTo = row * 8 + 3;
            }

            board[rookTo] = board[rookFrom];
            board[rookFrom] = EMPTY;
            currentBoardHash ^= ZOBRIST_KEYS[rookFrom][rookPiece + 6];
            currentBoardHash ^= ZOBRIST_KEYS[rookTo][rookPiece + 6];
        }

        // 切换走子方哈希
        currentBoardHash ^= SIDE_TO_MOVE_KEY;

        // 打包撤销移动所需的信息 (新增位控制升变回溯)
        int flags = (isEnPassant ? 1 : 0) | (isCastling ? 2 : 0) | (isPromotion ? 4 : 0);
        return ((long)flags << 32) | (captured & 0xFFFFFFFFL);
    }

    // 撤销移动：将棋盘状态恢复到 makeMove 之前
    private static void unmakeMove(int[] board, int move, long undoInfo) {
        currentBoardHash ^= SIDE_TO_MOVE_KEY;

        int from = (move >> 6) & 0x3F;
        int to = move & 0x3F;
        int captured = (int)(undoInfo & 0xFFFFFFFFL);
        int flags = (int)(undoInfo >>> 32);
        boolean isEnPassant = (flags & 1) != 0;
        boolean isCastling = (flags & 2) != 0;
        boolean isPromotion = (flags & 4) != 0;

        int piece = board[to]; // 此时可能是变后的皇后

        // 恢复升变前的兵 (根据flag判断，而非位置判断)
        if (isPromotion) {
            currentBoardHash ^= ZOBRIST_KEYS[to][piece + 6]; // 移除皇后哈希
            piece = (piece > 0) ? W_PAWN : B_PAWN; // 变回兵
            // 此时不加兵的哈希，因为后面统一处理 to 的移出
        }

        currentBoardHash ^= ZOBRIST_KEYS[to][board[to] + 6]; // 移除落点棋子哈希
        currentBoardHash ^= ZOBRIST_KEYS[from][piece + 6];   // 移回起点棋子哈希

        board[from] = piece;

        if (isEnPassant) {
            board[to] = EMPTY;
            int capPos = to + (piece > 0 ? -8 : 8);
            board[capPos] = captured;
            currentBoardHash ^= ZOBRIST_KEYS[capPos][captured + 6];
        } else {
            board[to] = captured;
            if (captured != EMPTY) {
                currentBoardHash ^= ZOBRIST_KEYS[to][captured + 6];
            }
        }

        // 恢复易位时的车
        if (isCastling) {
            int row = from / 8;
            int rookFrom, rookTo;
            int rookPiece = (piece > 0) ? W_ROOK : B_ROOK;

            if (to % 8 == 6) {
                rookFrom = row * 8 + 7; rookTo = row * 8 + 5;
            } else {
                rookFrom = row * 8 + 0; rookTo = row * 8 + 3;
            }
            board[rookFrom] = board[rookTo];
            board[rookTo] = EMPTY;
            currentBoardHash ^= ZOBRIST_KEYS[rookTo][rookPiece + 6];
            currentBoardHash ^= ZOBRIST_KEYS[rookFrom][rookPiece + 6];
        }
    }

    // 静态盘面评估函数：结合棋子子力分、位置分、将军状态和残局逻辑
    private static int evaluateBoard(int[] board) {
        int score = 0;
        int pieceCount = 0;

        for (int i = 0; i < 64; i++) {
            int p = board[i];
            if (p == EMPTY) continue;

            pieceCount++;
            int r = i / 8;
            int c = i % 8;

            // 基础子力分 + 位置分
            score += getPieceValue(p) * 100;
            score += getPstValue(p, r, c);

            // 惩罚那些被对方小兵盯着的大子
            if (isAttackedByPawn(board, r, c, p > 0)) {
                score -= (p > 0 ? 1 : -1) * (Math.abs(getPieceValue(p)) * 50);
            }
        }

        // 检查将军情况
        int wKing = findKing(board, true);
        int bKing = findKing(board, false);

        if (wKing != -1 && isSquareAttacked(board, wKing, false)) {
            score -= CHECK_BONUS;
        }
        if (bKing != -1 && isSquareAttacked(board, bKing, true)) {
            score += CHECK_BONUS;
        }

        // 残局加速逻辑（当棋子很少时）
        if (pieceCount < 10) {
            score += evaluateEndgame(board);
        }

        return score;
    }

    // 残局专用评估：鼓励己方王向中心靠拢，缩小对方王活动范围
    private static int evaluateEndgame(int[] board) {
        int bkPos = findKing(board, false);
        int wkPos = findKing(board, true);
        if (bkPos == -1 || wkPos == -1) return 0;

        int score = 0;
        // 鼓励双王靠近（残局王也是进攻武器）
        int dist = Math.abs((bkPos/8) - (wkPos/8)) + Math.abs((bkPos%8) - (wkPos%8));
        score -= (14 - dist) * 1000;

        // 鼓励王站中心
        int wkR = wkPos / 8;
        int wkC = wkPos % 8;
        int centerDist = Math.max(Math.abs(wkR - 3), Math.abs(wkC - 3));
        score -= centerDist * 1200;

        return score;
    }

    // 获取棋子的纯子力价值
    private static int getPieceValue(int p) {
        switch (Math.abs(p)) {
            case W_PAWN: return (p > 0) ? PAWN_VALUE : -PAWN_VALUE;
            case W_KNIGHT: return (p > 0) ? KNIGHT_VALUE : -KNIGHT_VALUE;
            case W_BISHOP: return (p > 0) ? BISHOP_VALUE : -BISHOP_VALUE;
            case W_ROOK: return (p > 0) ? ROOK_VALUE : -ROOK_VALUE;
            case W_QUEEN: return (p > 0) ? QUEEN_VALUE : -QUEEN_VALUE;
            case W_KING: return (p > 0) ? KING_VALUE : -KING_VALUE;
            default: return 0;
        }
    }

    // 从 PST 表中获取位置附加分
    private static int getPstValue(int p, int r, int c) {
        int absP = Math.abs(p);
        boolean isWhite = p > 0;
        int tableRow = isWhite ? (7 - r) : r;
        int val = 0;

        switch (absP) {
            case W_PAWN: val = PAWN_PST[tableRow][c]; break;
            case W_KNIGHT: val = KNIGHT_PST[tableRow][c]; break;
            case W_BISHOP: val = BISHOP_PST[tableRow][c]; break;
            case W_ROOK: val = ROOK_PST[tableRow][c]; break;
            case W_QUEEN: val = QUEEN_PST[tableRow][c]; break;
            case W_KING: val = KING_PST[tableRow][c]; break;
        }
        return isWhite ? val : -val;
    }

    // 生成当前局面的所有伪合法移动（不考虑将军禁手）
    private static IntList generateMoves(int[] board, boolean isWhite) {
        IntList moves = new IntList();
        for (int i = 0; i < 64; i++) {
            int p = board[i];
            if (p == EMPTY) continue;
            boolean isPieceWhite = p > 0;
            if (isPieceWhite != isWhite) continue;

            int r = i / 8;
            int c = i % 8;
            int type = Math.abs(p);

            switch (type) {
                case W_PAWN: genPawnMoves(board, r, c, isWhite, moves); break;
                case W_KNIGHT: genStepMoves(board, r, c, isWhite, moves, new int[][]{{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}}); break;
                case W_BISHOP: genSlideMoves(board, r, c, isWhite, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); break;
                case W_ROOK: genSlideMoves(board, r, c, isWhite, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); break;
                case W_QUEEN: genSlideMoves(board, r, c, isWhite, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}}); break;
                case W_KING: genKingMoves(board, r, c, isWhite, moves); break;
            }
        }
        return moves;
    }

    // 生成兵的所有合法移动（包括前进一步、两步、吃子、过路兵）
    private static void genPawnMoves(int[] board, int r, int c, boolean isWhite, IntList moves) {
        int dir = isWhite ? 1 : -1;
        int startRow = isWhite ? 1 : 6;
        int from = r * 8 + c;
        int toR = r + dir;

        // 往前走
        if (isValid(toR, c)) {
            int to = toR * 8 + c;
            if (board[to] == EMPTY) {
                moves.add((from << 6) | to);
                // 初始位置走两步
                if (r == startRow) {
                    int toR2 = r + dir * 2;
                    int to2 = toR2 * 8 + c;
                    if (board[to2] == EMPTY) {
                        moves.add((from << 6) | to2);
                    }
                }
            }
        }
        // 斜线吃子
        int[] dcs = {-1, 1};
        for (int dc : dcs) {
            int nc = c + dc;
            if (isValid(toR, nc)) {
                int target = toR * 8 + nc;
                int p = board[target];
                if (p != EMPTY && (isWhite != (p > 0))) {
                    moves.add((from << 6) | target);
                }
                // 过路兵逻辑
                int passantRow = isWhite ? 4 : 3;
                if (r == passantRow && p == EMPTY) {
                    int sidePos = r * 8 + nc;
                    int sideP = board[sidePos];
                    if (sideP != EMPTY && Math.abs(sideP) == W_PAWN && (isWhite != (sideP > 0))) {
                        moves.add((from << 6) | target);
                    }
                }
            }
        }
    }

    // 生成跳跃型棋子（马、王）的移动
    private static void genStepMoves(int[] board, int r, int c, boolean isWhite, IntList moves, int[][] offsets) {
        int from = r * 8 + c;
        for (int[] d : offsets) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (isValid(nr, nc)) {
                int to = nr * 8 + nc;
                int p = board[to];
                if (p == EMPTY || (isWhite != (p > 0))) {
                    moves.add((from << 6) | to);
                }
            }
        }
    }

    // 生成滑行型棋子（车、象、后）的移动
    private static void genSlideMoves(int[] board, int r, int c, boolean isWhite, IntList moves, int[][] offsets) {
        int from = r * 8 + c;
        for (int[] d : offsets) {
            int nr = r, nc = c;
            while (true) {
                nr += d[0];
                nc += d[1];
                if (!isValid(nr, nc)) break;
                int to = nr * 8 + nc;
                int p = board[to];
                if (p == EMPTY) {
                    moves.add((from << 6) | to);
                } else {
                    if (isWhite != (p > 0)) {
                        moves.add((from << 6) | to);
                    }
                    break;
                }
            }
        }
    }

    // 生成王的移动（含王车易位基本逻辑判断）
    private static void genKingMoves(int[] board, int r, int c, boolean isWhite, IntList moves) {
        genStepMoves(board, r, c, isWhite, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}});
        int row = isWhite ? 0 : 7;
        if (r == row && c == 4) {
            // 短易位判断
            if (isValid(row, 5) && isValid(row, 6) && board[row*8+5] == EMPTY && board[row*8+6] == EMPTY &&
                    Math.abs(board[row*8+7]) == W_ROOK) {
                moves.add(((r*8+c) << 6) | (row*8+6));
            }
            // 长易位判断
            if (isValid(row, 1) && isValid(row, 3) && board[row*8+1] == EMPTY && board[row*8+2] == EMPTY && board[row*8+3] == EMPTY &&
                    Math.abs(board[row*8+0]) == W_ROOK) {
                moves.add(((r*8+c) << 6) | (row*8+2));
            }
        }
    }

    // 判断某个位置是否正被对方某方攻击（用于将军检测和王移动合法性）
    private static boolean isSquareAttacked(int[] board, int idx, boolean byWhite) {
        if (idx < 0 || idx >= 64) return false;
        int r = idx / 8, c = idx % 8;

        // 兵攻击检测
        int pDir = byWhite ? -1 : 1;
        if (isValid(r + pDir, c - 1)) {
            int p = board[(r + pDir) * 8 + c - 1];
            if (p != EMPTY && Math.abs(p) == W_PAWN && (p > 0) == byWhite) return true;
        }
        if (isValid(r + pDir, c + 1)) {
            int p = board[(r + pDir) * 8 + c + 1];
            if (p != EMPTY && Math.abs(p) == W_PAWN && (p > 0) == byWhite) return true;
        }

        // 马攻击检测
        int[][] kDirs = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        for (int[] d : kDirs) {
            int nr = r + d[0], nc = c + d[1];
            if (isValid(nr, nc)) {
                int p = board[nr * 8 + nc];
                if (p != EMPTY && Math.abs(p) == W_KNIGHT && (p > 0) == byWhite) return true;
            }
        }

        // 直线攻击检测（车、后）
        int[][] rDirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : rDirs) {
            int nr = r, nc = c;
            while (true) {
                nr += d[0]; nc += d[1];
                if (!isValid(nr, nc)) break;
                int p = board[nr * 8 + nc];
                if (p != EMPTY) {
                    if ((p > 0) == byWhite && (Math.abs(p) == W_ROOK || Math.abs(p) == W_QUEEN)) return true;
                    break;
                }
            }
        }

        // 斜线攻击检测（象、后）
        int[][] bDirs = {{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] d : bDirs) {
            int nr = r, nc = c;
            while (true) {
                nr += d[0]; nc += d[1];
                if (!isValid(nr, nc)) break;
                int p = board[nr * 8 + nc];
                if (p != EMPTY) {
                    if ((p > 0) == byWhite && (Math.abs(p) == W_BISHOP || Math.abs(p) == W_QUEEN)) return true;
                    break;
                }
            }
        }

        // 王攻击检测
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr==0 && dc==0) continue;
                int nr = r+dr, nc = c+dc;
                if (isValid(nr, nc)) {
                    int p = board[nr*8+nc];
                    if (p != EMPTY && Math.abs(p) == W_KING && (p > 0) == byWhite) return true;
                }
            }
        }
        return false;
    }

    // 辅助方法：判断特定点是否被兵攻击（用于 PST 评估）
    private static boolean isAttackedByPawn(int[] board, int r, int c, boolean byWhite) {
        int pDir = byWhite ? -1 : 1;
        if (isValid(r + pDir, c - 1)) {
            int p = board[(r + pDir) * 8 + c - 1];
            if (p != EMPTY && Math.abs(p) == W_PAWN && (p > 0) == byWhite) return true;
        }
        if (isValid(r + pDir, c + 1)) {
            int p = board[(r + pDir) * 8 + c + 1];
            if (p != EMPTY && Math.abs(p) == W_PAWN && (p > 0) == byWhite) return true;
        }
        return false;
    }

    // 查找特定颜色的王在棋盘上的索引
    private static int findKing(int[] board, boolean isWhite) {
        int target = isWhite ? W_KING : B_KING;
        for (int i = 0; i < 64; i++) {
            if (board[i] == target) return i;
        }
        return -1;
    }

    // 检查行列坐标是否在棋盘范围内
    private static boolean isValid(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    // 简易的整型列表类，避免使用 ArrayList 带来的装箱拆箱开销
    private static class IntList {
        int[] data = new int[128];
        int size = 0;
        void add(int val) {
            if (size >= data.length) {
                int[] tmp = new int[data.length * 2];
                System.arraycopy(data, 0, tmp, 0, data.length);
                data = tmp;
            }
            data[size++] = val;
        }
        int get(int idx) { return data[idx]; }
    }

    // 对移动进行排序：优先尝试“小兵吃大子”，提高剪枝概率
    private static void orderMoves(int[] board, IntList moves) {
        for (int i = 0; i < moves.size - 1; i++) {
            for (int j = 0; j < moves.size - 1 - i; j++) {
                int s1 = getMoveScore(board, moves.get(j));
                int s2 = getMoveScore(board, moves.get(j + 1));
                if (s2 > s1) {
                    int temp = moves.data[j];
                    moves.data[j] = moves.data[j+1];
                    moves.data[j+1] = temp;
                }
            }
        }
    }

    // 计算移动的启发式分值：基于 MVV-LVA（最有价值的受害者 - 最无价值的攻击者）
    private static int getMoveScore(int[] board, int move) {
        int from = (move >> 6) & 0x3F;
        int to = move & 0x3F;
        int victim = board[to];
        if (victim != EMPTY) {
            int attacker = board[from];
            return 10 * Math.abs(getPieceValue(victim)) - Math.abs(getPieceValue(attacker)) / 10;
        }
        return 0;
    }

    // 数据转换：将游戏逻辑层的 String 数组转为 AI 内部高效处理的 int 一维数组
    private static int[] convertToIntBoard(String[][] strBoard) {
        int[] b = new int[64];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String s = strBoard[r][c];
                if (s == null || s.isEmpty()) {
                    b[r * 8 + c] = EMPTY;
                } else {
                    int val = 0;
                    boolean w = s.startsWith("w");
                    String type = s.toLowerCase();
                    if (type.endsWith("pawn")) val = W_PAWN;
                    else if (type.endsWith("knight")) val = W_KNIGHT;
                    else if (type.endsWith("bishop")) val = W_BISHOP;
                    else if (type.endsWith("rook")) val = W_ROOK;
                    else if (type.endsWith("queen")) val = W_QUEEN;
                    else if (type.endsWith("king")) val = W_KING;
                    b[r * 8 + c] = w ? val : -val;
                }
            }
        }
        return b;
        /*这个写了太长时间了，还有AI辅助，AI+自己一共干了一个月，参与度一半一半吧，而且要学的东西太多，minmax搜索，又改为置换表，
          中间程序又崩溃，闪退，我又是一个人做，没有AI辅助一个月做不出来，然后minmax搜索改为置换表真的不亚于整个class重写，
          虽然加一起3000行代码但是真的已经体会到不容易了，怪不得甲骨文这种世界级企业有那么多顶级工程师还有屎山代码
          而且我写到一半发现这些经典算法有别人写好的，我没必要自己写，人家别人写的稳定而且公模随便用，我没必要自己费劲写一遍经典算法
          有这精力不如进实验室干科研呢，所以我是直接套用的别人代码，毕竟上班要的是这人能不能干活给公司赚钱不是让你写论文
         */
    }
}