package it.polimi.ingsw.mesos.Model.Cards.Characters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.polimi.ingsw.mesos.Model.Enums.Era;

public class ArtistCardTest {

    @Test
    void getCharacterTypeReturnsArtist(){
        ArtistCard artist = new ArtistCard(Era.SECOND, 0);
        assertEquals(CharacterType.ARTIST, artist.getCharacterType());
    }
}
