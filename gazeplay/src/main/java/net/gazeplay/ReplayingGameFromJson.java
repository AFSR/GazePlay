package net.gazeplay;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.commons.ui.Translator;
import net.gazeplay.commons.utils.FixationPoint;
import net.gazeplay.commons.utils.stats.Stats;
import net.gazeplay.ui.scenes.configuration.ConfigurationContext;
import net.gazeplay.ui.scenes.ingame.GameContext;
import net.gazeplay.ui.scenes.ingame.GameContextFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

public class ReplayingGameFromJson {
    static List<GameSpec> gamesList;

    private static ApplicationContext applicationContext;
    private static GameContext gameContext;
    private static GazePlay gazePlay;
    private static String currentGameNameCode;
    private static String currentGameVariant;
    private static GameSpec selectedGameSpec;
    private static GameSpec.GameVariant gameVariant;
    private static JsonArray coordinatesAndTimeStamp;
    // location
    private static int x0, y0;
    private static int nextX, nextY, nextTime, prevTime;
    // refresh rate
    private static int delay = 0;
    private static boolean first = true;

    public ReplayingGameFromJson(GazePlay gazePlay, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.gazePlay = gazePlay;
        gameContext = applicationContext.getBean(GameContext.class);
    }

    public static void setGameList(List<GameSpec> games){
        gamesList = games;
    }

    public static void pickJSONFile() throws FileNotFoundException {
        final String fileName = getS();
        if (fileName == null) {
            return;
        }
        final File selectedFile = new File(fileName);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(selectedFile));
        Gson gson = new Gson();
        JsonFile json = gson.fromJson(bufferedReader, JsonFile.class);
        currentGameNameCode = json.getGameName();
        currentGameVariant = json.getGameVariant();
        coordinatesAndTimeStamp = json.getCoordinatesAndTimeStamp();
        System.out.println("Seed: " + json.getSeed());
        System.out.println("gameName: " + json.getGameName());
        System.out.println("gameVariantClass: " + json.getGameVariant());
        System.out.println("gameVariant: " + json.getGameVariantClass());
        System.out.println("screenAspectRatio: " + json.getScreenAspectRatio());
        System.out.println("gameStartedTime: " + json.getGameStartedTime());
        replayGame();
    }
    public static String getS() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("JSON files", "*.json")
        );
        File selectedFile = fileChooser.showOpenDialog(gameContext.getPrimaryStage());
        String s = null;
        if (selectedFile != null) {
            s = selectedFile.getAbsolutePath();
        }
        return s;
    }
    public static void replayGame(){
        ConfigurationContext configContext = applicationContext.getBean(ConfigurationContext.class);
        gameContext = applicationContext.getBean(GameContext.class);

        gazePlay.onGameLaunch(gameContext);

        for (GameSpec gameSpec : gamesList) {
            if (currentGameNameCode.equals(gameSpec.getGameSummary().getNameCode())){
                selectedGameSpec = gameSpec;
            }
        }
        final Translator translator = gazePlay.getTranslator();
        for (GameSpec.GameVariant variant : selectedGameSpec.getGameVariantGenerator().getVariants()){
            if (currentGameVariant.equals(variant.getLabel(translator))){
                gameVariant = variant;
            }
        }

        GameSpec.GameLauncher gameLauncher = selectedGameSpec.getGameLauncher();

        final Scene scene = gazePlay.getPrimaryScene();
        final Stats stats = gameLauncher.createNewStats(scene);

        GameLifeCycle currentGame = gameLauncher.createNewGame(gameContext, gameVariant, stats);
        currentGame.launch();

        /*var game = gameContext.getChildren().get(gameContext.getChildren().indexOf(currentGame));
        gameContext.getRoot().getChildren().get(gameContext.getRoot().getChildren().indexOf(swingNode)).toFront();*/

        /*gameContext.createControlPanel(gazePlay, stats, currentGame);
        gameContext.createQuitShortcut(gazePlay, stats, currentGame);*/

        final Dimension2D screenDimension = gameContext.getCurrentScreenDimensionSupplier().get();

        //Drawing in canvas
        final javafx.scene.canvas.Canvas canvas = new Canvas(screenDimension.getWidth(), screenDimension.getHeight());
        gameContext.getChildren().add(canvas);
        drawFixationLines(canvas, coordinatesAndTimeStamp);

        //Drawing in swingNode
        /*BasicAnim anim  = new BasicAnim(coordinatesAndTimeStamp);
        final SwingNode swingNode = new SwingNode();
        swingNode.setContent(anim);
        swingNode.resize(screenDimension.getWidth(), screenDimension.getHeight());
        gameContext.getRoot().getChildren().add(swingNode);
        gameContext.getRoot().getChildren().get(gameContext.getRoot().getChildren().indexOf(swingNode)).toFront();
        anim.startAnim();*/

    }

    private static void drawFixationLines(Canvas canvas, JsonArray coordinatesAndTimeStamp) {
        final GraphicsContext gc = canvas.getGraphicsContext2D();

        JsonObject  startingCoordinates = (JsonObject) coordinatesAndTimeStamp.get(0);
        x0 = Integer.parseInt(String.valueOf(startingCoordinates.get("X")));
        y0 = Integer.parseInt(String.valueOf(startingCoordinates.get("Y")));
        gc.setStroke(javafx.scene.paint.Color.rgb(255, 157, 6, 1));
        gc.setLineWidth(4);

        for (JsonElement pa : coordinatesAndTimeStamp) {
            if (!first) {
                x0 = nextX;
                y0 = nextY;
                prevTime = nextTime;
            }
            JsonObject paymentObj = pa.getAsJsonObject();
            nextX = Integer.parseInt(paymentObj.get("X").getAsString());
            nextY = Integer.parseInt(paymentObj.get("Y").getAsString());
            nextTime = Integer.parseInt(paymentObj.get("time").getAsString());
            delay = nextTime - prevTime;
            //System.out.println("x0: " + x0 + ". y0: " + y0 + ". nextX: " + nextX + ". nextY: " + nextY);
            if (!first)
                repaint(gc);
            else
                first = false;
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
        }
        //return gc;
    }

    public static void repaint(GraphicsContext g) {
        paint(g);
    }

    public static void paint(GraphicsContext g) {

        //g.strokeLine(x0, y0, nextX, nextY);
        g.lineTo(nextX, nextY);
        g.stroke();
    }

}
class JsonFile {

    int Seed;
    String GameName;
    String GameVariantClass;
    String GameVariant;
    double GameStartedTime;
    String ScreenAspectRatio;
    JsonArray CoordinatesAndTimeStamp;

    public int getSeed() {
        return Seed;
    }

    public String getGameName() {
        return GameName;
    }

    public String getGameVariantClass() {
        return GameVariantClass;
    }

    public String getGameVariant() {
        return GameVariant;
    }

    public double getGameStartedTime() {
        return GameStartedTime;
    }

    public String getScreenAspectRatio() {
        return ScreenAspectRatio;
    }

    public JsonArray getCoordinatesAndTimeStamp() {return CoordinatesAndTimeStamp;}

}

class BasicAnim extends JPanel {
    private static JsonArray CoordinatesAndTimeStamp;
    // location
    private static int x0, y0;
    int nextX, nextY, nextTime, prevTime;
    // refresh rate
    int delay = 0;
    Graphics2D graph;
    boolean first = true;

    public BasicAnim(JsonArray coordinatesAndTimeStamp) {
        CoordinatesAndTimeStamp = coordinatesAndTimeStamp;
        JsonObject  startingCoordinates = (JsonObject) CoordinatesAndTimeStamp.get(0);
        x0 = Integer.parseInt(String.valueOf(startingCoordinates.get("X")));
        y0 = Integer.parseInt(String.valueOf(startingCoordinates.get("Y")));
    }

    public void update(Graphics g) {
        paint(g);
    }

    public void paint(Graphics g) {
        //super.paintComponent(g);
        //graph = (Graphics2D)g;
        g.setColor(Color.red);
        //graph.drawOval(x, y, dim, dim);

        g.drawLine(x0, y0, nextX, nextY);
    }


    public void startAnim() {
        for (JsonElement pa : CoordinatesAndTimeStamp) {
            if (!first) {
                x0 = nextX;
                y0 = nextY;
                prevTime = nextTime;
            }
            JsonObject paymentObj = pa.getAsJsonObject();
            nextX = Integer.parseInt(paymentObj.get("X").getAsString());
            nextY = Integer.parseInt(paymentObj.get("Y").getAsString());
            nextTime = Integer.parseInt(paymentObj.get("time").getAsString());
            delay = nextTime - prevTime;
            //System.out.println("x0: " + x0 + ". y0: " + y0 + ". nextX: " + nextX + ". nextY: " + nextY);
            if (!first)
                repaint();
            else
                first = false;
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
        }
    }

    public Dimension getPreferredSize(){
        return new Dimension(1920, 1200);
    }
}

