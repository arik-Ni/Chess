import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class Main {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setTitle("Chess Game");
        // 设置全屏
        config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        config.useVsync(true);
        config.setForegroundFPS(60);

        // 启动Chess 类
        new Lwjgl3Application(new Chess(), config);
    }/*这个总成还好吧，因为是第一次使用libgdx，不知道为什么关键字和API的单词这么长，很难记住幸亏有idea自动补全*/
}
