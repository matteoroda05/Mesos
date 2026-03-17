package it.polimi.ingsw.mesos.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Cards.ArtistCard;
import it.polimi.ingsw.mesos.Model.Enums.CharacterType;
import it.polimi.ingsw.mesos.Model.Enums.Era;

public class ArtistCardTest {

    @Test
    void getCharacterTypeReturnsArtist(){
        ArtistCard artist = new ArtistCard(Era.SECOND, 0);
        assertEquals(CharacterType.ARTIST, artist.getCharacterType());
    }
}
