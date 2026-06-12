package it.polimi.ingsw.mesos.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.polimi.ingsw.mesos.Model.Cards.BuildingCard;
import it.polimi.ingsw.mesos.Model.Cards.Card;
import it.polimi.ingsw.mesos.Model.Cards.CharacterCard;
import it.polimi.ingsw.mesos.Model.Cards.Characters.*;
import it.polimi.ingsw.mesos.Model.Enums.BuildingTrigger;
import it.polimi.ingsw.mesos.Model.Enums.InventionIcon;

public class Tribe {
    private final Map<Class<? extends CharacterCard>, List<CharacterCard>> characters;
    private final List<BuildingCard> buildings;

    /**
     * Creates an empty tribe with one character bucket for each supported
     * character type and no buildings.
     */
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

    /**
     * Counts all character cards currently owned by the tribe.
     *
     * @return total number of character cards
     */
    public int getNumCharacters() {
        int total = 0;
        for (List<CharacterCard> cards : this.characters.values()) {
            total += cards.size();
        }
        return total;
    }

    /**
     * Counts the cards owned for one character type.
     *
     * @param type concrete character card class to count
     * @return number of owned cards of that type
     * @throws IllegalArgumentException if the type is null
     * @throws IllegalStateException if the type is not tracked by this tribe
     */
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

    /**
     * Builds a count map for every character type tracked by the tribe.
     *
     * @return map from character class to owned-card count
     */
    public Map<Class<? extends CharacterCard>, Integer> getNumCharactersOfEachType() {
        Map<Class<? extends CharacterCard>, Integer> output = new HashMap<>();
        characters.forEach((k, v) -> {
            output.put(k, v.size());
        });
        return output;
    }

    /**
     * Filters owned buildings by the trigger that activates their effect.
     *
     * @param trigger trigger to match
     * @return buildings that react to the given trigger
     * @throws IllegalArgumentException if the trigger is null
     */
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

    /**
     * Counts buildings currently owned by the tribe.
     *
     * @return number of owned buildings
     */
    public int getNumBuildings() {
        return this.buildings.size();
    }

    public List<CharacterCard> getCharacterCards() {
        List<CharacterCard> ownedCards = new ArrayList<>();
        List<Class<? extends CharacterCard>> orderedTypes = List.of(
                HunterCard.class,
                ShamanCard.class,
                ArtistCard.class,
                BuilderCard.class,
                InventorCard.class,
                GathererCard.class
        );

        for (Class<? extends CharacterCard> type : orderedTypes) {
            ownedCards.addAll(this.characters.get(type));
        }
        return List.copyOf(ownedCards);
    }

    public List<BuildingCard> getBuildingCards() {
        return List.copyOf(this.buildings);
    }

    // TODO: REMOVE THIS FUNCTION USEFUL: FOR DEBUG ONLY
    public List<Card> getCards() {
        List<Card> cards = new ArrayList<>();
        for (List<CharacterCard> character_cards : this.characters.values()) {
            cards.addAll(character_cards);
        }
        cards.addAll(this.buildings);
        return cards;
    }

    /**
     * Sums the printed prestige points of all buildings owned by the tribe.
     *
     * @return total building prestige
     */
    public int buildingsPrestige() {
        int prestige = 0;
        for (BuildingCard building : this.buildings) {
            prestige += building.getPrestigePoints();
        }
        return prestige;
    }

    /**
     * Counts the total ritual stars provided by the tribe's shaman cards.
     *
     * @return total shaman stars
     */
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

    /**
     * Computes the total food discount granted by builder cards when buying
     * buildings.
     *
     * @return total building-cost discount
     */
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

    /**
     * Computes the end-game prestige printed on owned builder cards.
     *
     * @return total builder prestige
     */
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

    /**
     * Computes the total sustenance discount granted by gatherer cards.
     *
     * @return total food requirement discount during sustenance events
     */
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

    /**
     * Collects the distinct invention icons represented by the tribe's inventor
     * cards.
     *
     * @return set of owned invention icons
     */
    public Set<InventionIcon> inventionIcons() {
        List<CharacterCard> inventors = this.characters.get(InventorCard.class);
        Set<InventionIcon> icons = new HashSet<>();
        for (CharacterCard inventor : inventors) {
            // DOWN CASTING
            icons.add(((InventorCard) inventor).getInventionIcon());
        }
        return icons;
    }

    /**
     * Computes inventor end-game prestige as inventor count multiplied by the
     * number of distinct invention icons.
     *
     * @return end-game prestige from inventors
     */
    private int inventorsEndGamePrestige() {
        int inventor_count = getNumCharacterOfType(InventorCard.class);
        int different_icons = inventionIcons().size();
        return inventor_count * different_icons;
    }

    /**
     * Computes artist end-game prestige by awarding ten points for each pair of
     * artists.
     *
     * @return end-game prestige from artist pairs
     */
    private int artistsEndGamePrestige() {
        int artist_count = getNumCharacterOfType(ArtistCard.class);
        return 10 * (artist_count / 2);
    }

    /**
     * Computes the full end-game prestige generated by the tribe, including
     * buildings and character-based scoring rules.
     * Does not include prestige from "endgame buildings".
     *
     * @return total end-game prestige generated by the tribe
     */
    public int tribeEndGamePrestige() {
        int prestige = 0;
        prestige += buildingsPrestige();
        prestige += buildersEndGamePrestige();
        prestige += inventorsEndGamePrestige();
        prestige += artistsEndGamePrestige();
        return prestige;
    }

    /**
     * Adds a character card to the bucket matching its concrete class.
     *
     * @param card character card to add
     * @return the concrete character class used as the bucket key
     * @throws IllegalArgumentException if the card is null
     * @throws NullPointerException if the card class is not one of the tracked
     *                              character types
     */
    public Class<? extends CharacterCard> addCard(CharacterCard card) {
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }

        Class<? extends CharacterCard> card_type = card.getClass();

        this.characters.get(card_type).add(card);

        return card_type;
    }

    /**
     * Adds a building card to the tribe's owned buildings.
     *
     * @param card building card to add
     * @throws IllegalArgumentException if the card is null
     */
    public void addCard(BuildingCard card) {
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }
        this.buildings.add(card);
    }
}
