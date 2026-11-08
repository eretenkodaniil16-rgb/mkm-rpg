package com.eretenkodaniil.mkm.showcase;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginService;
import com.eretenkodaniil.mkm.encounter.EncounterAttachments;
import com.eretenkodaniil.mkm.encounter.EncounterDirector;
import com.eretenkodaniil.mkm.encounter.EncounterLedger;
import com.eretenkodaniil.mkm.quests.net.FordQuestNetworking;
import com.eretenkodaniil.mkm.registry.MkmBlocks;
import com.eretenkodaniil.mkm.showcase.net.ShowcaseChoiceRequest;
import com.eretenkodaniil.mkm.showcase.net.ShowcaseDecisionView;
import com.eretenkodaniil.mkm.showcase.net.ShowcaseNetworking;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * A self-contained 10–15-minute authored scenario. Existing E2.1 quests and
 * v4 Character receipts remain authoritative and untouched.
 */
public final class ShowcaseService {
    private static final double RUNE_RANGE_SQ = 36.0;
    private ShowcaseService() {}

    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND
                    || !level.getBlockState(hit.getBlockPos()).is(MkmBlocks.RUNE_SLATE))
                return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            if (isStoryRune(serverPlayer, hit.getBlockPos())) {
                onRune(serverPlayer);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    public static ShowcaseProgress get(ServerPlayer player) {
        return player.getAttachedOrCreate(ShowcaseAttachments.PROGRESS);
    }
    public static ShowcaseSite site(ServerPlayer player) {
        return ((ServerLevel)player.level()).getAttachedOrCreate(ShowcaseAttachments.SITE);
    }
    private static EncounterLedger ledger(ServerPlayer player) {
        return ((ServerLevel)player.level()).getAttachedOrCreate(EncounterAttachments.ENCOUNTERS);
    }
    private static void say(ServerPlayer player,String line) {
        player.sendSystemMessage(Component.literal("[MKM · Башня] " + line));
    }
    public static boolean bind(ServerPlayer player, BlockPos origin) {
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD))
            return false;
        ShowcaseSite previous=site(player);
        if (previous.configured()) {
            say(player,"Регион уже закреплён за башней: " + previous.origin() + ". Повторная привязка запрещена.");
            return false;
        }
        BlockPos rune=origin.offset(7,5,6);
        var wall=level.getBlockState(origin.offset(4,5,4));
        if (!level.hasChunkAt(rune)
                || !level.getBlockState(rune).is(MkmBlocks.RUNE_SLATE)
                || !(wall.is(MkmBlocks.AGED_ASHLAR) || wall.is(MkmBlocks.MOSSY_ASHLAR)
                        || wall.is(MkmBlocks.CRACKED_ASHLAR))) {
            say(player,"Сначала безопасно установите башню PR #60: маркеры стен и руны не обнаружены.");
            return false;
        }
        level.setAttached(ShowcaseAttachments.SITE,previous.place(origin));
        say(player,"Маршрут привязан к " + origin + ". Игроки смогут получить координаты у Ильры.");
        return true;
    }
    public static void onIlra(ServerPlayer player) {
        ShowcaseSite site=site(player);
        if (!site.configured()) return;
        reconcile(player);
        ShowcaseProgress progress=get(player);
        if (progress.stage().equals(ShowcaseProgress.UNKNOWN)) {
            player.setAttached(ShowcaseAttachments.PROGRESS,progress.discover());
            FordQuestNetworking.sendJournal(player);
            say(player,"У разрушенной башни исчезли дозорные. Проверь записи и исследуй руну: "
                    + "X " + (site.x()+7) + ", Z " + (site.z()+6) + ". Задание добавлено в J.");
        } else if (progress.stage().equals(ShowcaseProgress.FOUND)) {
            say(player,"Ищи разрушенную башню у X " + (site.x()+7) + ", Z " + (site.z()+6)
                    + ". Рунический камень на верхнем ярусе.");
        }
    }
    private static boolean isStoryRune(ServerPlayer player, BlockPos at) {
        ShowcaseSite site=site(player);
        return site.configured() && site.runePos().equals(at)
                && player.distanceToSqr(at.getX()+.5,at.getY()+.5,at.getZ()+.5) <= RUNE_RANGE_SQ;
    }
    /** A missing loaded entity is not proof of death: the saved roster is authoritative. */
    private static EncounterLedger.Entry savedEncounter(ServerPlayer player, String choice) {
        return ledger(player).find(ShowcaseRules.eventId(player.getUUID().toString(),choice));
    }
    private static void reconcile(ServerPlayer player) {
        ShowcaseProgress progress=get(player);
        if (progress.stage().equals(ShowcaseProgress.COMPLETED)) return;
        for (String choice : new String[]{ShowcaseProgress.DEFEND,ShowcaseProgress.STUDY}) {
            EncounterLedger.Entry recorded=savedEncounter(player,choice);
            if (recorded==null || !recorded.ownerId().equals(player.getUUID().toString())) continue;
            if (progress.stage().equals(ShowcaseProgress.UNKNOWN)
                    || progress.stage().equals(ShowcaseProgress.FOUND)) {
                player.setAttached(ShowcaseAttachments.PROGRESS,
                        progress.discover().begin(choice,true));
                FordQuestNetworking.sendJournal(player);
                say(player,"Сервер восстановил сохранённую встречу без создания новых врагов.");
            }
            return;
        }
    }
    private static void onRune(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        reconcile(player);
        ShowcaseProgress p=get(player);
        ShowcaseSite s=site(player);
        if (p.stage().equals(ShowcaseProgress.UNKNOWN)) {
            player.setAttached(ShowcaseAttachments.PROGRESS,p.discover());
            p=get(player);
            say(player,"Вы нашли древнюю башню без подсказки Ильры. Новая запись появилась в журнале J.");
            FordQuestNetworking.sendJournal(player);
        }
        if (p.stage().equals(ShowcaseProgress.COMPLETED)) {
            say(player,p.approach().equals(ShowcaseProgress.DEFEND)
                    ? "Переправа защищена, у башни дежурит выживший смотритель."
                    : "Руны изучены. Они предупреждали о старом дозоре, погибшем без помощи.");
            return;
        }
        EncounterLedger.Entry event=p.stage().equals(ShowcaseProgress.ACTIVE)
                ? savedEncounter(player,p.approach()) : null;
        byte mode;
        if (p.stage().equals(ShowcaseProgress.FOUND)) mode=ShowcaseDecisionView.SELECT;
        else if (event != null && (event.phase().equals(EncounterLedger.DEFEATED)
                || event.phase().equals(EncounterLedger.CLAIMED))) mode=ShowcaseDecisionView.RESOLVE;
        else {
            say(player,event == null ? "Встреча не найдена в журнале мира; проверьте сохранение."
                    : "Бой ещё не окончен: осталось " + event.aliveCount()
                      + " подтверждённых противников. Выгруженных мобов нельзя считать мёртвыми.");
            return;
        }
        if (!ShowcaseNetworking.canShow(player)) {
            say(player,"Клиентский экран Showcase недоступен; требуется одинаковая версия мода.");
            return;
        }
        boolean canStudy=ShowcaseRules.mayStudy(CharacterService.get(player).perception(),
                CharacterOriginService.get(player));
        long token=ShowcaseSessions.open(player,s.runePos(),mode,
                ((ServerLevel)player.level()).getGameTime()).nonce();
        byte branch=(byte)(p.approach().equals(ShowcaseProgress.DEFEND)?1:
                p.approach().equals(ShowcaseProgress.STUDY)?2:0);
        ShowcaseNetworking.show(player,new ShowcaseDecisionView(s.x(),s.z(),mode,canStudy,branch,token));
    }

    public static void onChoice(ServerPlayer player, ShowcaseChoiceRequest request) {
        if (!player.isAlive() || player.isSpectator()
                || !(player.level() instanceof ServerLevel level)) return;
        ShowcaseSite site=site(player);
        if (!site.configured() || !isStoryRune(player,site.runePos())) return;
        ShowcaseProgress progress=get(player);
        byte mode=progress.stage().equals(ShowcaseProgress.FOUND)
                ? ShowcaseDecisionView.SELECT : ShowcaseDecisionView.RESOLVE;
        // Single-use server token is consumed BEFORE any state change, respawn or XP.
        if (!ShowcaseSessions.consume(player,request.nonce(),site.runePos(),
                mode,level.getGameTime())) return;
        if (request.action()==ShowcaseChoiceRequest.CLOSE) return;
        if (mode==ShowcaseDecisionView.SELECT) {
            String branch=request.action()==ShowcaseChoiceRequest.DEFEND
                    ? ShowcaseProgress.DEFEND : request.action()==ShowcaseChoiceRequest.STUDY
                    ? ShowcaseProgress.STUDY : ShowcaseProgress.NONE;
            boolean eligible=branch.equals(ShowcaseProgress.DEFEND)
                    || branch.equals(ShowcaseProgress.STUDY)
                    && ShowcaseRules.mayStudy(CharacterService.get(player).perception(),
                    CharacterOriginService.get(player));
            ShowcaseProgress next=progress.begin(branch,eligible);
            if (next==progress) return;
            if (savedEncounter(player,ShowcaseProgress.DEFEND)!=null
                    || savedEncounter(player,ShowcaseProgress.STUDY)!=null) {
                reconcile(player);return;
            }
            // Freeze approach before actor reservation: no invented alternative
            // payout if player attachment saves before the world event.
            player.setAttached(ShowcaseAttachments.PROGRESS,next);
            if (!EncounterDirector.startStoryAt(player,site.encounterAnchor(),branch)) {
                if (savedEncounter(player,branch)==null) {
                    // No world reservation and no entity was created: safe local rollback.
                    player.setAttached(ShowcaseAttachments.PROGRESS,progress);
                } else {
                    say(player,"Часть противников уже зарезервирована; не пересоздаём их.");
                }
            }
            FordQuestNetworking.sendJournal(player);
            return;
        }
        if (mode==ShowcaseDecisionView.RESOLVE && request.action()==ShowcaseChoiceRequest.FINISH) {
            resolve(player,progress);
        }
    }

    private static void resolve(ServerPlayer player, ShowcaseProgress progress) {
        String owner=player.getUUID().toString();
        String key=ShowcaseRules.eventId(owner,progress.approach());
        EncounterLedger.Entry entry=ledger(player).find(key);
        if (!ShowcaseRules.canResolve(progress,entry,owner)) return;
        // World states and v4 receipts are separately saved, so retry from
        // DEFEATED or CLAIMED must rely on the one-file Character receipt.
        String reward=ShowcaseRules.rewardId(progress.approach());
        if (entry.phase().equals(EncounterLedger.CLAIMED)
                && !CharacterService.hasExperienceReward(player,reward)) {
            say(player,"Награда требует проверки старого сохранения; повторная выплата заблокирована.");
            return;
        }
        var result=CharacterService.grantExperienceOnce(player,reward,entry.rewardXp());
        if (!result.confirmed()) {
            say(player,"Сохранение XP не подтверждено: " + result.status());
            return;
        }
        if (entry.phase().equals(EncounterLedger.DEFEATED)) {
            EncounterLedger old=ledger(player);
            ((ServerLevel)player.level()).setAttached(EncounterAttachments.ENCOUNTERS,old.claim(key));
        }
        player.setAttached(ShowcaseAttachments.PROGRESS,progress.finish());
        if (progress.approach().equals(ShowcaseProgress.DEFEND)) {
            tryCreateKeeper(player);
            say(player,"Переправа защищена. У подножия башни остался нейтральный хранитель. "
                    + "Награда: +" + result.creditedExperience() + " MKM XP.");
            ((ServerLevel)player.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    site(player).x()+9.5,site(player).y()+2,site(player).z()+22.5,
                    16,1.5,1,1.5,.02);
        } else {
            say(player,"Руны расшифрованы: дозор запечатал источник нападений. "
                    + "Исследование сохранено в журнале. Награда: +" + result.creditedExperience() + " MKM XP.");
            BlockPos rune=site(player).runePos();
            // An actual persistent visual change for the knowledge route.
            // Never overwrite another player's block or obstruct the rune itself.
            BlockPos lantern=rune.above();
            if (((ServerLevel)player.level()).getBlockState(lantern).isAir()) {
                ((ServerLevel)player.level()).setBlock(lantern,Blocks.SOUL_LANTERN.defaultBlockState(),3);
            }
            ((ServerLevel)player.level()).sendParticles(ParticleTypes.END_ROD,
                    rune.getX()+.5,rune.getY()+1,rune.getZ()+.5,35,1,1,1,.03);
        }
        FordQuestNetworking.sendJournal(player);
    }

    private static void tryCreateKeeper(ServerPlayer player) {
        ShowcaseSite site=site(player);
        if (!site.keeperUuid().isBlank()) return;
        ServerLevel level=((ServerLevel)player.level());
        BlockPos origin=site.origin();
        BlockPos[] targets={origin.offset(9,0,23),origin.offset(10,0,23),
                origin.offset(8,0,24),origin.offset(9,0,25)};
        for (BlockPos pos:targets) {
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos.below()).isSolid()) continue;
            Villager npc=new Villager(EntityTypes.VILLAGER,level);
            npc.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
            if (!level.noCollision(npc)) continue;
            UUID id=UUID.randomUUID();
            npc.setUUID(id);
            npc.setCustomName(Component.literal("Хранитель переправы"));
            npc.setCustomNameVisible(true);
            npc.setNoAi(true);
            npc.setPersistenceRequired();
            npc.setPermanentlyInvulnerable(true);
            // Persist identity before entity creation. If insertion or world save
            // fails, do not auto-generate another neutral villager.
            level.setAttached(ShowcaseAttachments.SITE,site.withKeeper(id.toString()));
            if (!level.addFreshEntity(npc))
                MkmMod.LOGGER.error("Failed to insert reserved Showcase keeper {}",id);
            return;
        }
        MkmMod.LOGGER.warn("Showcase keeper site obstructed; no entity inserted, world outcome retained.");
    }

    public static byte journalStage(ShowcaseProgress s) {
        return switch(s.stage()) {
            case ShowcaseProgress.UNKNOWN -> 0;
            case ShowcaseProgress.FOUND -> 1;
            case ShowcaseProgress.ACTIVE -> 2;
            case ShowcaseProgress.COMPLETED -> 3;
            default -> 4;
        };
    }
    public static byte journalBranch(ShowcaseProgress s) {
        return (byte)(s.approach().equals(ShowcaseProgress.DEFEND)?1:
                s.approach().equals(ShowcaseProgress.STUDY)?2:0);
    }
    public static int remaining(ServerPlayer player) {
        ShowcaseProgress p=get(player);
        if (!p.stage().equals(ShowcaseProgress.ACTIVE)) return -1;
        var event=savedEncounter(player,p.approach());
        return event==null?-1:event.aliveCount();
    }
    public static void onLogin(ServerPlayer player) {
        ShowcaseSessions.invalidate(player);
        if (site(player).configured()) reconcile(player);
    }
}
