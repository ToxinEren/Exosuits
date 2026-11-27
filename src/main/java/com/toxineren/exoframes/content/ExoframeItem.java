package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.CompatItem;
import com.toxineren.exoframes.compat.ItemDataAccess.FrameItemData;
import com.toxineren.exoframes.core.frame.FrameState;
import com.toxineren.exoframes.entity.ExoframeEntity;
import com.toxineren.exoframes.frame.FrameLog;
import com.toxineren.exoframes.frame.FramePlacement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * L'oggetto telaio (design, par. 5.4): usato su un blocco piazza un telaio vuoto (identita' nuova, nulla di montato) al
 * suolo sotto il lato cliccato, rivolto verso chi lo usa. Un oggetto fabbricato non ha dati, e chi lo usa ne diventa il
 * proprietario; quello che torna dalla raccolta di un telaio ({@link #pickedUp}) porta sblocco, rinforzo e, se a
 * raccoglierlo non e' stato il proprietario, il proprietario, e la posa li rilegge (design M4 §5.11).
 */
public final class ExoframeItem extends CompatItem {
    /** Messaggio sulla barra delle azioni quando il telaio li' non ci sta. */
    public static final String NO_ROOM = "message." + RedstoneExoframes.MOD_ID + ".no_room";
    /**
     * Messaggio sulla barra delle azioni quando l'oggetto porta pezzi o una cella nei dati (solo un comando li scrive):
     * si posa solo un telaio vuoto.
     */
    public static final String FULL = "message." + RedstoneExoframes.MOD_ID + ".frame_item_full";
    /**
     * Quanti blocchi al massimo il telaio scende dal punto richiesto fino al suolo: oltre, l'oggetto rifiuta come se
     * non ci fosse spazio (usato sul fianco di un dirupo non manda il telaio in fondo). L'uscita e la morte non hanno
     * questo limite.
     */
    public static final double MAX_DROP = 3.0;

    /** Le chiavi delle righe grigie del suggerimento, nell'ordine: piazzarlo, montare, entrare, togliere le cose. */
    private static final List<String> TOOLTIP_KEYS = List.of(
            "tooltip." + RedstoneExoframes.MOD_ID + ".exoframe.place",
            "tooltip." + RedstoneExoframes.MOD_ID + ".exoframe.mount",
            "tooltip." + RedstoneExoframes.MOD_ID + ".exoframe.enter",
            "tooltip." + RedstoneExoframes.MOD_ID + ".exoframe.unmount");
    /** La riga del suggerimento di un oggetto che porta lo sblocco. */
    private static final String TOOLTIP_UNLOCKED = "tooltip." + RedstoneExoframes.MOD_ID + ".exoframe.unlocked";
    /** La riga del suggerimento di un oggetto che porta il rinforzo. */
    private static final String TOOLTIP_REINFORCED = "tooltip." + RedstoneExoframes.MOD_ID + ".exoframe.reinforced";

    /**
     * Tutte le chiavi dei testi dell'oggetto, che i due file di lingua devono avere: le quattro righe grigie del
     * suggerimento, le due delle bandierine e {@link #FULL}.
     */
    public static final List<String> TEXT_KEYS = Stream.concat(TOOLTIP_KEYS.stream(),
            Stream.of(TOOLTIP_UNLOCKED, TOOLTIP_REINFORCED, FULL)).toList();

    public ExoframeItem(Item.Properties properties) {
        super(properties);
    }

    /**
     * L'oggetto telaio che torna a chi raccoglie a mano un telaio vuoto, fatto con lo stato del telaio letto prima di
     * toglierlo dal registro (design M4 §5.11). L'oggetto porta lo sblocco e il rinforzo del telaio, e il proprietario
     * solo se il telaio ne ha uno e a raccoglierlo e' un altro giocatore: un telaio sbloccato, prestato e raccolto da
     * un altro, resta cosi' del suo proprietario anche quando lo si posa di nuovo, mentre un telaio raccolto dal suo
     * proprietario e' libero, e chi lo posa ne diventa il proprietario. Quando non c'e' nulla da portare (nessun
     * proprietario da conservare, ne' sbloccato ne' rinforzato) l'oggetto resta senza dati, uguale a uno fabbricato.
     * Pezzi e cella non ci sono mai: si raccoglie solo un telaio vuoto.
     *
     * @param picker chi raccoglie il telaio
     */
    public static ItemStack pickedUp(FrameState state, UUID picker) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(picker, "picker must not be null");
        ItemStack stack = new ItemStack(ModItems.EXOFRAME.get());
        Optional<UUID> owner = Optional.ofNullable(state.owner()).filter(o -> !o.equals(picker));
        if (owner.isEmpty() && !state.unlocked() && !state.reinforced()) {
            return stack;
        }
        Compat.itemData().setFrame(stack, new FrameItemData(owner, state.unlocked(), state.reinforced(),
                Optional.empty(), Map.of()));
        return stack;
    }

    /**
     * Le righe "Sbloccato" e "Rinforzato" in oro, quando i dati dell'oggetto le portano, e poi quattro righe grigie che
     * insegnano i comandi: piazzarlo, montare pezzi e cella, entrare, togliere le cose. Il proprietario non si mostra
     * mai.
     */
    @Override
    public void tooltip(ItemStack stack, List<Component> lines, boolean advanced) {
        Optional<FrameItemData> data = Compat.itemData().frame(stack);
        if (data.isPresent() && data.get().unlocked()) {
            lines.add(Component.translatable(TOOLTIP_UNLOCKED).withStyle(ChatFormatting.GOLD));
        }
        if (data.isPresent() && data.get().reinforced()) {
            lines.add(Component.translatable(TOOLTIP_REINFORCED).withStyle(ChatFormatting.GOLD));
        }
        for (String key : TOOLTIP_KEYS) {
            lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Sul client dice solo che l'uso e' riuscito. Sul server, in quest'ordine: un oggetto che porta pezzi o una cella
     * nei dati non si posa (chi lo usa vede {@link #FULL} sulla barra delle azioni, e la pila resta com'e'); lo stato
     * nuovo ha come proprietario quello dei dati, se c'e', altrimenti chi lo usa (nessuno senza giocatore), e lo
     * sblocco e il rinforzo dei dati (falsi senza dati); poi piazza il telaio al suolo sotto
     * {@code Vec3.atBottomCenterOf} del blocco accanto alla faccia cliccata, con l'orientamento di chi lo usa girato di
     * 180 gradi, e toglie un oggetto dalla pila se chi lo usa non e' in creativa. Se il telaio non ci sta (o sotto non
     * c'e' suolo, o il suolo e' piu' di {@link #MAX_DROP} blocchi sotto il punto, o il punto e' fuori dal bordo del
     * mondo) la pila resta com'e', non nasce alcun record e chi lo usa vede {@link #NO_ROOM} sulla barra delle azioni.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        Optional<FrameItemData> data = Compat.itemData().frame(context.getItemInHand());
        if (data.isPresent() && (!data.get().pieces().isEmpty() || data.get().cellEnergy().isPresent())) {
            if (player != null) {
                player.displayClientMessage(Component.translatable(FULL), true);
            }
            return InteractionResult.FAIL;
        }
        FrameState state = new FrameState(RedstoneExoframes.config());
        Optional<UUID> owner = data.flatMap(FrameItemData::owner);
        if (owner.isPresent()) {
            state.setOwner(owner.get());
        } else if (player != null) {
            state.setOwner(player.getUUID());
        }
        state.setUnlocked(data.isPresent() && data.get().unlocked());
        state.setReinforced(data.isPresent() && data.get().reinforced());
        Vec3 from = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        float yaw = Mth.wrapDegrees(context.getRotation() + 180f);
        Optional<ExoframeEntity> placed = FramePlacement.groundBelow(level, from)
                .filter(feet -> from.y - feet.y <= MAX_DROP)
                .flatMap(feet -> FramePlacement.place(level, state, feet, yaw));
        if (placed.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable(NO_ROOM), true);
            }
            return InteractionResult.FAIL;
        }
        FrameLog.event(placed.get().frameId(), "placed", player, level, placed.get().position());
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
