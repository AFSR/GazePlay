package net.gazeplay.games.shooter;

import javafx.scene.Scene;
import net.gazeplay.GameLifeCycle;
import net.gazeplay.IGameContext;
import net.gazeplay.IGameLauncher;
import net.gazeplay.commons.gamevariants.IGameVariant;
import net.gazeplay.commons.utils.stats.Stats;

public class ShooterGameLauncher implements IGameLauncher {
    @Override
    public Stats createNewStats(Scene scene) {
        return new ShooterGamesStats(scene, "biboule");
    }

    @Override
    public GameLifeCycle createNewGame(IGameContext gameContext, IGameVariant gameVariant,
                                       Stats stats) {
        return new Shooter(gameContext, stats, "biboule");
    }
}
