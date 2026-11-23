package de.raindancer.modules.essentials.service;

import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.world.poi.Poi;
import de.raindancer.core.world.poi.PoiStore;
import de.raindancer.core.world.teleport.Travel;
import de.raindancer.core.world.teleport.Trip;
import de.raindancer.core.world.teleport.TravelWatcher;
import de.raindancer.modules.essentials.EssentialsSettings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpawnServiceTest {

    @Test
    @DisplayName("arriving at spawn says nothing: the player is standing there")
    void arrivingIsSilent() {
        PoiStore places = mock(PoiStore.class);
        Poi spawn = mock(Poi.class);
        Location destination = mock(Location.class);
        when(spawn.location()).thenReturn(Optional.of(destination));
        when(places.ofKind(SpawnService.KIND)).thenReturn(List.of(spawn));
        Travel travel = mock(Travel.class);
        Messages messages = mock(Messages.class);
        SpawnService service = new SpawnService(places, () -> { }, travel, messages, EssentialsSettings.DEFAULTS);
        Player player = mock(Player.class);

        service.go(player);

        ArgumentCaptor<TravelWatcher> watcher = ArgumentCaptor.forClass(TravelWatcher.class);
        verify(travel).go(any(Player.class), any(Location.class), any(Trip.class), watcher.capture());
        watcher.getValue().arrived(player, destination, Trip.to("spawn"));

        verifyNoInteractions(messages);
    }
}
