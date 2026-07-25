package com.pvp_utils.client;

import com.google.gson.JsonParser;
import com.pvp_utils.PVPUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.block.entity.SignText;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerTranslationContents {
    private static final ReferenceQueue<TranslatableContents> QUEUE = new ReferenceQueue<>();
    private static final Set<IdentityWeakReference> SERVER_CONTENTS = ConcurrentHashMap.newKeySet();
    private static final Map<SignKey, String[]> PENDING_SIGN_UPDATES = new ConcurrentHashMap<>();
    private static final Set<String> VANILLA_TRANSLATION_KEYS = loadVanillaTranslationKeys();
    private static final Map<String, Set<String>> SERVER_PACK_TRANSLATION_KEYS = new HashMap<>();

    private ServerTranslationContents() {
    }

    public static void markComponent(Component component) {
        mark(component, new IdentityHashMap<>());
    }

    public static boolean isServerContents(TranslatableContents contents) {
        purgeCollectedContents();
        return SERVER_CONTENTS.contains(new IdentityWeakReference(contents));
    }

    public static void trackSignText(BlockPos pos, boolean frontText, SignText text) {
        String[] lines = new String[4];
        for (int line = 0; line < lines.length; line++) {
            Component component = text.getMessage(line, false);
            markComponent(component);
            lines[line] = component.getString();
        }
        PENDING_SIGN_UPDATES.put(new SignKey(pos, frontText), lines);
    }

    public static ServerboundSignUpdatePacket replaceSignUpdate(ServerboundSignUpdatePacket packet) {
        String[] lines = PENDING_SIGN_UPDATES.remove(new SignKey(packet.getPos(), packet.isFrontText()));
        if (lines == null) {
            return packet;
        }
        return new ServerboundSignUpdatePacket(packet.getPos(), packet.isFrontText(), lines[0], lines[1], lines[2], lines[3]);
    }

    private static void mark(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || visited.put(value, Boolean.TRUE) != null) {
            return;
        }
        if (value instanceof Component component) {
            if (component.getContents() instanceof TranslatableContents contents) {
                if (!isAllowedTranslationKey(contents.getKey())) {
                    purgeCollectedContents();
                    SERVER_CONTENTS.add(new IdentityWeakReference(contents, QUEUE));
                }
                for (Object argument : contents.getArgs()) {
                    mark(argument, visited);
                }
            }
            for (Component sibling : component.getSiblings()) {
                mark(sibling, visited);
            }
            return;
        }
    }

    private static Set<String> loadVanillaTranslationKeys() {
        IoSupplier<InputStream> resource = Minecraft.getInstance().getVanillaPackResources().getResource(
                PackType.CLIENT_RESOURCES,
                Identifier.withDefaultNamespace("lang/en_us.json")
        );
        if (resource == null) {
            PVPUtils.LOGGER.warn("Could not find the vanilla English language file");
            return Set.of();
        }

        try (Reader reader = new InputStreamReader(resource.get(), StandardCharsets.UTF_8)) {
            return Set.copyOf(JsonParser.parseReader(reader).getAsJsonObject().keySet());
        } catch (IOException exception) {
            PVPUtils.LOGGER.warn("Could not load vanilla translation keys", exception);
            return Set.of();
        }
    }

    private static synchronized boolean isAllowedTranslationKey(String key) {
        if (VANILLA_TRANSLATION_KEYS.contains(key)) {
            return true;
        }

        Set<String> activePackIds = new HashSet<>();
        Set<String> translationKeys = new HashSet<>();
        Minecraft.getInstance().getResourceManager().listPacks().forEach(pack -> {
            if (pack.location().source() != PackSource.SERVER) {
                return;
            }
            activePackIds.add(pack.packId());
            translationKeys.addAll(SERVER_PACK_TRANSLATION_KEYS.computeIfAbsent(pack.packId(), ignored -> loadTranslationKeys(pack)));
        });
        SERVER_PACK_TRANSLATION_KEYS.keySet().retainAll(activePackIds);
        return translationKeys.contains(key);
    }

    private static Set<String> loadTranslationKeys(PackResources pack) {
        Set<String> keys = new HashSet<>();
        for (String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
            pack.listResources(PackType.CLIENT_RESOURCES, namespace, "lang", (id, resource) -> {
                if (!id.getPath().endsWith(".json")) {
                    return;
                }
                try (Reader reader = new InputStreamReader(resource.get(), StandardCharsets.UTF_8)) {
                    keys.addAll(JsonParser.parseReader(reader).getAsJsonObject().keySet());
                } catch (IOException | IllegalStateException exception) {
                    PVPUtils.LOGGER.warn("Could not load translation keys from server resource pack {}", pack.packId(), exception);
                }
            });
        }
        return Set.copyOf(keys);
    }

    private static void purgeCollectedContents() {
        IdentityWeakReference reference;
        while ((reference = (IdentityWeakReference) QUEUE.poll()) != null) {
            SERVER_CONTENTS.remove(reference);
        }
    }

    private static final class IdentityWeakReference extends WeakReference<TranslatableContents> {
        private final int hashCode;

        private IdentityWeakReference(TranslatableContents contents) {
            super(contents);
            hashCode = System.identityHashCode(contents);
        }

        private IdentityWeakReference(TranslatableContents contents, ReferenceQueue<TranslatableContents> queue) {
            super(contents, queue);
            hashCode = System.identityHashCode(contents);
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof IdentityWeakReference reference && get() == reference.get();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private record SignKey(BlockPos pos, boolean frontText) {
    }
}
