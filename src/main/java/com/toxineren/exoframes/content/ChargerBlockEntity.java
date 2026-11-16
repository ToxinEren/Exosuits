package com.toxineren.exoframes.content;

import com.toxineren.exoframes.RedstoneExoframes;
import com.toxineren.exoframes.compat.Compat;
import com.toxineren.exoframes.compat.CompatBlockEntity;
import com.toxineren.exoframes.compat.NbtKeys;
import com.toxineren.exoframes.core.config.ConfigValidation;
import com.toxineren.exoframes.core.config.ExoframesConfig;
import com.toxineren.exoframes.core.energy.ChargerRule;
import com.toxineren.exoframes.frame.Handouts;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * La block entity del caricatore (design §12.11): tiene il contenuto e chiama la regola di carica
 * ({@link ChargerRule}) una volta per tick del server. Solo un operatore puo' scriverne l'NBT con un comando.
 * <p>
 * <b>Come una fornace.</b> Slot 0: una cella. Slot 1: polvere di redstone, uno stack normale fino a
 * {@link #MAX_DUST}. In piu' la riserva, i RE della polvere che sta bruciando, e il riporto del ritmo in millesimi di
 * RE. A ogni tick, se c'e' una cella non piena, la regola brucia al piu' una polvere e passa RE dalla riserva alla
 * cella; qui si applica solo cio' che e' cambiato.
 * <p>
 * <b>Due veri {@link ItemStack}.</b> In memoria cella e polvere sono stack veri, e {@link #cell()} e {@link #dust()}
 * restituiscono proprio quelli, mai una copia: le tramogge cambiano sul posto lo stack che un contenitore restituisce
 * (ne tolgono o ne aggiungono oggetti), e una copia perderebbe o duplicherebbe cio' che spostano. Il tick legge la
 * carica dallo stack della cella e ce la riscrive quando cambia. Nei dati del chunk invece non c'e' nessuno stack:
 * cinque numeri alla radice del tag ({@code NbtKeys.CHARGER_*}), perche' una cella e' solo la sua carica (design
 * §12.10) e la polvere solo un numero. Per questo una cella che entra diventa subito uno stack nuovo con la sola
 * carica ({@link #setCell}): il nome dato con l'incudine e ogni altro dato si perdono all'ingresso, sempre, e non al
 * primo salvataggio.
 * <p>
 * <b>La carica non si taglia qui.</b> Ne' la lettura ({@link #readData}) ne' {@link #setCell} riducono la carica alla
 * capacita' della config. La lettura, perche' un avvio con la capacita' abbassata, o con il file di config rovinato
 * (valgono i predefiniti), non deve distruggere energia salvata. {@link #setCell}, perche' non distingue un ingresso
 * da un rimettere: chi toglie una cella e non riesce a consegnarla (una tramoggia senza posto) la rimette per questa
 * strada, e un taglio qui distruggerebbe la sua carica. Una cella sopra la capacita' vale piena: la regola non la
 * tocca. Il taglio di design §12.10 e' di chi inserisce una cella a mano, prima di chiamare {@link #setCell}.
 * <p>
 * <b>Lo stato del blocco lo scrive solo il tick.</b> {@link #setCell} e {@link #setDust} cambiano i campi e chiamano
 * {@code setChanged()} (chunk da salvare, comparatori vicini aggiornati); le proprieta' {@code cell} e {@code lit} le
 * ricava {@link #serverTick} una volta per tick, e le scrive solo se differiscono.
 * <p>
 * <b>Tramogge.</b> Il caricatore e' un {@link WorldlyContainer} del gioco base, senza codice per loader: slot 0 la
 * cella, slot 1 la polvere. Da sopra entra una cella nello slot 0, solo se e' vuoto; dai lati entra la polvere di
 * redstone nello slot 1, finche' ne tiene meno di {@link #MAX_DUST}; da sotto esce la cella, solo quando e' piena
 * (carica almeno pari alla capacita' della config). Ogni altro passaggio e' rifiutato. I limiti stanno in
 * {@link #canPlaceItem} e nelle facce ({@link #canPlaceItemThroughFace}, {@link #canTakeItemThroughFace}), mai in
 * {@code getMaxStackSize()}, che qui non si ridefinisce: le tramogge ignorano la dimensione massima di un
 * contenitore. Due fatti delle tramogge decidono il
 * resto: quella che spinge in uno slot non vuoto fa crescere sul posto lo stack avuto da {@link #getItem} (per questo
 * mai una copia); quella che tira e non ha dove mettere la cella l'ha gia' tolta con {@link #removeItem} e la rimette
 * con {@link #setItem}, a ogni tick finche' non trova posto (per questo {@link #setItem}, come {@link #setCell}, non
 * taglia la carica). Una cella che entra da una tramoggia tiene quindi la sua carica anche sopra la capacita', e vale
 * piena.
 * <p>
 * <b>Limite dichiarato</b> (design §7 punto 27, §8 domanda 10). Il contenuto sta nei dati del chunk e si salva con il
 * salvataggio automatico, come quello di un forziere del gioco base; non sta nel registro dei telai. Un inserimento o
 * un ritiro seguito dall'uscita del giocatore e da un crash prima del salvataggio puo' duplicare o perdere una cella.
 * In M3 nessun rimedio.
 */
public final class ChargerBlockEntity extends CompatBlockEntity implements WorldlyContainer {
    /** Quante polveri di redstone tiene al piu' lo slot 1. */
    public static final int MAX_DUST = 64;

    /** Lo slot della cella. */
    private static final int SLOT_CELL = 0;
    /** Lo slot della polvere di redstone. */
    private static final int SLOT_DUST = 1;
    /** Gli slot che si vedono da sopra e da sotto: la cella. */
    private static final int[] CELL_SLOTS = {SLOT_CELL};
    /** Gli slot che si vedono dai quattro lati: la polvere. */
    private static final int[] DUST_SLOTS = {SLOT_DUST};

    /** Il riporto e' in millesimi di RE: sempre sotto un RE intero. */
    private static final int MAX_CARRY_MILLI = 999;

    /** Le posizioni il cui tick ha gia' scritto la riga ERROR da quando il gioco e' partito. */
    private static final Set<GlobalPos> FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    /** Slot 0: la cella, uno stack da uno, o {@link ItemStack#EMPTY}. */
    private ItemStack cell = ItemStack.EMPTY;
    /** Slot 1: la polvere di redstone, o {@link ItemStack#EMPTY}. */
    private ItemStack dust = ItemStack.EMPTY;
    /** RE della polvere che sta bruciando, in [0, {@link ConfigValidation#MAX_RE_PER_REDSTONE}]. */
    private int reserve;
    /** Riporto del ritmo di carica, in millesimi di RE, in [0, 999]. */
    private int carryMilli;

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGER.get(), pos, state);
    }

    /**
     * Un tick del server del caricatore: il ticker del blocco lo chiama e basta. Senza cella non carica; con la cella
     * chiama {@link ChargerRule#tick} con i valori della config e applica solo cio' che e' cambiato. Poi ricava
     * {@code cell} e {@code lit} e scrive lo stato del blocco solo se differisce.
     * <p>
     * Non lancia mai una {@link RuntimeException}: la prima per posizione (dimensione compresa) scrive una riga ERROR,
     * le altre passano in silenzio fino alla chiusura del gioco (design §12, regole di tutta la 3c).
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargerBlockEntity charger) {
        try {
            charger.tick(level, pos, state);
        } catch (RuntimeException e) {
            if (FAILURES_LOGGED.add(GlobalPos.of(level.dimension(), pos))) {
                RedstoneExoframes.LOGGER.error("Charger tick failed at {} in {}", pos, level.dimension().location(),
                        e);
            }
        }
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        boolean charging = false;
        if (!cell.isEmpty()) {
            ExoframesConfig.Energy cfg = RedstoneExoframes.config().energy;
            int stored = Compat.itemData().cellEnergy(cell);
            ChargerRule.Result r = ChargerRule.tick(stored, cfg.cellCapacity, dust.getCount(), reserve, carryMilli,
                    cfg.chargerRePerRedstone, cfg.chargerRePerSecond);
            boolean changed = false;
            if (r.stored() != stored) {
                Compat.itemData().setCellEnergy(cell, r.stored());
                changed = true;
            }
            if (r.dust() != dust.getCount()) {
                // la regola brucia al piu' una polvere per tick
                dust.shrink(1);
                if (dust.isEmpty()) {
                    dust = ItemStack.EMPTY;
                }
                changed = true;
            }
            if (r.reserve() != reserve) {
                reserve = r.reserve();
                changed = true;
            }
            if (r.carryMilli() != carryMilli) {
                carryMilli = r.carryMilli();
                changed = true;
            }
            if (changed) {
                setChanged();
            }
            charging = r.charging();
        }
        boolean hasCell = !cell.isEmpty();
        if (state.getValue(ChargerBlock.CELL) != hasCell || state.getValue(ChargerBlock.LIT) != charging) {
            level.setBlock(pos, state.setValue(ChargerBlock.CELL, hasCell).setValue(ChargerBlock.LIT, charging), 3);
        }
    }

    /** Lo stack vero dello slot 0, la cella; {@link ItemStack#EMPTY} se non c'e'. Mai una copia. */
    public ItemStack cell() {
        return cell;
    }

    /** Lo stack vero dello slot 1, la polvere di redstone; {@link ItemStack#EMPTY} se non c'e'. Mai una copia. */
    public ItemStack dust() {
        return dust;
    }

    /**
     * Mette una cella nello slot 0, o lo svuota con uno stack vuoto. Lo slot prende uno stack nuovo con la sola carica
     * di {@code stack}, non tagliata alla capacita' della config (vedi la classe); {@code stack} non si tocca. Non
     * scrive lo stato del blocco: lo fa il tick.
     *
     * @throws IllegalArgumentException se {@code stack} non e' vuoto e non e' una sola cella
     */
    public void setCell(ItemStack stack) {
        if (stack.isEmpty()) {
            cell = ItemStack.EMPTY;
        } else {
            if (!(stack.getItem() instanceof CellItem) || stack.getCount() != 1) {
                throw new IllegalArgumentException("not a single redstone cell: " + stack);
            }
            ItemStack fresh = new ItemStack(ModItems.REDSTONE_CELL.get());
            Compat.itemData().setCellEnergy(fresh, Compat.itemData().cellEnergy(stack));
            cell = fresh;
        }
        setChanged();
    }

    /**
     * Mette la polvere nello slot 1, o lo svuota con uno stack vuoto. Lo slot tiene proprio {@code stack}, non una
     * copia: le tramogge lo fanno crescere sul posto. Non scrive lo stato del blocco: lo fa il tick.
     *
     * @throws IllegalArgumentException se {@code stack} non e' vuoto e non e' polvere di redstone, o ne ha piu' di
     *                                  {@link #MAX_DUST}
     */
    public void setDust(ItemStack stack) {
        if (stack.isEmpty()) {
            dust = ItemStack.EMPTY;
        } else {
            if (!stack.is(Items.REDSTONE) || stack.getCount() > MAX_DUST) {
                throw new IllegalArgumentException("not redstone dust, or more than " + MAX_DUST + ": " + stack);
            }
            dust = stack;
        }
        setChanged();
    }

    /** Due slot: 0 la cella, 1 la polvere di redstone. */
    @Override
    public int getContainerSize() {
        return 2;
    }

    /** Vero se non c'e' ne' la cella ne' la polvere; riserva e riporto non contano. */
    @Override
    public boolean isEmpty() {
        return cell.isEmpty() && dust.isEmpty();
    }

    /** Lo stack vero dello slot ({@link #cell()} o {@link #dust()}), mai una copia; vuoto per ogni altro slot. */
    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case SLOT_CELL -> cell;
            case SLOT_DUST -> dust;
            default -> ItemStack.EMPTY;
        };
    }

    /**
     * Toglie dallo slot al piu' {@code amount} oggetti e li restituisce. Dallo slot 0 esce lo stack stesso della cella
     * (lo stesso oggetto, non una copia) e lo slot resta vuoto; dallo slot 1 escono le polveri chieste, e lo slot
     * resta vuoto se non ne restano. Chiama {@code setChanged()} solo se e' uscito qualcosa. Con {@code amount} sotto
     * 1, o per un altro slot, non esce nulla.
     */
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ItemStack.EMPTY;
        if (amount >= 1) {
            if (slot == SLOT_CELL) {
                removed = cell;
                cell = ItemStack.EMPTY;
            } else if (slot == SLOT_DUST && !dust.isEmpty()) {
                removed = dust.split(amount);
                if (dust.isEmpty()) {
                    dust = ItemStack.EMPTY;
                }
            }
        }
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    /** Toglie e restituisce lo stack dello slot, senza {@code setChanged()}; vuoto per ogni altro slot. */
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = getItem(slot);
        if (slot == SLOT_CELL) {
            cell = ItemStack.EMPTY;
        } else if (slot == SLOT_DUST) {
            dust = ItemStack.EMPTY;
        }
        return removed;
    }

    /**
     * Slot 0: {@link #setCell}; slot 1: {@link #setDust}. Non taglia mai la carica di una cella: e' anche il rimettere
     * di una tramoggia che l'ha tolta e non ha trovato dove metterla (vedi la classe).
     * <p>
     * Lancia per uno stack che {@link #canPlaceItem} rifiuterebbe perche' non e' cio' che lo slot tiene (non una sola
     * cella, non polvere di redstone, piu' di {@link #MAX_DUST}) e per ogni altro slot: tramogge e distributori
     * chiedono prima {@link #canPlaceItem}.
     *
     * @throws IllegalArgumentException per uno stack non vuoto che lo slot non puo' tenere, o per un altro slot
     */
    @Override
    public void setItem(int slot, ItemStack stack) {
        switch (slot) {
            case SLOT_CELL -> setCell(stack);
            case SLOT_DUST -> setDust(stack);
            default -> throw new IllegalArgumentException("no such slot in a charger: " + slot);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    /** Svuota i due slot senza far cadere nulla (lo usa {@code /setblock}); riserva e riporto restano. */
    @Override
    public void clearContent() {
        if (isEmpty()) {
            return;
        }
        cell = ItemStack.EMPTY;
        dust = ItemStack.EMPTY;
        setChanged();
    }

    /**
     * Che cosa puo' entrare in uno slot, da qualunque parte arrivi. Slot 0: una sola cella, e solo se lo slot e' vuoto
     * (uno stack di due celle no). Slot 1: polvere di redstone, e solo finche' lo slot ne tiene meno di
     * {@link #MAX_DUST}: il limite non si affida al massimo dello stack della polvere, che su 1.21.1 e' un dato
     * dell'oggetto. Ogni altro slot: nulla. I limiti stanno qui e nelle facce, non in {@code getMaxStackSize()}.
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_CELL -> stack.getItem() instanceof CellItem && stack.getCount() == 1 && cell.isEmpty();
            case SLOT_DUST -> stack.is(Items.REDSTONE) && dust.getCount() < MAX_DUST;
            default -> false;
        };
    }

    /** Da sopra e da sotto si vede lo slot 0, la cella; dai quattro lati lo slot 1, la polvere. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP || side == Direction.DOWN ? CELL_SLOTS : DUST_SLOTS;
    }

    /**
     * Da sopra entra solo cio' che {@link #canPlaceItem} accetta nello slot 0 (una cella, a slot vuoto); da un lato
     * solo cio' che accetta nello slot 1 (polvere di redstone, finche' ne tiene meno di {@link #MAX_DUST}). Da sotto,
     * o senza una faccia ({@code null}), nulla.
     */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (face == null || face == Direction.DOWN) {
            return false;
        }
        int faceSlot = face == Direction.UP ? SLOT_CELL : SLOT_DUST;
        return slot == faceSlot && canPlaceItem(slot, stack);
    }

    /**
     * Esce solo la cella, solo da sotto e solo quando e' piena: la carica della cella dello slot 0 e' almeno la
     * capacita' della config (una cella che tiene di piu' vale piena). La polvere non esce da nessuna faccia.
     */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return face == Direction.DOWN && slot == SLOT_CELL && !cell.isEmpty() && storedEnergy() >= capacity();
    }

    /**
     * Lascia a terra, ferme, al centro del blocco, la cella (con la sua carica) e la polvere, e svuota i due slot: lo
     * chiama il blocco quando viene rotto. La riserva della polvere che stava bruciando non si rimborsa (design
     * §12.11). Un caricatore vuoto, o non ancora in un livello, non fa nulla.
     */
    public void dropContents() {
        if (level == null || isEmpty()) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(worldPosition);
        Handouts.dropStill(level, center, cell);
        Handouts.dropStill(level, center, dust);
        cell = ItemStack.EMPTY;
        dust = ItemStack.EMPTY;
        setChanged();
    }

    /** I RE della polvere che sta bruciando, non ancora passati alla cella. */
    public int reserve() {
        return reserve;
    }

    /** Il riporto del ritmo di carica, in millesimi di RE, in [0, 999]. */
    public int carryMilli() {
        return carryMilli;
    }

    /** Lo stato da dire al giocatore, in ordine di precedenza ({@link ChargerRule#status}). */
    public ChargerRule.Status status() {
        return ChargerRule.status(!cell.isEmpty(), storedEnergy(), capacity(), dust.getCount(), reserve);
    }

    /** La carica della cella in centesimi della capacita' della config, da 0 a 100; 0 senza cella. */
    public int chargePercent() {
        return ChargerRule.percent(storedEnergy(), capacity());
    }

    /** L'uscita per il comparatore, da 0 a 15; 0 senza cella ({@link ChargerRule#comparator}). */
    public int comparatorOutput() {
        return ChargerRule.comparator(!cell.isEmpty(), storedEnergy(), capacity());
    }

    /** La carica della cella dello slot 0, mai negativa; 0 senza cella. */
    private int storedEnergy() {
        return cell.isEmpty() ? 0 : Compat.itemData().cellEnergy(cell);
    }

    private static int capacity() {
        return RedstoneExoframes.config().energy.cellCapacity;
    }

    /**
     * Non lancia mai, per chiavi mancanti o di tipo sbagliato (valgono 0 e falso), e non taglia mai la carica della
     * cella alla capacita' della config (vedi la classe): la carica e' almeno 0, la riserva in
     * [0, {@link ConfigValidation#MAX_RE_PER_REDSTONE}], il riporto in [0, 999], le polveri in [0, {@link #MAX_DUST}].
     * Scrive i campi direttamente, senza {@link #setCell} e senza {@code setChanged()}: leggere non e' un cambio.
     */
    @Override
    protected void readData(CompoundTag tag) {
        reserve = Mth.clamp(tag.getInt(NbtKeys.CHARGER_RESERVE), 0, ConfigValidation.MAX_RE_PER_REDSTONE);
        carryMilli = Mth.clamp(tag.getInt(NbtKeys.CHARGER_CARRY_MILLI), 0, MAX_CARRY_MILLI);
        int count = Mth.clamp(tag.getInt(NbtKeys.CHARGER_REDSTONE), 0, MAX_DUST);
        dust = count == 0 ? ItemStack.EMPTY : new ItemStack(Items.REDSTONE, count);
        if (tag.getBoolean(NbtKeys.CHARGER_HAS_CELL)) {
            ItemStack stack = new ItemStack(ModItems.REDSTONE_CELL.get());
            Compat.itemData().setCellEnergy(stack, Math.max(0, tag.getInt(NbtKeys.CHARGER_CELL_ENERGY)));
            cell = stack;
        } else {
            cell = ItemStack.EMPTY;
        }
    }

    /** Sempre le cinque chiavi, tutte numeri: nessuno stack nell'NBT. */
    @Override
    protected void writeData(CompoundTag tag) {
        tag.putBoolean(NbtKeys.CHARGER_HAS_CELL, !cell.isEmpty());
        tag.putInt(NbtKeys.CHARGER_CELL_ENERGY, storedEnergy());
        tag.putInt(NbtKeys.CHARGER_REDSTONE, dust.getCount());
        tag.putInt(NbtKeys.CHARGER_RESERVE, reserve);
        tag.putInt(NbtKeys.CHARGER_CARRY_MILLI, carryMilli);
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }
}
