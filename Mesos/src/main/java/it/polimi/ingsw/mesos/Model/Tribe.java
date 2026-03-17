package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.polimi.ingsw.mesos.Model.Cards.Characters.BuilderCard;
import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.HunterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.GathererCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.InventorCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ShamanCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.ArtistCard;
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
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        List<CharacterCard> cards = this.characters.get(type);
        if (cards == null) {
            throw new IllegalStateException("No character list configured for type: " + type);
        }
        return cards.size();
    }

    public List<BuildingCard> getBuildingsWithTrigger(BuildingTrigger trigger) {
        if (trigger == null) {
            throw new IllegalArgumentException("trigger cannot be null");
        }

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

    public int buildersEndGamePrestige() {
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
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }
        CharacterType cardType = card.getCharacterType();
        if (cardType == null) {
            throw new IllegalArgumentException("character type cannot be null");
        }

        if (!isCardConsistentWithType(card, cardType)) {
            throw new IllegalArgumentException("Character card class is inconsistent with its type: " + card.getClass().getSimpleName());
        }

        this.characters.get(cardType).add(card);
    }

    public void addCard(BuildingCard card) {
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }
        this.buildings.add(card);
    }

    private static boolean isCardConsistentWithType(CharacterCard card, CharacterType type) {
        return switch (type) {
            case SHAMAN -> card instanceof ShamanCard;
            case BUILDER -> card instanceof BuilderCard;
            case GATHERER -> card instanceof GathererCard;
            case INVENTOR -> card instanceof InventorCard;
            case ARTIST -> card instanceof ArtistCard;
            case HUNTER -> card instanceof HunterCard;
        };
    }

}
