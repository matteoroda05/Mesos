package it.polimi.ingsw.mesos.Network;

import it.polimi.ingsw.mesos.Controller.GameSetup;
import it.polimi.ingsw.mesos.Model.CardLoader;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingEndGame;
import it.polimi.ingsw.mesos.Model.Cards.Buildings.BuildingShamanicRitual;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.BuilderCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Enums.Era;
import it.polimi.ingsw.mesos.Model.GameContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesosServerCoreSummaryTest {
    @Test
    void privateSummaryHelpersDescribeEveryEndGameBuildingVariant() throws Exception {
        Object session = newSession();
        Method describeEndGameBuilding = session.getClass()
                .getDeclaredMethod("describeEndGameBuilding", BuildingEndGame.class);
        describeEndGameBuilding.setAccessible(true);

        assertTrue(((String) describeEndGameBuilding.invoke(session,
                new BuildingEndGame(Era.FIRST, 2, 1, 1, true, 0, 0, null, 0)))
                .contains("double"));
        assertTrue(((String) describeEndGameBuilding.invoke(session,
                new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 5, 0, null, 0)))
                .contains("complete character set"));
        assertTrue(((String) describeEndGameBuilding.invoke(session,
                new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 0, 2, HunterCard.class, 0)))
                .contains("Hunter"));
        assertTrue(((String) describeEndGameBuilding.invoke(session,
                new BuildingEndGame(Era.FIRST, 2, 1, 1, false, 0, 0, null, 25)))
                .contains("+25"));
    }

    @Test
    void privateSummaryHelpersDescribeNestedCardVariantsAndFormatting() throws Exception {
        Object session = newSession();
        Method describeCard = session.getClass()
                .getDeclaredMethod("describeCard", it.polimi.ingsw.mesos.Model.Cards.Card.class);
        Method formatName = session.getClass().getDeclaredMethod("formatName", String.class);
        Method signed = session.getClass().getDeclaredMethod("signed", int.class, String.class);
        describeCard.setAccessible(true);
        formatName.setAccessible(true);
        signed.setAccessible(true);

        assertTrue(((String) describeCard.invoke(session,
                new BuildingShamanicRitual(Era.FIRST, 2, 1, 1, 3, 0))).contains("+3"));
        assertTrue(((String) describeCard.invoke(session,
                new BuildingShamanicRitual(Era.FIRST, 2, 1, 1, 0, 2))).contains("x2"));
        assertTrue(((String) describeCard.invoke(session,
                new BuildingShamanicRitual(Era.FIRST, 2, 1, 1, 0, 0))).contains("cancel"));
        assertTrue(((String) describeCard.invoke(session,
                new ArtistCard(Era.FIRST, 2))).isEmpty());
        assertTrue(((String) describeCard.invoke(session,
                new BuilderCard(Era.FIRST, 2, 1, 2))).contains("food discount"));
        assertTrue(((String) formatName.invoke(session, "ShamanicRitualEvent")).contains("Shamanic Ritual Event"));
        assertTrue(((String) signed.invoke(session, -1, " food")).contains("-1"));
    }

    private static Object newSession() throws Exception {
        MesosServerCore serverCore = new MesosServerCore();
        GameSetup setup = new GameSetup();
        GameContext context = setup.createGame("Summary", 2, CardLoader.load());
        Class<?> sessionClass = Class.forName("it.polimi.ingsw.mesos.Network.MesosServerCore$GameSession");
        Constructor<?> constructor = sessionClass.getDeclaredConstructor(MesosServerCore.class, GameContext.class);
        constructor.setAccessible(true);
        return constructor.newInstance(serverCore, context);
    }
}
