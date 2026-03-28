package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.*;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class Tribe {
    private final Map<Class<? extends CharacterCard>, List<CharacterCard>> characters;
    private final List<BuildingCard> buildings;

    public Tribe() {
        this.characters = Map.of(
            InventorCard.class, new ArrayList<>(),
            GathererCard.class, new ArrayList<>(),
            ShamanCard.class, new ArrayList<>(),
            BuilderCard.class, new ArrayList<>(),
            ArtistCard.class, new ArrayList<>(),
            HunterCard.class, new ArrayList<>()
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

    public int getNumCharacterOfType(Class<? extends CharacterCard> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        List<CharacterCard> cards = this.characters.get(type);
        if (cards == null) {
            throw new IllegalStateException("No character list configured for type: " + type);
        }
        return cards.size();
    }

    public Map<Class<? extends CharacterCard>, Integer> getNumCharactersOfEachType() {
        Map<Class<? extends CharacterCard>, Integer> output = new HashMap<>();
        characters.forEach((k, v) -> {
            output.put(k, v.size());
        });
        return output;
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

    public int countShamanStars() {
        List<CharacterCard> shamans = this.characters.get(ShamanCard.class);
        if (shamans.isEmpty()) {
            return 0;
        }

        int stars = 0;
        for (CharacterCard shaman : shamans) {
            // DOWN CASTING
            stars += ((ShamanCard) shaman).getStars();
        }
        return stars;
    }

    public int buildersDiscount() {
        List<CharacterCard> builders = this.characters.get(BuilderCard.class);
        if (builders.isEmpty()) {
            return 0;
        }

        int discount = 0;
        for (CharacterCard builder : builders) {
            // DOWN CASTING
            discount += ((BuilderCard) builder).getBuildingDiscount();
        }
        return discount;
    }

    public int buildersEndGamePrestige() {
        List<CharacterCard> builders = this.characters.get(BuilderCard.class);
        if (builders.isEmpty()) {
            return 0;
        }

        int prestige = 0;
        for (CharacterCard builder : builders) {
            // DOWN CASTING
            prestige += ((BuilderCard) builder).getPrestigePoints();
        }
        return prestige;
    }

    public int gatherersDiscount() {
        List<CharacterCard> gatherers = this.characters.get(GathererCard.class);
        if (gatherers.isEmpty()) {
            return 0;
        }

        int discount = 0;
        for (CharacterCard gatherer : gatherers) {
            // DOWN CASTING
            discount += ((GathererCard) gatherer).getDiscount();
        }
        return discount;
    }

    public Set<InventionIcon> inventionIcons() {
        List<CharacterCard> inventors = this.characters.get(InventorCard.class);
        Set<InventionIcon> icons = new HashSet<>();
        for (CharacterCard inventor : inventors) {
            // DOWN CASTING
            icons.add(((InventorCard) inventor).getInventionIcon());
        }
        return icons;
    }

    private int inventorsEndGamePrestige() {
        int inventor_count = getNumCharacterOfType(InventorCard.class);
        int different_icons = inventionIcons().size();
        return inventor_count * different_icons;
    }

    private int artistsEndGamePrestige() {
        int artist_count = getNumCharacterOfType(ArtistCard.class);
        return 10 * (artist_count / 2);
    }

    public int tribeEndGamePrestige() {
        int prestige = 0;
        prestige += buildingsPrestige();
        prestige += buildersEndGamePrestige();
        prestige += inventorsEndGamePrestige();
        prestige += artistsEndGamePrestige();
        return prestige;
    }

    public Class<? extends CharacterCard> addCard(CharacterCard card) {
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }

        Class<? extends CharacterCard> card_type = card.getClass();

        this.characters.get(card_type).add(card);

        return card_type;
    }

    public void addCard(BuildingCard card) {
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }
        this.buildings.add(card);
    }
}
