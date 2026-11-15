package de.raindancer.modules.essentials.listener;

import de.raindancer.modules.essentials.EssentialsServices;
import de.raindancer.modules.essentials.service.AfkService;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Using your hands counts as being here, not only walking, talking and typing commands. */
class EssentialsActivityTest {

    private final EssentialsServices services = mock(EssentialsServices.class);
    private final AfkService afk = mock(AfkService.class);
    private final Player player = mock(Player.class);
    private EssentialsSessionListener listener;

    @BeforeEach
    void wire() {
        when(services.afk()).thenReturn(afk);
        listener = new EssentialsSessionListener(services);
    }

    @Test
    @DisplayName("using an item counts, also when the click hit air and the event comes in cancelled")
    void usingAnItem() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.isCancelled()).thenReturn(true);

        listener.onInteract(event);

        verify(afk).activity(player);
    }

    @Test
    @DisplayName("breaking or placing a block counts")
    void clickingABlock() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);

        listener.onInteract(event);

        verify(afk).activity(player);
    }

    @Test
    @DisplayName("being pushed onto a pressure plate does not count")
    void steppingOnAPlate() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getAction()).thenReturn(Action.PHYSICAL);

        listener.onInteract(event);

        verify(afk, never()).activity(player);
    }

    @Test
    @DisplayName("right-clicking a mob or a villager counts")
    void clickingAnEntity() {
        PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
        when(event.getPlayer()).thenReturn(player);

        listener.onInteractEntity(event);

        verify(afk).activity(player);
    }

    @Test
    @DisplayName("hitting something counts")
    void attacking() {
        PrePlayerAttackEntityEvent event = mock(PrePlayerAttackEntityEvent.class);
        when(event.getPlayer()).thenReturn(player);

        listener.onAttack(event);

        verify(afk).activity(player);
    }

    @Test
    @DisplayName("scrolling the hotbar, swapping hands and dropping an item count")
    void handlingItems() {
        PlayerItemHeldEvent held = mock(PlayerItemHeldEvent.class);
        when(held.getPlayer()).thenReturn(player);
        PlayerSwapHandItemsEvent swap = mock(PlayerSwapHandItemsEvent.class);
        when(swap.getPlayer()).thenReturn(player);
        PlayerDropItemEvent drop = mock(PlayerDropItemEvent.class);
        when(drop.getPlayer()).thenReturn(player);

        listener.onHotbar(held);
        listener.onSwapHands(swap);
        listener.onDrop(drop);

        verify(afk, org.mockito.Mockito.times(3)).activity(player);
    }

    @Test
    @DisplayName("clicking or dragging in an inventory counts")
    void usingAnInventory() {
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        when(click.getWhoClicked()).thenReturn(player);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getWhoClicked()).thenReturn(player);

        listener.onInventoryClick(click);
        listener.onInventoryDrag(drag);

        verify(afk, org.mockito.Mockito.times(2)).activity(player);
    }

    @Test
    @DisplayName("writing a sign or a book counts")
    void writing() {
        SignChangeEvent sign = mock(SignChangeEvent.class);
        when(sign.getPlayer()).thenReturn(player);
        when(sign.getSide()).thenReturn(Side.FRONT);
        PlayerEditBookEvent book = mock(PlayerEditBookEvent.class);
        when(book.getPlayer()).thenReturn(player);

        listener.onSign(sign);
        listener.onBook(book);

        verify(afk, org.mockito.Mockito.times(2)).activity(player);
    }
}
