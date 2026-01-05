import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random; // 新增用于生成随机秒数

public class GameBoard {
    // 图像资源
    private Texture boardTexture, frameTexture, hintTexture, checkTexture, promoBg;
    // 缓存所有棋子的纹理
    private HashMap<String, Texture> pieceTextures;
    // 棋盘核心数据：存储棋子名称的二维数组
    private String[][] chessBoard;
    private BitmapFont font;
    private BitmapFont timerFont;

    // 棋盘布局常量计算
    private final float TILE_BLOCK_SIZE = 167f * 1.25f;
    private final float CELL_SIZE = TILE_BLOCK_SIZE / 2f;
    private final int REPEAT_COUNT = 4;
    private final float FRAME_PADDING = 25f;
    private final float PIECE_SCALE = 0.8f;

    private float boardOriginX, boardOriginY;

    // 游戏状态变量
    private int selectedRow = -1, selectedCol = -1;
    private ArrayList<int[]> validMoves = new ArrayList<>();
    private boolean isWhiteTurn = true;

    // 计时器系统：费舍尔模式
    private float whiteTime = 600f;
    private float blackTime = 600f;
    private final float INCREMENT = 5f;
    private boolean timerRunning = false;
    private final float TIMER_MARGIN_X = 60f;

    // 特殊走法逻辑：记录王和车是否移动过
    private boolean wKingMoved = false, bKingMoved = false;
    private boolean wRook0Moved = false, wRook7Moved = false;
    private boolean bRook0Moved = false, bRook7Moved = false;

    // 将军提示状态
    private int[] kingInCheckPos = null;
    private float checkFlashTimer = 0;
    private boolean showCheckFlash = true;

    // 升变与结局状态
    private boolean isPromoting = false;
    private boolean isGameOver = false;
    private String winnerText = "";
    private int promoR, promoC;
    private String[] promoOptions = {"queen", "rook", "bishop", "knight"};

    // 记录兵移动，用于吃过路兵判定
    private int lastPawnMoveCol = -1;
    private int lastPawnMoveRow = -1;

    // AI 状态标志
    private boolean aiEnabled = false;

    // AI 延迟控制变量
    private float aiDelayTimer = 0f;
    private boolean isAiWaiting = false;
    private Move pendingAiMove = null;
    private Random random = new Random();

    public GameBoard() {
        // 加载棋盘背景
        boardTexture = new Texture(Gdx.files.internal("Assets/grey_white.png"));

        // 初始化普通字体
        font = new BitmapFont();
        font.getData().setScale(1.8f);

        // 初始化计时器字体并开启平滑过滤
        timerFont = new BitmapFont();
        timerFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        timerFont.getData().setScale(2.2f);

        // 初始化各种动态生成的纹理资源
        createWoodFrame();
        create3DHintTexture();
        createCheckTexture();
        createPromoUI();
        loadPieceAssets();
        initPiecePositions();
        updatePosition();
    }

    // 生成被将军时的红色半透明背景
    private void createCheckTexture() {
        Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setColor(1, 0, 0, 0.7f);
        p.fill();
        checkTexture = new Texture(p);
        p.dispose();
    }

    // 生成升变选择菜单的背景色块
    private void createPromoUI() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(0.98f, 0.94f, 0.88f, 0.92f);
        p.fill();
        promoBg = new Texture(p);
        p.dispose();
    }

    // 生成棋盘外围的木质边框纹理
    private void createWoodFrame() {
        int size = (int) (TILE_BLOCK_SIZE * REPEAT_COUNT + FRAME_PADDING * 2);
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.35f, 0.22f, 0.12f, 1));
        pixmap.fill();
        frameTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    // 生成提示合法走法的蓝色半透明圆点
    private void create3DHintTexture() {
        int size = 128;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.0f, 0.85f, 1.0f, 0.5f));
        pixmap.fill();
        hintTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    // 批量加载所有棋子图片文件
    private void loadPieceAssets() {
        pieceTextures = new HashMap<>();
        String[] colors = {"black-", "white-"};
        String[] types = {"rook", "knight", "bishop", "queen", "king", "pawn"};
        for (String c : colors) {
            for (String t : types) {
                String key = (c.startsWith("b") ? "b-" : "w-") + t;
                pieceTextures.put(key, new Texture(Gdx.files.internal("Assets/" + c + t + ".png")));
            }
        }
    }//这个直接键值对双嵌套不用一个一个引入棋子图片了，对我这种第一次做3000行代码以上项目的人很有帮助

    // 重置棋局：摆放棋子，重置状态标志和计时器
    private void initPiecePositions() {
        chessBoard = new String[8][8];
        setupRow(7, "b");
        for (int i = 0; i < 8; i++) chessBoard[6][i] = "b-pawn";
        setupRow(0, "w");
        for (int i = 0; i < 8; i++) chessBoard[1][i] = "w-pawn";

        isWhiteTurn = true;
        isPromoting = false;
        isGameOver = false;
        timerRunning = false;
        whiteTime = 600f;
        blackTime = 600f;
        wKingMoved = bKingMoved = wRook0Moved = wRook7Moved = bRook0Moved = bRook7Moved = false;
        lastPawnMoveCol = lastPawnMoveRow = -1;

        // 重置 AI 延迟状态
        isAiWaiting = false;
        aiDelayTimer = 0f;
        pendingAiMove = null;

        updateCheckStatus();
    }

    // 辅助方法，快速摆放底线大棋子
    private void setupRow(int row, String c) {
        String[] order = {"rook", "knight", "bishop", "queen", "king", "bishop", "knight", "rook"};
        for (int i = 0; i < 8; i++) chessBoard[row][i] = c + "-" + order[i];
    }

    // 处理用户的鼠标点击或触摸输入
    public void handleInput() {
        if (!Gdx.input.justTouched()) return;
        if (isGameOver) {
            initPiecePositions();
            return;
        }

        float mx = Gdx.input.getX(), my = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (isPromoting) {
            handlePromoClick(mx, my);
            return;
        }

        // 计算点击位置对应的格点坐标
        int col = (int) ((mx - boardOriginX) / CELL_SIZE), row = (int) ((my - boardOriginY) / CELL_SIZE);
        if (row < 0 || row >= 8 || col < 0 || col >= 8) return;

        // 如果点击的是高亮的合法路径，执行走子
        for (int[] m : validMoves) {
            if (m[0] == row && m[1] == col) {
                executeMove(selectedRow, selectedCol, row, col);
                return;
            }
        }

        // 选中自己的棋子并显示所有合法走法
        String piece = chessBoard[row][col];
        if (piece != null && piece.startsWith(isWhiteTurn ? "w" : "b")) {
            selectedRow = row;
            selectedCol = col;
            calculateRealMoves(row, col, true);
        }
    }

    // 执行棋子移动的逻辑核心
    private void executeMove(int fr, int fc, int tr, int tc) {
        if (!timerRunning) timerRunning = true;
        String p = chessBoard[fr][fc];

        // 特殊规则：王车易位的车位置变动
        if (p.endsWith("king") && Math.abs(tc - fc) == 2) {
            if (tc == 6) {
                chessBoard[tr][5] = chessBoard[tr][7];
                chessBoard[tr][7] = null;
            } else if (tc == 2) {
                chessBoard[tr][3] = chessBoard[tr][0];
                chessBoard[tr][0] = null;
            }
        }

        // 特殊规则：吃过路兵的敌方兵移除
        if (p.endsWith("pawn")) {
            int pawnDir = p.startsWith("w") ? 1 : -1;
            if (Math.abs(tc - fc) == 1 && tr == fr + pawnDir && chessBoard[tr][tc] == null) chessBoard[fr][tc] = null;
        }

        // 更新棋盘数组状态
        chessBoard[tr][tc] = p;
        chessBoard[fr][fc] = null;

        // 更新棋子是否移动过的状态，用于后续易位判定
        if (p.endsWith("king")) {
            if (isWhiteTurn) wKingMoved = true;
            else bKingMoved = true;
        }
        if (p.endsWith("rook")) {
            if (isWhiteTurn) {
                if (fc == 0) wRook0Moved = true;
                if (fc == 7) wRook7Moved = true;
            } else {
                if (fc == 0) bRook0Moved = true;
                if (fc == 7) bRook7Moved = true;
            }
        }

        // 记录兵是否走两步，更新过路兵判定坐标
        if (p.endsWith("pawn") && Math.abs(tr - fr) == 2) {
            lastPawnMoveCol = tc;
            lastPawnMoveRow = tr;
        } else {
            lastPawnMoveCol = -1;
            lastPawnMoveRow = -1;
        }

        // 检查是否进入兵升变状态
        if (p.endsWith("pawn") && (tr == 0 || tr == 7)) {
            isPromoting = true;
            promoR = tr;
            promoC = tc;
            validMoves.clear();
        } else {
            // 补偿计时器并切换回合
            if (isWhiteTurn) whiteTime += INCREMENT;
            else blackTime += INCREMENT;
            switchTurn();
        }
    }

    // 切换回合及后期清理
    private void switchTurn() {
        isWhiteTurn = !isWhiteTurn;
        selectedRow = -1;
        validMoves.clear();
        updateCheckStatus(); // 检查是否有王被将军
        checkCheckmate(); // 检查是否死局

        // 如果开启了AI模式且现在是黑棋回合，则执行AI移动
        if (aiEnabled && !isWhiteTurn && !isGameOver && !isPromoting) {
            makeAIMove();
        }
    }

    // 寻找当前回合的王，并检测其是否处于被攻击状态
    private void updateCheckStatus() {
        kingInCheckPos = null;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                String p = chessBoard[r][c];
                if (p != null && p.endsWith("king") && p.startsWith(isWhiteTurn ? "w" : "b")) {
                    if (isAttacked(r, c, !isWhiteTurn)) kingInCheckPos = new int[]{r, c};
                }
            }
    }

    // 遍历棋局检查当前玩家是否还有任何合法走法，没有则宣布胜负或和局
    private void checkCheckmate() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                String p = chessBoard[r][c];
                if (p != null && p.startsWith(isWhiteTurn ? "w" : "b")) {
                    calculateRealMoves(r, c, true);
                    if (!validMoves.isEmpty()) {
                        validMoves.clear();
                        return;
                    }
                }
            }
        }
        isGameOver = true;
        winnerText = (kingInCheckPos != null) ? (isWhiteTurn ? "BLACK WINS!" : "WHITE WINS!") : "STALEMATE!";
    }

    // 检测指定位置是否正在受到指定颜色的棋子威胁
    private boolean isAttacked(int r, int c, boolean byWhite) {
        String enemyColor = byWhite ? "w" : "b";
        // 检测直线车和后
        int[][] straight = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : straight) {
            for (int i = 1; i < 8; i++) {
                int nr = r + d[0] * i, nc = c + d[1] * i;
                if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) break;
                if (chessBoard[nr][nc] != null) {
                    if (chessBoard[nr][nc].startsWith(enemyColor) && (chessBoard[nr][nc].endsWith("rook") || chessBoard[nr][nc].endsWith("queen")))
                        return true;
                    break;
                }
            }
        }
        // 检测斜线 象、后
        int[][] diagonal = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : diagonal) {
            for (int i = 1; i < 8; i++) {
                int nr = r + d[0] * i, nc = c + d[1] * i;
                if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) break;
                if (chessBoard[nr][nc] != null) {
                    if (chessBoard[nr][nc].startsWith(enemyColor) && (chessBoard[nr][nc].endsWith("bishop") || chessBoard[nr][nc].endsWith("queen")))
                        return true;
                    break;
                }
            }
        }
        // 检测马的威胁
        int[][] knightMoves = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
        for (int[] m : knightMoves) {
            int nr = r + m[0], nc = c + m[1];
            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && chessBoard[nr][nc] != null) {
                if (chessBoard[nr][nc].startsWith(enemyColor) && chessBoard[nr][nc].endsWith("knight")) return true;
            }
        }
        // 检测兵的威胁
        int pDir = byWhite ? -1 : 1;
        int[][] pawnAttacks = {{pDir, 1}, {pDir, -1}};
        for (int[] a : pawnAttacks) {
            int nr = r + a[0], nc = c + a[1];
            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && chessBoard[nr][nc] != null) {
                if (chessBoard[nr][nc].startsWith(enemyColor) && chessBoard[nr][nc].endsWith("pawn")) return true;
            }
        }
        // 检测王的直接威胁,避免两王接触
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue;
                int nr = r + i, nc = c + j;
                if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && chessBoard[nr][nc] != null) {
                    if (chessBoard[nr][nc].startsWith(enemyColor) && chessBoard[nr][nc].endsWith("king")) return true;
                }
            }
        return false;
    }

    // 计算某个棋子在物理规律下的所有走法
    private void calculateRealMoves(int r, int c, boolean filterIllegal) {
        validMoves.clear();
        String name = chessBoard[r][c];
        if (name == null) return;
        String type = name.substring(2);
        boolean isW = name.startsWith("w");

        // 兵的走法（单步、首行双步、斜吃、过路兵）
        if (type.equals("pawn")) {
            int d = isW ? 1 : -1;
            if (isEmpty(r + d, c)) {
                validMoves.add(new int[]{r + d, c});
                if ((isW && r == 1 || !isW && r == 6) && isEmpty(r + 2 * d, c)) validMoves.add(new int[]{r + 2 * d, c});
            }
            if (isEnemy(r + d, c - 1, isW)) validMoves.add(new int[]{r + d, c - 1});
            if (isEnemy(r + d, c + 1, isW)) validMoves.add(new int[]{r + d, c + 1});
            if (lastPawnMoveCol == c - 1 && lastPawnMoveRow == r && isEmpty(r + d, c - 1))
                validMoves.add(new int[]{r + d, c - 1});
            if (lastPawnMoveCol == c + 1 && lastPawnMoveRow == r && isEmpty(r + d, c + 1))
                validMoves.add(new int[]{r + d, c + 1});
        } else if (type.equals("king")) {
            // 王的走法及易位判定
            for (int i = -1; i <= 1; i++)
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) continue;
                    addIfSafe(r + i, c + j, isW);
                }
            if (kingInCheckPos == null) {
                if (isW && !wKingMoved) {
                    if (!wRook7Moved && isEmpty(0, 5) && isEmpty(0, 6) && !isAttacked(0, 4, false) && !isAttacked(0, 5, false))
                        validMoves.add(new int[]{0, 6});
                    if (!wRook0Moved && isEmpty(0, 1) && isEmpty(0, 2) && isEmpty(0, 3) && !isAttacked(0, 4, false) && !isAttacked(0, 3, false))
                        validMoves.add(new int[]{0, 2});
                }
                if (!isW && !bKingMoved) {
                    if (!bRook7Moved && isEmpty(7, 5) && isEmpty(7, 6) && !isAttacked(7, 4, true) && !isAttacked(7, 5, true))
                        validMoves.add(new int[]{7, 6});
                    if (!bRook0Moved && isEmpty(7, 1) && isEmpty(7, 2) && isEmpty(7, 3) && !isAttacked(7, 4, true) && !isAttacked(7, 3, true))
                        validMoves.add(new int[]{7, 2});
                }
            }
        } else if (type.equals("knight")) {
            int[][] steps = {{2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
            for (int[] s : steps) addIfSafe(r + s[0], c + s[1], isW);
        } else if (type.equals("rook")) addSliding(r, c, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}, isW);
        else if (type.equals("bishop")) addSliding(r, c, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}, isW);
        else if (type.equals("queen"))
            addSliding(r, c, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}, isW);

        // 如果开启非法过滤，则移除那些走完后会导致自己被将军的走法
        if (filterIllegal) {
            Iterator<int[]> it = validMoves.iterator();
            while (it.hasNext()) {
                int[] m = it.next();
                if (simulateAndCheck(r, c, m[0], m[1], isW)) it.remove();
            }
        }
    }

    // 核心的模拟演练,试走一步棋，看看自己的王是否还安全
    private boolean simulateAndCheck(int fr, int fc, int tr, int tc, boolean isWhite) {
        String sourcePiece = chessBoard[fr][fc];
        String targetPiece = chessBoard[tr][tc];
        chessBoard[tr][tc] = sourcePiece;
        chessBoard[fr][fc] = null;
        int kr = -1, kc = -1;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                if (chessBoard[r][c] != null && chessBoard[r][c].equals(isWhite ? "w-king" : "b-king")) {
                    kr = r;
                    kc = c;
                    break;
                }
            }
        boolean stillInCheck = isAttacked(kr, kc, !isWhite);
        // 回溯棋盘，必须将棋盘状态还原回去
        chessBoard[fr][fc] = sourcePiece;
        chessBoard[tr][tc] = targetPiece;
        return stillInCheck;
    }

    // 处理滑动类棋子（象、车、后）的连续射线探测
    private void addSliding(int r, int c, int[][] dirs, boolean isW) {
        for (int[] d : dirs) {
            for (int i = 1; i < 8; i++) {
                int nr = r + d[0] * i, nc = c + d[1] * i;
                if (nr < 0 || nr >= 8 || nc < 0 || nc >= 8) break;

                String target = chessBoard[nr][nc];
                if (target == null) {
                    validMoves.add(new int[]{nr, nc});
                } else {
                    // 如果目标是对方王，就不能吃（也不继续穿透）
                    if (target.startsWith(isW ? "b" : "w") && !target.endsWith("king")) {
                        validMoves.add(new int[]{nr, nc});
                    }
                    break;
                }
            }
        }
    }

    // 基础边界检查和棋子颜色合法判定
    private void addIfSafe(int r, int c, boolean isW) {
        if (r >= 0 && r < 8 && c >= 0 && c < 8) {
            String target = chessBoard[r][c];
            if (target == null ||
                    (target.startsWith(isW ? "b" : "w") && !target.endsWith("king"))) {  // ← 关键：排除对方王
                validMoves.add(new int[]{r, c});
            }
        }
    }

    private boolean isEmpty(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8 && chessBoard[r][c] == null;
    }

    private boolean isEnemy(int r, int c, boolean isW) {
        return r >= 0 && r < 8 && c >= 0 && c < 8 && chessBoard[r][c] != null && chessBoard[r][c].startsWith(isW ? "b" : "w");
    }

    // 更新棋盘在屏幕中央的渲染坐标
    public void updatePosition() {
        float total = TILE_BLOCK_SIZE * REPEAT_COUNT;
        boardOriginX = (Gdx.graphics.getWidth() - total) / 2;
        boardOriginY = (Gdx.graphics.getHeight() - total) / 2;
    }

    // 格式化秒数为 MM:SS
    private String formatTime(float sec) {
        return String.format("%02d:%02d", (int) sec / 60, (int) sec % 60);
    }

    // 游戏主渲染逻辑
    public void draw(SpriteBatch batch) {
        // 处理 AI 延迟落子逻辑
        if (isAiWaiting && !isGameOver && !isPromoting) {
            aiDelayTimer -= Gdx.graphics.getDeltaTime();
            if (aiDelayTimer <= 0) {
                isAiWaiting = false;
                if (pendingAiMove != null) {
                    executeMove(pendingAiMove.fromR, pendingAiMove.fromC, pendingAiMove.toR, pendingAiMove.toC);
                    pendingAiMove = null;
                }
            }
        }

        // 更新计时器，检测超时
        if (timerRunning && !isGameOver && !isPromoting) {
            float dt = Gdx.graphics.getDeltaTime();
            if (isWhiteTurn) {
                whiteTime -= dt;
                if (whiteTime <= 0) {
                    whiteTime = 0;
                    isGameOver = true;
                    winnerText = "WHITE TIME OUT!";
                }
            } else {
                blackTime -= dt;
                if (blackTime <= 0) {
                    blackTime = 0;
                    isGameOver = true;
                    winnerText = "BLACK TIME OUT!";
                }
            }
        }

        // 调用输入处理
        handleInput();

        // AI 黑棋自动升变逻辑
        // 当黑棋升变且 AI 已启用时，自动升变为皇后，无需显示菜单
        if (isPromoting && !isWhiteTurn && aiEnabled) {
            chessBoard[promoR][promoC] = "b-queen";
            isPromoting = false;
            if (isWhiteTurn) whiteTime += INCREMENT;
            else blackTime += INCREMENT;
            switchTurn();
        }

        // 计算将军警告的闪烁频率
        if (kingInCheckPos != null) {
            checkFlashTimer += Gdx.graphics.getDeltaTime();
            showCheckFlash = (int) (checkFlashTimer * 4) % 2 == 0;
        }

        // 渲染核心棋盘内容 ，无论游戏是否结束都渲染

        // 渲染棋盘框架和格子
        batch.draw(frameTexture, boardOriginX - FRAME_PADDING, boardOriginY - FRAME_PADDING);
        for (int i = 0; i < REPEAT_COUNT; i++)
            for (int j = 0; j < REPEAT_COUNT; j++)
                batch.draw(boardTexture, boardOriginX + i * TILE_BLOCK_SIZE, boardOriginY + j * TILE_BLOCK_SIZE, TILE_BLOCK_SIZE, TILE_BLOCK_SIZE);

        // 渲染双方计时器
        float timerX = boardOriginX + (TILE_BLOCK_SIZE * REPEAT_COUNT) + TIMER_MARGIN_X;
        timerFont.setColor(0.9f, 0.9f, 0.9f, 1f);
        String wText = "WHITE " + formatTime(whiteTime) + (isWhiteTurn && !isGameOver ? " <" : "");
        String bText = "BLACK " + formatTime(blackTime) + (!isWhiteTurn && !isGameOver ? " <" : "");
        timerFont.draw(batch, bText, timerX, boardOriginY + (TILE_BLOCK_SIZE * REPEAT_COUNT) - 50);
        timerFont.draw(batch, wText, timerX, boardOriginY + 50);

        // 如果被将军，渲染红色高亮
        if (kingInCheckPos != null && showCheckFlash)
            batch.draw(checkTexture, boardOriginX + kingInCheckPos[1] * CELL_SIZE, boardOriginY + kingInCheckPos[0] * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        // 渲染可落子的蓝色提示点 (游戏结束时不显示)
        if (!isGameOver) {
            for (int[] m : validMoves)
                batch.draw(hintTexture, boardOriginX + m[1] * CELL_SIZE + 6, boardOriginY + m[0] * CELL_SIZE + 6, CELL_SIZE - 12, CELL_SIZE - 12);
        }

        // 渲染所有棋子
        float ds = CELL_SIZE * PIECE_SCALE, off = (CELL_SIZE - ds) / 2f;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                String n = chessBoard[r][c];
                if (n != null)
                    batch.draw(pieceTextures.get(n), boardOriginX + c * CELL_SIZE + off, boardOriginY + r * CELL_SIZE + off + (r == selectedRow && c == selectedCol ? 15 : 0), ds, ds);
            }

        // 渲染 UI 覆盖层
        if (isPromoting) {
            drawPromoMenu(batch);
        } else if (isGameOver) {
            drawGameOverMenu(batch);
        }
    }

    // 渲染兵升变的悬浮菜单
    private void drawPromoMenu(SpriteBatch batch) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        batch.draw(promoBg, 0, 0, sw, sh);
        font.setColor(0.2f, 0.2f, 0.2f, 1f);
        font.draw(batch, "PROMOTION", sw / 2f - 80, sh / 2f + 150);
        float menuW = 440, startX = (sw - menuW) / 2f, startY = (sh - 120) / 2f;
        String color = isWhiteTurn ? "w-" : "b-";
        for (int i = 0; i < promoOptions.length; i++)
            batch.draw(pieceTextures.get(color + promoOptions[i]), startX + i * 110 + 5, startY + 10, 100, 100);
    }

    // 渲染游戏结束界面 - 修改为半透明横条，不遮挡棋盘
    private void drawGameOverMenu(SpriteBatch batch) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        // 仅在屏幕中央绘制一个半透明背景横条
        batch.setColor(1, 1, 1, 0.85f);
        batch.draw(promoBg, 0, sh / 2f - 100, sw, 200);
        batch.setColor(1, 1, 1, 1f);

        font.setColor(0.15f, 0.15f, 0.15f, 1f);
        font.draw(batch, winnerText, sw / 2f - 120, sh / 2f + 50);
        font.draw(batch, "TAP TO RESTART", sw / 2f - 110, sh / 2f - 30);
    }

    // 处理兵升变菜单中的点击事件
    // 只有白棋会进入此方法，因为黑棋在 draw 中自动升变
    private void handlePromoClick(float mx, float my) {
        float sw = Gdx.graphics.getWidth(), sh = Gdx.graphics.getHeight();
        float menuW = 440, startX = (sw - menuW) / 2f, startY = (sh - 120) / 2f;
        if (my >= startY && my <= startY + 120) {
            int index = (int) ((mx - startX) / 110);
            if (index >= 0 && index < 4) {
                chessBoard[promoR][promoC] = (isWhiteTurn ? "w-" : "b-") + promoOptions[index];
                isPromoting = false;
                if (isWhiteTurn) whiteTime += INCREMENT;
                else blackTime += INCREMENT;
                switchTurn();
            }
        }
    }

    // 销毁棋盘及其占用的显存资源
    public void dispose() {
        boardTexture.dispose();
        frameTexture.dispose();
        hintTexture.dispose();
        checkTexture.dispose();
        promoBg.dispose();
        font.dispose();
        timerFont.dispose();
        for (Texture t : pieceTextures.values()) t.dispose();
    }

    //走法数据结构封装
    public static class Move {
        public int fromR, fromC, toR, toC;

        public Move(int fr, int fc, int tr, int tc) {
            this.fromR = fr;
            this.fromC = fc;
            this.toR = tr;
            this.toC = tc;
        }
    }

    // 设置是否开启人机模式
    public void setAiEnabled(boolean enabled) {
        this.aiEnabled = enabled;
    }

    // 执行 AI 决策
    private void makeAIMove() {
        // 加入将军应对逻辑
        updateCheckStatus(); // 确保将军状态是最新的

        Move bestMove = AI.getBestMove(chessBoard);

        // 如果 AI 此时被将军 (kingInCheckPos != null)
        if (kingInCheckPos != null) {
            // 检查 AI 选出的走法是否能化解将军
            if (bestMove == null || simulateAndCheck(bestMove.fromR, bestMove.fromC, bestMove.toR, bestMove.toC, false)) {
                // 如果找不到能化解将军的棋，AI 认输
                isGameOver = true;
                winnerText = "WHITE WINS! (AI RESIGNS)";
                return;
            }
        }

        if (bestMove != null) {
            // 不再直接执行，而是进入延迟队列
            pendingAiMove = bestMove;
            isAiWaiting = true;
            // 随机生成 2 到 5 之间的浮点秒数
            aiDelayTimer = 2.0f + random.nextFloat() * 3.0f;
        } else {
            // 如果非将军状态下也无子可动，判定为和棋或败北
            checkCheckmate();
        }
    }

    // 重置方法：确保点击进入时是新棋局
    public void resetGame(boolean useAi) {
        this.aiEnabled = useAi;
        initPiecePositions();
    }/*这个类写了很久，也学到了很多东西很多很多，第一次独立做3000行代码以上的项目，感受到了不容易，大部分是手搓代码，
       AI也有参与辅助，还是要多刷刷leetcode呀，然后我发现不少老工程师也会忘记API名字和关键字哈哈哈
    */
}