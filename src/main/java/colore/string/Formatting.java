package colore.string;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;

public class Formatting {
    public static final Style ITALIC = Style.EMPTY.withItalic(true);
    public static final Style RESET_STYLE = Style.EMPTY
            .withObfuscated(false)
            .withBold(false)
            .withStrikethrough(false)
            .withUnderlined(false)
            .withItalic(false)
            .applyFormat(ChatFormatting.WHITE);
//    private static final Map<Integer, ChatFormatting> TEXT_COLOR_TO_CHAT_FORMATTING;
//
//    static {
//        TEXT_COLOR_TO_CHAT_FORMATTING = Stream.of(ChatFormatting.values()).filter(ChatFormatting::isColor).collect(ImmutableMap.toImmutableMap(ChatFormatting::getColor, Function.identity()));
//    }

//    public static String componentToString(Component component, Style currStyle, Style resetStyle) {
//        StringBuilder sb = new StringBuilder();
//        Style lastStyle = currStyle;
//        for (var c : component.toFlatList()) {
//            Style style = c.getStyle();
//            if ((!style.isBold() && lastStyle.isBold() ||
//                    !style.isObfuscated() && lastStyle.isObfuscated() ||
//                    !style.isStrikethrough() && lastStyle.isStrikethrough() ||
//                    !style.isUnderlined() && lastStyle.isUnderlined() ||
//                    !style.isItalic() && lastStyle.isItalic()
//                ) && (style.getColor() == null ||
//                    lastStyle.getColor() != null && style.getColor().getValue() == lastStyle.getColor().getValue() ||
//                    resetStyle.getColor() != null && style.getColor().getValue() == resetStyle.getColor().getValue())
//            ) {
//                sb.append("&r");
//                lastStyle = style;
//            }
//            TextColor color = style.getColor();
//            if (color != null && (lastStyle.getColor() == null || lastStyle.getColor().getValue() != color.getValue())) {
//                ChatFormatting formatting = TEXT_COLOR_TO_CHAT_FORMATTING.get(color.getValue());
//                if (formatting != null) {
//                    sb.append('&');
//                    sb.append(formatting.getChar());
//                }
//            }
//            if (style.isObfuscated()) {
//                sb.append("&k");
//            }
//            if (style.isBold()) {
//                sb.append("&l");
//            }
//            if (style.isStrikethrough()) {
//                sb.append("&m");
//            }
//            if (style.isUnderlined()) {
//                sb.append("&n");
//            }
//            if (style.isItalic()) {
//                sb.append("&o");
//            }
//            sb.append(c.getString());
//        }
//        return sb.toString();
//    }

    public static MutableComponent formatString(String string, Style style, Style reset) {
        if (string.isEmpty()) return null;

        int length = string.length();
        ArrayList<MutableComponent> components = new ArrayList<>();
        Style currentStyle = style;
        String text = "";
        int start = 0;

        for (int k = start; k < length; ++k) {
            char c = string.charAt(k);
            char d;
            if (c == '&') {
                ++k;
                if (k >= length) break;
                d = string.charAt(k);
                if (d == '&') {
                    text = string.substring(start, k);
                } else {
                    ChatFormatting format = ChatFormatting.getByCode(d);
                    if (format == null) continue;
                    text = text.concat(string.substring(start, k - 1));
                    if (!text.isEmpty()) components.add(Component.literal(text).withStyle(currentStyle));
                    currentStyle = format == ChatFormatting.RESET ? reset : currentStyle.applyLegacyFormat(format);
                    text = "";
                }
                start = k + 1;
            }
        }

        if (start < length) {
            text = text.concat(string.substring(start, length));
        }

        if (!text.isEmpty()) {
            components.add(Component.literal(text).withStyle(currentStyle));
        }

        if (components.isEmpty()) return null;

        MutableComponent component = components.get(0);

        for (int i = 1; i < components.size(); i++) {
            component.append(components.get(i));
        }

        if (StringUtils.isBlank(component.getString())) {
            return null;
        }

        return component;
    }

    @Nullable
    private static MutableComponent componentFromJson(String s) {
        try {
            return Component.Serializer.fromJson(s);
        } catch (Exception ignored) {
            return null;
        }
    }


    private static @NotNull MutableComponent getLoreLineComponent(String line, Style style) {
        MutableComponent component = Component.literal(line).withStyle(style);
        // reset italic and purple color back to white text, purple doesn't really make sense as the default so use
        // white text as base
        if (!style.isItalic()) component = component.withStyle(Style.EMPTY.withItalic(false));
        if (style.getColor() == null) component = component.withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
        return component;
    }

    // Minecraft's formatted text APIs are terrible
    private static class BookContentVisitor implements FormattedText.StyledContentConsumer<Object> {
        private final ArrayList<Component> components;
        MutableComponent lastComponent;
        BookContentVisitor(ArrayList<Component> components) {
            this.components = components;
        }
        @Override
        public @NotNull Optional<Object> accept(Style style, String string) {
            String[] lines = StringUtils.split(string, "\r\n");
            if (lines.length == 0) {
                components.add(Component.empty());
                return Optional.empty();
            }
            String first = lines[0];
            int j = 0;
            if (lastComponent != null) {
                lastComponent.append(getLoreLineComponent(first, style));
                components.add(lastComponent);
                lastComponent = null;
                j = 1;
            }
            for (; j < lines.length - 1; j++) {
                String line = lines[j];
                components.add(getLoreLineComponent(line, style));
            }
            if (j >= lines.length) {
                return Optional.empty();
            }
            lastComponent = getLoreLineComponent(lines[j], style);
            return Optional.empty();
        }
    }

    public static ArrayList<Component> createLoreText(ItemStack writtenBook, boolean writable, boolean ampersandFormattingInWritableBooks) {
        CompoundTag tag = writtenBook.getTag();
        if (tag == null || !tag.contains("pages", Tag.TAG_LIST)) {
            return null;
        }
        ListTag pages = tag.getList("pages", ListTag.TAG_STRING);
        int numPages = pages.size();
        if (numPages == 0 || numPages == 1 && StringUtils.isBlank(pages.getString(0))) {
            return null;
        }
        ArrayList<Component> components = new ArrayList<>();
        for (int i = 0; i < numPages; i++) {
            String page = pages.getString(i);
            MutableComponent c = writable ? ampersandFormattingInWritableBooks ? formatString(page, Style.EMPTY, Style.EMPTY) : Component.literal(page) : componentFromJson(page);
            if (c != null) {
                BookContentVisitor visitor = new BookContentVisitor(components);
                c.visit(visitor, Style.EMPTY);
                if (visitor.lastComponent != null) components.add(visitor.lastComponent);
            }
        }
        if (components.isEmpty()) return null;
        return components;
    }

    public static void setLoreText(ItemStack result, ArrayList<Component> lore) {
        ListTag list = new ListTag();
        for (Component loreLine : lore) {
            list.add(StringTag.valueOf(Component.Serializer.toJson(loreLine)));
        }
        result.getOrCreateTagElement("display").put("Lore", list);
    }

    public static boolean hasLoreText(ItemStack input) {
        CompoundTag compoundTag = input.getTagElement("display");
        return compoundTag != null && compoundTag.contains("Lore", Tag.TAG_LIST);
    }

    public static void resetLoreText(ItemStack result) {
        CompoundTag compoundTag = result.getTagElement("display");
        if (compoundTag != null) {
            compoundTag.remove("Lore");
            if (compoundTag.isEmpty()) {
                result.removeTagKey("display");
            }
        }

        CompoundTag tag = result.getTag();
        if (tag != null && tag.isEmpty()) {
            result.setTag(null);
        }
    }
}
