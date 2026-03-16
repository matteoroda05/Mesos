package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.polimi.ingsw.mesos.Model.Cards.BuilderCard;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.GathererCard;
import it.polimi.ingsw.mesos.Model.Cards.InventorCard;
import it.polimi.ingsw.mesos.Model.Cards.ShamanCard;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class Tribe {
    private final Map<CharacterType, List<CharacterCard>> characters;
    private final List<BuildingCard> buildings;

    public Tribe() {
        this.characters = Map.of(
            CharacterType.INVENTOR, new ArrayList<>(),
            CharacterType.GATHERER, new ArrayList<>(),
            CharacterType.SHAMAN, new ArrayList<>(),
            CharacterType.BUILDER, new ArrayList<>(),
            CharacterType.ARTIST, new ArrayList<>(),
            CharacterType.HUNTER, new ArrayList<>()
        );

        this.buildings = new ArrayList<>();
    }

    // Getters

    public int getNumCharacters() {
        int total = 0;
        for (List<CharacterCard> cards : this.characters.values()) {
            total += cards.size();
        }
        return total;
    }

    public int getNumCharacterOfType(CharacterType type) {
        return this.characters.get(type).size();
    }

    public List<BuildingCard> getBuildingsWithTrigger(BuildingTrigger trigger) {
        List<BuildingCard> triggered_buildings = new ArrayList<>();
        for (BuildingCard building : this.buildings) {
            if (building.getTrigger() == trigger) {
                triggered_buildings.add(building);
            }
        }
        return triggered_buildings;
    }

    public int getNumBuildings() {
        return this.buildings.size();
    }

    public int buildingsPrestige() {
        int prestige = 0;
        for (BuildingCard building : this.buildings) {
            prestige += building.getPrestigePoints();
        }
        return prestige;
    }

    public int buildingsEffectPrestige() {
        int prestige = 0;
        for (BuildingCard building : this.buildings) {
            if (building.getTrigger() == BuildingTrigger.ON_END_GAME) {
                // prestige += building.effect();
            }
        }
        return prestige;
    }

    // TODO: chiedere come downcastare in modo più elegante

    public int countShamanStars() {
        List<CharacterCard> shamans = this.characters.get(CharacterType.SHAMAN);
        if (shamans.isEmpty()) {
            return 0;
        }

        int stars = 0;
        for (CharacterCard shaman : shamans) {
            // DOWNCASTING
            stars += ((ShamanCard) shaman).getStars();
        }
        return stars;
    }

    public int buildersDiscount() {
        List<CharacterCard> builders = this.characters.get(CharacterType.BUILDER);
        if (builders.isEmpty()) {
            return 0;
        }

        int discount = 0;
        for (CharacterCard builder : builders) {
            // DOWNCASTING
            discount += ((BuilderCard) builder).getBuildingDiscount();
        }
        return discount;
    }

    private int buildersEndGamePrestige() {
        List<CharacterCard> builders = this.characters.get(CharacterType.BUILDER);
        if (builders.isEmpty()) {
            return 0;
        }

        int prestige = 0;
        for (CharacterCard builder : builders) {
            // DOWNCASTING
            prestige += ((BuilderCard) builder).getPrestigePoints();
        }
        return prestige;
    }

    public int gatherersDiscount() {
        List<CharacterCard> gatherers = this.characters.get(CharacterType.GATHERER);
        if (gatherers.isEmpty()) {
            return 0;
        }

        int discount = 0;
        for (CharacterCard gatherer : gatherers) {
            // DOWNCASTING
            discount += ((GathererCard) gatherer).getDiscount();
        }
        return discount;
    }

    public Set<InventionIcon> inventionIcons() {
        List<CharacterCard> inventors = this.characters.get(CharacterType.INVENTOR);
        Set<InventionIcon> icons = new HashSet<>();
        for (CharacterCard inventor : inventors) {
            // DOWNCASTING
            icons.add(((InventorCard) inventor).getInventionIcon());
        }
        return icons;
    }

    private int inventorsEndGamePrestige() {
        int inventor_count = getNumCharacterOfType(CharacterType.INVENTOR);
        int different_icons = inventionIcons().size();
        return inventor_count * different_icons;
    }

    private int artistsEndGamePrestige() {
        int artist_count = getNumCharacterOfType(CharacterType.ARTIST);
        return 10 * (artist_count / 2);
    }

    public int tribeEndGamePrestige() {
        int prestige = 0;
        prestige += buildingsPrestige();
        prestige += buildingsEffectPrestige();
        prestige += buildersEndGamePrestige();
        prestige += inventorsEndGamePrestige();
        prestige += artistsEndGamePrestige();
        return prestige;
    }

    public void addCard(CharacterCard card) {
        this.characters.get(card.getCharacterType()).add(card);
    }

    public void addCard(BuildingCard card) {
        this.buildings.add(card);
    }

}
