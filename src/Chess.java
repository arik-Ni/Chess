import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class Chess extends ApplicationAdapter {
    private Stage stage;
    private SpriteBatch batch;
    private BitmapFont font;
    private Color bgColor = new Color(0.96f, 0.96f, 0.96f, 1);

    // 棋盘渲染与逻辑控制
    private GameBoard gameBoard;

    // UI 布局容器
    private Table mainMenuTable;
    private Table creditsTable;

    // 游戏运行状态
    private boolean isPlaying = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        font = new BitmapFont();

        // 初始化游戏核心逻辑
        gameBoard = new GameBoard();

        // 构造按钮通用背景样式
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        TextureRegionDrawable whiteBg = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = whiteBg;
        btnStyle.font = font;
        btnStyle.fontColor = Color.BLACK;

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.BLACK);

        // 主菜单布局
        mainMenuTable = new Table();
        mainMenuTable.setFillParent(true);
        stage.addActor(mainMenuTable);

        Label title = new Label("CHESS", titleStyle);
        title.setFontScale(6f);
        mainMenuTable.add(title).padBottom(80).row();

        // 菜单交互按钮
        TextButton pvpBtn = new TextButton("LOCAL PVP", btnStyle);
        TextButton aiBtn = new TextButton("VS COMPUTER", btnStyle);
        TextButton creditsBtn = new TextButton("CREDITS", btnStyle);
        TextButton exitBtn = new TextButton("EXIT", btnStyle);

        mainMenuTable.add(pvpBtn).width(300).height(50).pad(15).row();
        mainMenuTable.add(aiBtn).width(300).height(50).pad(15).row();
        mainMenuTable.add(creditsBtn).width(300).height(50).pad(15).row();
        mainMenuTable.add(exitBtn).width(300).height(50).pad(15).row();

        // 主题配色选择栏
        Table colorTable = new Table();
        TextButton day = new TextButton("Day", btnStyle);
        TextButton night = new TextButton("Night", btnStyle);
        TextButton dusk = new TextButton("Dusk", btnStyle);
        colorTable.add(day).pad(10);
        colorTable.add(night).pad(10);
        colorTable.add(dusk).pad(10);
        mainMenuTable.add(colorTable).padTop(100);

        // 制作人员名单布局
        creditsTable = new Table();
        creditsTable.setFillParent(true);
        creditsTable.setVisible(false);
        stage.addActor(creditsTable);

        Label creditsTitle = new Label("CREDITS", titleStyle);
        creditsTitle.setFontScale(4f);

        Label info = new Label(
                "Developer: NI Yunbo\n\n" +
                        "Engine: LibGDX\n" +
                        "Language: Java\n\n" +
                        "Version: 1.0.0\n\n" +
                        "Thanks for Claude AI\n\n"+
                        "Thanks for playing!", titleStyle);
        info.setFontScale(1.5f);
        info.setAlignment(1);

        TextButton backBtn = new TextButton("BACK", btnStyle);

        creditsTable.add(creditsTitle).padBottom(50).row();
        creditsTable.add(info).padBottom(50).row();
        creditsTable.add(backBtn).width(200).height(50);

        // 事件监听逻辑处理

        // 开启本地双人对战：重置棋盘并禁用AI
        pvpBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameBoard.resetGame(false);
                mainMenuTable.setVisible(false);
                isPlaying = true;
            }
        });

        // 开启人机对战：重置棋盘并激活AI
        aiBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameBoard.resetGame(true);
                mainMenuTable.setVisible(false);
                isPlaying = true;
            }
        });

        creditsBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                mainMenuTable.setVisible(false);
                creditsTable.setVisible(true);
            }
        });

        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                creditsTable.setVisible(false);
                mainMenuTable.setVisible(true);
            }
        });

        day.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { bgColor.set(0.96f, 0.96f, 0.96f, 1); }
        });
        night.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { bgColor.set(0.2f, 0.2f, 0.2f, 1); }
        });
        dusk.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { bgColor.set(1.0f, 0.8f, 0.5f, 1); }
        });

        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { Gdx.app.exit(); }
        });
    }

    @Override
    public void render() {
        // 全局键盘快捷键响应
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (isPlaying) {
                isPlaying = false;
                mainMenuTable.setVisible(true);
            } else if (creditsTable.isVisible()) {
                creditsTable.setVisible(false);
                mainMenuTable.setVisible(true);
            } else {
                Gdx.app.exit();
            }
        }

        ScreenUtils.clear(bgColor);

        // 绘制激活中的游戏棋盘
        if (isPlaying) {
            batch.begin();
            gameBoard.draw(batch);
            batch.end();
        }

        // 更新并渲染 UI 舞台
        stage.act();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (gameBoard != null) {
            gameBoard.updatePosition(); // 适配窗口缩放
        }
    }
    //这个类太好写了，不到1天就写完了，调节缩放确实一直在改数据运行测试然后搞了几个小时
}