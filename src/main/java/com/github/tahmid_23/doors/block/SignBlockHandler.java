package com.github.tahmid_23.doors.block;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class SignBlockHandler implements BlockHandler {

    public static final NamespaceID HANDLER_ID = NamespaceID.from("minecraft", "sign");

    private final Collection<Tag<?>> blockEntityTags = List.of(Tag.String("Text1"));

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return blockEntityTags;
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return HANDLER_ID;
    }
}
