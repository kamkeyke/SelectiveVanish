package net.kamkeyke.selective_vanish.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.UUID;

/**
 * Represents a player known by the server.
 *
 * <p>The UUID identifies the player permanently, while the name represents
 * the name currently associated with that UUID in the known-player cache.</p>
 *
 * @param uuid the UUID of the known player
 * @param name the player's name or selector
 */
public record KnownPlayer(UUID uuid, String name) {

    /**
     * Creates a chat component containing this player's name with an interactive
     * click event that copies the name to the clipboard.
     *
     * <p>The component also displays a hover tooltip indicating that the name
     * can be copied when the player hovers over it.</p>
     *
     * @return a {@link MutableComponent} containing the player's name with copy-to-clipboard
     *         and hover events configured
     */
    public MutableComponent toNameCopyNameComponent(){
        return Component.literal(this.name).withStyle(s -> s
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, this.name))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.raccooncore.copy.nickname.click")))
        );
    }

    public MutableComponent toNameCopyUUIDComponent(){
        return Component.literal(this.name).withStyle(s -> s
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, this.uuid.toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.raccooncore.copy.uuid.click")))
        );
    }

    public MutableComponent toUUIDCopyUUIDComponent(){
        return Component.literal(this.uuid.toString()).withStyle(s -> s
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, this.uuid.toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.raccooncore.copy.uuid.click")))
        );
    }
}

