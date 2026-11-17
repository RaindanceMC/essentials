package de.raindancer.modules.essentials;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.core.ui.travel.TravelDoor;
import de.raindancer.modules.essentials.service.SpawnService;
import de.raindancer.modules.essentials.util.PermissionNodes;
import de.raindancer.core.ui.menu.Menu;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EssentialsOffersTest {

    @Test
    @DisplayName("join music is a preference in its own group")
    void joinMusic() {
        Preference preference = EssentialsOffers.joinMusic();

        assertThat(preference.id()).isEqualTo("essentials:join-music");
        assertThat(preference.group()).isEqualTo("Join music");
        assertThat(preference.title()).isEqualTo("Join music");
        assertThat(preference.isPage()).isFalse();
    }

    @Test
    @DisplayName("spawn is door 60 on /travel and goes where /spawn goes")
    void spawnDoor() {
        SpawnService spawn = mock(SpawnService.class);
        Player player = mock(Player.class);
        TravelDoor door = EssentialsOffers.spawnDoor(spawn);

        assertThat(door.id()).isEqualTo("essentials:spawn");
        assertThat(door.order()).isEqualTo(60);
        assertThat(door.title()).isEqualTo("Spawn");

        door.open().accept(player, null);
        verify(spawn).go(player);
    }

    @Test
    @DisplayName("spawn door is locked for whoever lacks the /spawn permission")
    void spawnDoorLocked() {
        Player player = mock(Player.class);
        TravelDoor door = EssentialsOffers.spawnDoor(mock(SpawnService.class));

        when(player.hasPermission(PermissionNodes.SPAWN)).thenReturn(false);
        assertThat(door.locked().apply(player)).isPresent();

        when(player.hasPermission(PermissionNodes.SPAWN)).thenReturn(true);
        assertThat(door.locked().apply(player)).isEmpty();
    }

    @Test
    @DisplayName("the ignore list is a page for whoever may /ignore, and opens with /preferences as parent")
    void ignoredPage() {
        Player player = mock(Player.class);
        Menu parent = mock(Menu.class);
        java.util.List<Object> opened = new java.util.ArrayList<>();
        Preference preference = EssentialsOffers.ignored((who, back) -> {
            opened.add(who);
            opened.add(back);
        });

        assertThat(preference.id()).isEqualTo("essentials:ignored");
        assertThat(preference.title()).isEqualTo("People you ignore");
        assertThat(preference.isPage()).isTrue();
        preference.page().accept(player, parent);
        assertThat(opened).containsExactly(player, parent);

        when(player.hasPermission(PermissionNodes.IGNORE)).thenReturn(false);
        assertThat(preference.shown().test(player)).isFalse();
        when(player.hasPermission(PermissionNodes.IGNORE)).thenReturn(true);
        assertThat(preference.shown().test(player)).isTrue();
    }

    @Test
    @DisplayName("the nickname is a page for whoever may /nick, and opens with /preferences as parent")
    void nicknamePage() {
        Player player = mock(Player.class);
        Menu parent = mock(Menu.class);
        java.util.List<Object> opened = new java.util.ArrayList<>();
        Preference preference = EssentialsOffers.nickname((who, back) -> {
            opened.add(who);
            opened.add(back);
        });

        assertThat(preference.id()).isEqualTo("essentials:nickname");
        assertThat(preference.title()).isEqualTo("Your nickname");
        assertThat(preference.isPage()).isTrue();
        preference.page().accept(player, parent);
        assertThat(opened).containsExactly(player, parent);

        when(player.hasPermission(PermissionNodes.NICK)).thenReturn(false);
        assertThat(preference.shown().test(player)).isFalse();
        when(player.hasPermission(PermissionNodes.NICK)).thenReturn(true);
        assertThat(preference.shown().test(player)).isTrue();
    }
}
